import { Injectable, computed, signal } from '@angular/core';
import { Capacitor } from '@capacitor/core';

import { HBand } from './hband.plugin';
import type {
  HBandDataEvent, HBandDevice, HBandHistoryRecord, HBandLogEntry, HBandStatus, MetricId,
} from './hband.types';

const INITIAL_STATUS: HBandStatus = {
  available: false,
  bluetoothEnabled: false,
  state: 'idle',
  capabilities: {},
  platform: Capacitor.getPlatform(),
};

const METRIC_OPERATIONS = new Set([
  'measure.heartRate.start', 'measure.heartRate.stop',
  'measure.bloodPressure.start', 'measure.bloodPressure.stop',
  'measure.oxygen.start', 'measure.oxygen.stop',
  'measure.temperature.start', 'measure.temperature.stop',
  'measure.bloodGlucose.start', 'measure.bloodGlucose.stop',
  'measure.hrv.start', 'measure.hrv.stop',
  'measure.ecg.start', 'measure.ecg.stop',
  'measure.bodyComposition.start', 'measure.bodyComposition.stop',
  'measure.stress.start', 'measure.stress.stop',
  'history.metric',
]);

@Injectable({ providedIn: 'root' })
export class HBandService {
  readonly isNative = Capacitor.isNativePlatform();
  readonly status = signal<HBandStatus>(INITIAL_STATUS);
  readonly devices = signal<HBandDevice[]>([]);
  readonly data = signal<Partial<Record<MetricId, HBandDataEvent>>>({});
  readonly finalMeasurements = signal<Partial<Record<MetricId, HBandDataEvent>>>({});
  readonly history = signal<Partial<Record<MetricId, HBandDataEvent>>>({});
  readonly logs = signal<HBandLogEntry[]>([]);
  readonly busyOperation = signal<string | null>(null);
  readonly activeMeasurements = signal<Partial<Record<MetricId, boolean>>>({});
  readonly simulation = signal(!this.isNative);
  readonly connected = computed(() => this.status().state === 'connected');
  private queue: Promise<unknown> = Promise.resolve();
  private logSequence = 0;
  private readonly operationMetrics = new Map<string, MetricId>();

  /**
   * Liga os eventos uma única vez e recupera o estado da bridge nativa.
   */
  async initialize(): Promise<void> {
    if (this.simulation()) {
      this.seedSimulation();
      return;
    }
    await HBand.addListener('deviceFound', (device) => this.upsertDevice(device));
    await HBand.addListener('statusChanged', (status) => this.storeStatus(status));
    await HBand.addListener('data', (event) => this.storeData(event));
    await HBand.addListener('log', (entry) => this.appendLog(entry));
    try {
      this.status.set(await HBand.getStatus());
    } catch (error) {
      this.fail('session.initialize', error);
    }
  }

  async scan(): Promise<void> {
    if (this.simulation()) {
      this.simulateScan();
      return;
    }
    const permission = await HBand.requestPermissions();
    if (!permission.granted) {
      throw new Error('BLE_PERMISSION_DENIED');
    }
    this.devices.set([]);
    await HBand.startScan({ timeoutMs: 12_000 });
  }

  async connect(deviceId: string, password = '0000'): Promise<void> {
    if (this.simulation()) {
      this.simulateConnection(deviceId);
      return;
    }
    await HBand.connect({ deviceId, password });
  }

  async disconnect(): Promise<void> {
    if (this.simulation()) {
      this.status.update((status) => ({ ...status, state: 'disconnected', device: undefined }));
      this.activeMeasurements.set({});
      return;
    }
    await HBand.disconnect();
    this.activeMeasurements.set({});
  }

  /**
   * Serializa os comandos porque o protocolo não permite operações BLE longas
   * concorrentes sobre a mesma pulseira.
   */
  execute(operation: string, params: Record<string, unknown> = {}): Promise<void> {
    const task = async () => {
      const metric = this.metricForOperation(operation, params);
      if (metric) {
        this.operationMetrics.set(operation, metric);
      }
      this.busyOperation.set(operation);
      this.appendLog({
        id: this.nextLogId(operation),
        timestamp: new Date().toISOString(),
        level: 'info',
        message: operation,
        operation,
        metric,
        kind: 'operation',
      });
      try {
        if (this.simulation()) {
          this.simulateOperation(operation, params);
        } else {
          await HBand.execute({ operation, params });
        }
        this.appendLog({
          id: this.nextLogId(operation),
          timestamp: new Date().toISOString(),
          level: 'success',
          message: operation,
          operation,
          metric,
          kind: 'operation',
        });
      } catch (error) {
        this.fail(operation, error, metric);
        throw error;
      } finally {
        this.busyOperation.set(null);
      }
    };
    this.queue = this.queue.then(task, task);
    return this.queue.then(() => undefined);
  }

  capability(name: string): 'supported' | 'unsupported' | 'unknown' {
    return this.status().capabilities[name] ?? 'unknown';
  }

  operationAvailable(operation: string): boolean {
    return this.simulation() || METRIC_OPERATIONS.has(operation);
  }

  latestFor(metric: MetricId): HBandDataEvent | undefined {
    return this.data()[metric];
  }

  historyFor(metric: MetricId): HBandDataEvent | undefined {
    return this.history()[metric];
  }

  finalFor(metric: MetricId): HBandDataEvent | undefined {
    return this.finalMeasurements()[metric];
  }

  measurementActive(metric: MetricId): boolean {
    return this.activeMeasurements()[metric] === true;
  }

  metricLogs(metric: MetricId): HBandLogEntry[] {
    return this.logs().filter((entry) => entry.metric === metric);
  }

  /**
   * Alterna a mesma ação visual entre os contratos nativos de início e fim.
   * O estado só muda depois de a bridge aceitar a operação correspondente.
   */
  async toggleMeasurement(metric: MetricId, startOperation: string, stopOperation: string): Promise<void> {
    const active = this.measurementActive(metric);
    if (!active && metric === 'ecg') {
      this.data.update((data) => {
        const current = data.ecg;
        return current
          ? { ...data, ecg: { ...current, samples: [] } }
          : data;
      });
    }
    await this.execute(active ? stopOperation : startOperation);
    if (!active && metric === 'bloodGlucose') {
      this.finalMeasurements.update((measurements) => ({ ...measurements, bloodGlucose: undefined }));
    }
    this.activeMeasurements.update((measurements) => ({
      ...measurements,
      [metric]: !active,
    }));
  }

  private storeStatus(status: HBandStatus): void {
    this.status.set(status);
    if (status.state === 'disconnected' || status.state === 'unavailable') {
      this.activeMeasurements.set({});
    }
  }

  private upsertDevice(device: HBandDevice): void {
    this.devices.update((devices) => {
      const next = devices.filter((item) => item.id !== device.id);
      return [...next, device].sort((a, b) => (b.rssi ?? -100) - (a.rssi ?? -100));
    });
  }

  /**
   * Separa histórico e tempo real, impedindo que uma leitura de outro dia
   * substitua o valor atual apresentado no cartão da métrica.
   */
  private storeData(event: HBandDataEvent): void {
    const metric = event.metric ?? this.metricForType(event.type);
    if (!metric) {
      return;
    }
    this.appendLog({
      id: this.nextLogId(event.type),
      timestamp: event.timestamp || new Date().toISOString(),
      level: 'success',
      message: event.type,
      metric,
      kind: 'data',
      data: event,
    });
    if (event.type === 'history') {
      this.history.update((history) => ({ ...history, [metric]: event }));
    } else {
      /*
       * ECG, composição corporal, Stress e outras medições distribuem amostras,
       * progresso e resultado por callbacks distintos. A composição conserva
       * a leitura completa sem apagar amostras quando chega apenas um estado.
       */
      this.data.update((data) => {
        const previous = data[metric];
        const merged: HBandDataEvent = {
          ...previous,
          ...event,
          metric,
          values: { ...(previous?.values ?? {}), ...event.values },
          samples: this.mergeSamples(metric, previous?.samples, event.samples),
        };
        return { ...data, [metric]: merged };
      });
      if (metric === 'bloodGlucose' && this.progress(event) >= 100) {
        const complete = this.data()[metric];
        if (complete) {
          this.finalMeasurements.update((measurements) => ({ ...measurements, [metric]: complete }));
        }
      }
      const current = this.data()[metric];
      if (current && this.measurementFinished(metric, current)) {
        this.activeMeasurements.update((measurements) => ({ ...measurements, [metric]: false }));
      }
    }
  }

  /**
   * Converte apenas percentagens numéricas confirmadas pelo payload do SDK.
   */
  private progress(event: HBandDataEvent): number {
    const value = event.values['progress'];
    return typeof value === 'number' && Number.isFinite(value) ? value : 0;
  }

  /**
   * Termina visualmente apenas medições que o SDK identifica como finitas.
   * A frequência cardíaca não usa estados de dados como sinal de fim, pois
   * `STATE_HEART_NORMAL` representa precisamente uma leitura ainda ativa.
   */
  private measurementFinished(metric: MetricId, event: HBandDataEvent): boolean {
    const finishesAtFullProgress: MetricId[] = [
      'bloodPressure', 'oxygen', 'temperature', 'bloodGlucose', 'ecg',
      'bodyComposition', 'stress',
    ];
    if (finishesAtFullProgress.includes(metric)) {
      return this.progress(event) >= 100;
    }
    return metric === 'hrv'
      && typeof event.values['milliseconds'] === 'number'
      && event.values['milliseconds'] > 0;
  }

  /**
   * Mantém uma janela móvel do ECG para o gráfico não mostrar apenas o último
   * pacote ADC. Nas restantes métricas conserva o comportamento de substituição.
   */
  private mergeSamples(
    metric: MetricId,
    previous: number[] | undefined,
    incoming: number[] | undefined,
  ): number[] | undefined {
    if (!incoming?.length) {
      return previous;
    }
    if (metric !== 'ecg') {
      return incoming;
    }
    return [...(previous ?? []), ...incoming].slice(-1_000);
  }

  private metricForType(type: string): MetricId | undefined {
    const metrics: MetricId[] = [
      'heartRate', 'bloodPressure', 'oxygen', 'temperature', 'bloodGlucose',
      'hrv', 'ecg', 'bodyComposition', 'met', 'stress',
    ];
    return metrics.find((metric) => metric === type);
  }

  private appendLog(entry: HBandLogEntry): void {
    const metric = entry.metric
      ?? (entry.operation ? this.operationMetrics.get(entry.operation) : undefined)
      ?? (entry.operation ? this.metricForOperation(entry.operation) : undefined);
    this.logs.update((logs) => [{ ...entry, metric, kind: entry.kind ?? 'bridge' }, ...logs].slice(0, 200));
  }

  private fail(operation: string, error: unknown, metric?: MetricId): void {
    const message = error instanceof Error ? error.message : String(error);
    this.appendLog({
      id: this.nextLogId(operation),
      timestamp: new Date().toISOString(),
      level: 'error',
      message,
      operation,
      metric,
      kind: 'operation',
    });
  }

  /**
   * Deduz a métrica a partir do contrato executado. Para histórico usa o
   * parâmetro explícito; para medições usa o segmento `measure.<métrica>`.
   */
  private metricForOperation(
    operation: string,
    params: Record<string, unknown> = {},
  ): MetricId | undefined {
    if (operation === 'history.metric') {
      const metric = params['metric'];
      return typeof metric === 'string' && this.metricForType(metric) ? metric as MetricId : undefined;
    }
    const match = /^measure\.([^.]+)\.(start|stop)$/.exec(operation);
    return match ? this.metricForType(match[1]) : undefined;
  }

  private nextLogId(source: string): string {
    this.logSequence += 1;
    return `${Date.now()}-${this.logSequence}-${source}`;
  }

  private seedSimulation(): void {
    const capabilities = Object.fromEntries([
      'heartRate', 'bloodPressure', 'bloodOxygen', 'temperature', 'bloodGlucose',
      'hrv', 'ecg', 'bodyComposition', 'met', 'stress',
    ].map((capability) => [capability, 'supported' as const]));
    this.status.set({
      available: true,
      bluetoothEnabled: true,
      state: 'idle',
      capabilities,
      platform: 'web',
      sdkVersion: 'simulation',
    });
  }

  private simulateScan(): void {
    this.status.update((status) => ({ ...status, state: 'scanning' }));
    this.devices.set([]);
    window.setTimeout(() => {
      this.upsertDevice({ id: 'MF91-DEMO', name: 'MF91', rssi: -48, model: 'MF91' });
      this.status.update((status) => ({ ...status, state: 'idle' }));
    }, 300);
  }

  private simulateConnection(deviceId: string): void {
    const device = this.devices().find((item) => item.id === deviceId)
      ?? { id: deviceId, name: 'MF91', model: 'MF91', rssi: -48 };
    this.status.update((status) => ({
      ...status,
      state: 'connected',
      device: { ...device, firmware: 'simulation' },
    }));
  }

  private simulateOperation(operation: string, params: Record<string, unknown>): void {
    const now = new Date().toISOString();
    const live: Record<string, HBandDataEvent> = {
      'measure.heartRate.start': { type: 'heartRate', metric: 'heartRate', timestamp: now, values: { bpm: 74 }, samples: [69, 71, 73, 72, 75, 77, 74] },
      'measure.bloodPressure.start': { type: 'bloodPressure', metric: 'bloodPressure', timestamp: now, values: { systolic: 121, diastolic: 78, pulseBpm: 73 } },
      'measure.oxygen.start': { type: 'oxygen', metric: 'oxygen', timestamp: now, values: { percent: 97 }, samples: [96, 97, 97, 98, 97] },
      'measure.temperature.start': { type: 'temperature', metric: 'temperature', timestamp: now, values: { celsius: 36.4 }, samples: [36.1, 36.2, 36.3, 36.4] },
      'measure.bloodGlucose.start': { type: 'bloodGlucose', metric: 'bloodGlucose', timestamp: now, values: { mmolL: 5.2 }, samples: [4.9, 5.1, 5.2] },
      'measure.hrv.start': { type: 'hrv', metric: 'hrv', timestamp: now, values: { milliseconds: 54 }, samples: [48, 52, 51, 57, 54] },
      'measure.ecg.start': { type: 'ecg', metric: 'ecg', timestamp: now, values: { bpm: 72 }, samples: [0, 12, 45, -18, -8, 2, 4, 42, -20, -7, 1, 3] },
      'measure.bodyComposition.start': { type: 'bodyComposition', metric: 'bodyComposition', timestamp: now, values: { bmi: 22.4, bodyFatPercent: 18.8, waterPercent: 58.2, muscleMassKg: 49.6, boneMassKg: 2.8, basalMetabolismKcal: 1540 } },
      'measure.stress.start': { type: 'stress', metric: 'stress', timestamp: now, values: { score: 38 }, samples: [32, 36, 44, 40, 38] },
    };
    if (operation === 'history.metric') {
      const metric = params['metric'] as MetricId;
      const date = String(params['date']);
      const records = this.simulatedHistory(metric, date);
      this.storeData({
        type: 'history',
        metric,
        date,
        timestamp: now,
        values: { records: records.length },
        records,
        samples: records.flatMap((record) => record.samples ?? this.numericValues(record.values)),
      });
      return;
    }
    const event = live[operation];
    if (event) {
      this.storeData(event);
    }
  }

  private simulatedHistory(metric: MetricId, date: string): HBandHistoryRecord[] {
    const valueSets: Record<MetricId, Array<Record<string, number>>> = {
      heartRate: [{ bpm: 68 }, { bpm: 73 }, { bpm: 76 }, { bpm: 71 }],
      bloodPressure: [{ systolic: 118, diastolic: 76 }, { systolic: 121, diastolic: 79 }],
      oxygen: [{ percent: 97 }, { percent: 98 }, { percent: 96 }],
      temperature: [{ celsius: 36.2 }, { celsius: 36.4 }, { celsius: 36.3 }],
      bloodGlucose: [{ mmolL: 4.9 }, { mmolL: 5.5 }, { mmolL: 5.1 }],
      hrv: [{ milliseconds: 49 }, { milliseconds: 55 }, { milliseconds: 52 }],
      ecg: [{ bpm: 71 }, { bpm: 73 }],
      bodyComposition: [{ bmi: 22.4, bodyFatPercent: 18.8, waterPercent: 58.2, muscleMassKg: 49.6 }],
      met: [{ met: 1.0 }, { met: 2.2 }, { met: 4.4 }, { met: 6.6 }],
      stress: [{ score: 31 }, { score: 44 }, { score: 38 }],
    };
    return valueSets[metric].map((values, index) => ({
      timestamp: `${date}T${String(8 + index * 3).padStart(2, '0')}:00:00`,
      values,
      samples: metric === 'ecg' ? [0, 10, 42, -20, -7, 2, 4, 38, -18, -5] : undefined,
    }));
  }

  private numericValues(values: Record<string, string | number | boolean | null>): number[] {
    return Object.values(values).filter((value): value is number => typeof value === 'number');
  }
}
