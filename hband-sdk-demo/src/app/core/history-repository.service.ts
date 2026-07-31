import { Injectable } from '@angular/core';

import type {
  HBandDataEvent, HBandHistoryRecord, HBandReadingSource, MetricId,
} from './hband.types';

const DATABASE_NAME = 'hband-history';
const DATABASE_VERSION = 2;
const HISTORY_STORE = 'history-days';

interface StoredHistoryEvent {
  key: string;
  deviceId: string;
  date: string;
  metric: MetricId;
  event: HBandDataEvent;
  updatedAt: string;
}

export interface HBandHistorySnapshot {
  events: Partial<Record<MetricId, HBandDataEvent>>;
  recordCount: number;
  updatedAt?: string;
}

/**
 * Arquiva o histórico por pulseira, métrica e data numa base IndexedDB.
 * O armazenamento web do Capacitor é comum ao Android e ao iOS e permite
 * conservar séries extensas, incluindo amostras de ECG, sem novos plugins.
 */
@Injectable({ providedIn: 'root' })
export class HistoryRepositoryService {
  private readonly memoryFallback = new Map<string, StoredHistoryEvent>();
  private databasePromise: Promise<IDBDatabase | null> | null = null;
  private writeQueue: Promise<unknown> = Promise.resolve();

  /**
   * Obtém o retrato completo de um dia depois de concluir escritas pendentes,
   * evitando apresentar uma versão anterior ao último callback recebido.
   */
  async loadDate(deviceId: string, date: string): Promise<HBandHistorySnapshot> {
    await this.writeQueue.catch(() => undefined);
    const database = await this.database();
    const stored = database
      ? await this.readDateFromDatabase(database, deviceId, date)
      : [...this.memoryFallback.values()].filter(
        (item) => item.deviceId === deviceId && item.date === date,
      );
    const events: Partial<Record<MetricId, HBandDataEvent>> = {};
    let updatedAt: string | undefined;
    for (const item of stored) {
      events[item.metric] = item.event;
      if (!updatedAt || item.updatedAt > updatedAt) {
        updatedAt = item.updatedAt;
      }
    }
    return {
      events,
      recordCount: Object.values(events).reduce(
        (total, event) => total + (event?.records?.length ?? 0),
        0,
      ),
      updatedAt,
    };
  }

  /**
   * Lista apenas datas que contêm pelo menos um registo. O calendário usa este
   * índice para destacar dias com medições sem carregar todos os payloads.
   */
  async listDates(deviceId: string): Promise<string[]> {
    await this.writeQueue.catch(() => undefined);
    const database = await this.database();
    const stored = database
      ? await this.readDeviceFromDatabase(database, deviceId)
      : [...this.memoryFallback.values()].filter((item) => item.deviceId === deviceId);
    return [...new Set(
      stored
        .filter((item) => (item.event.records?.length ?? 0) > 0)
        .map((item) => item.date),
    )].sort();
  }

  /**
   * Faz upsert do bloco mais recente, usando o instante como identidade do
   * registo. Este comportamento reproduz o `INSERT OR REPLACE` observado na
   * G Band e impede duplicados quando a pulseira volta a enviar o mesmo dia.
   */
  mergeEvent(deviceId: string, event: HBandDataEvent): Promise<HBandDataEvent> {
    const task = async (): Promise<HBandDataEvent> => {
      if (!event.metric || !event.date) {
        return event;
      }
      const key = this.eventKey(deviceId, event.metric, event.date);
      const database = await this.database();
      const previous = database
        ? await this.readEventFromDatabase(database, key)
        : this.memoryFallback.get(key);
      const merged = this.mergeHistoryEvents(previous?.event, event);
      const stored: StoredHistoryEvent = {
        key,
        deviceId,
        date: event.date,
        metric: event.metric,
        event: merged,
        updatedAt: new Date().toISOString(),
      };
      if (database) {
        await this.writeEventToDatabase(database, stored);
      } else {
        this.memoryFallback.set(key, stored);
      }
      return merged;
    };
    const result = this.writeQueue.then(task, task);
    this.writeQueue = result;
    return result;
  }

  private async database(): Promise<IDBDatabase | null> {
    if (!this.databasePromise) {
      this.databasePromise = this.openDatabase();
    }
    return this.databasePromise;
  }

  /**
   * Mantém um fallback em memória para testes e ambientes onde IndexedDB não
   * esteja disponível; a aplicação continua funcional, embora sem persistência.
   */
  private openDatabase(): Promise<IDBDatabase | null> {
    if (!globalThis.indexedDB) {
      return Promise.resolve(null);
    }
    return new Promise((resolve) => {
      const request = globalThis.indexedDB.open(DATABASE_NAME, DATABASE_VERSION);
      request.onupgradeneeded = () => {
        const database = request.result;
        let store: IDBObjectStore;
        if (!database.objectStoreNames.contains(HISTORY_STORE)) {
          store = database.createObjectStore(HISTORY_STORE, { keyPath: 'key' });
          store.createIndex('device-date', ['deviceId', 'date'], { unique: false });
        } else {
          store = request.transaction!.objectStore(HISTORY_STORE);
        }
        if (!store.indexNames.contains('device-id')) {
          store.createIndex('device-id', 'deviceId', { unique: false });
        }
      };
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => resolve(null);
      request.onblocked = () => resolve(null);
    });
  }

  private readDateFromDatabase(
    database: IDBDatabase,
    deviceId: string,
    date: string,
  ): Promise<StoredHistoryEvent[]> {
    return new Promise((resolve, reject) => {
      const transaction = database.transaction(HISTORY_STORE, 'readonly');
      const index = transaction.objectStore(HISTORY_STORE).index('device-date');
      const request = index.getAll(IDBKeyRange.only([deviceId, date]));
      request.onsuccess = () => resolve(request.result as StoredHistoryEvent[]);
      request.onerror = () => reject(request.error);
    });
  }

  private readEventFromDatabase(
    database: IDBDatabase,
    key: string,
  ): Promise<StoredHistoryEvent | undefined> {
    return new Promise((resolve, reject) => {
      const transaction = database.transaction(HISTORY_STORE, 'readonly');
      const request = transaction.objectStore(HISTORY_STORE).get(key);
      request.onsuccess = () => resolve(request.result as StoredHistoryEvent | undefined);
      request.onerror = () => reject(request.error);
    });
  }

  private readDeviceFromDatabase(
    database: IDBDatabase,
    deviceId: string,
  ): Promise<StoredHistoryEvent[]> {
    return new Promise((resolve, reject) => {
      const transaction = database.transaction(HISTORY_STORE, 'readonly');
      const index = transaction.objectStore(HISTORY_STORE).index('device-id');
      const request = index.getAll(IDBKeyRange.only(deviceId));
      request.onsuccess = () => resolve(request.result as StoredHistoryEvent[]);
      request.onerror = () => reject(request.error);
    });
  }

  private writeEventToDatabase(
    database: IDBDatabase,
    stored: StoredHistoryEvent,
  ): Promise<void> {
    return new Promise((resolve, reject) => {
      const transaction = database.transaction(HISTORY_STORE, 'readwrite');
      transaction.objectStore(HISTORY_STORE).put(stored);
      transaction.oncomplete = () => resolve();
      transaction.onerror = () => reject(transaction.error);
      transaction.onabort = () => reject(transaction.error);
    });
  }

  private eventKey(deviceId: string, metric: MetricId, date: string): string {
    return JSON.stringify([deviceId, metric, date]);
  }

  private mergeHistoryEvents(
    previous: HBandDataEvent | undefined,
    incoming: HBandDataEvent,
  ): HBandDataEvent {
    const defaultSource: HBandReadingSource = incoming.metric === 'ecg'
      || incoming.metric === 'bodyComposition' ? 'manual' : 'automatic';
    const records = this.mergeRecords(
      previous?.records ?? [],
      incoming.records ?? [],
      incoming.readingSource ?? defaultSource,
    );
    return {
      ...previous,
      ...incoming,
      values: {
        ...(previous?.values ?? {}),
        ...incoming.values,
        records: records.length,
      },
      records,
      samples: records.flatMap((record) => record.samples ?? []),
    };
  }

  private mergeRecords(
    previous: HBandHistoryRecord[],
    incoming: HBandHistoryRecord[],
    defaultSource: HBandReadingSource,
  ): HBandHistoryRecord[] {
    const records = new Map<string, HBandHistoryRecord>();
    for (const record of [...previous, ...incoming]) {
      const source = record.source ?? defaultSource;
      const key = `${record.timestamp}|${source}`;
      const current = records.get(key);
      records.set(key, {
        ...current,
        ...record,
        source,
        values: { ...(current?.values ?? {}), ...record.values },
        samples: record.samples?.length ? record.samples : current?.samples,
      });
    }
    return [...records.values()].sort(
      (left, right) =>
        new Date(left.timestamp).getTime() - new Date(right.timestamp).getTime(),
    );
  }
}
