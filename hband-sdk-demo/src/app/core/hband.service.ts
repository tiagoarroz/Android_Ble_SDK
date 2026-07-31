import { Injectable, computed, inject, signal } from '@angular/core';
import { Capacitor } from '@capacitor/core';

import { HBand } from './hband.plugin';
import { HistoryRepositoryService } from './history-repository.service';
import type {
  HBandBatteryStatus, HBandDataEvent, HBandDevice, HBandHistoryRecord, HBandLogEntry,
  HBandHistoryState, HBandMonitoringSetting, HBandReadingSource, HBandStatus,
  HBandSyncStatus, MetricId,
} from './hband.types';

const INITIAL_STATUS: HBandStatus = {
  available: false,
  bluetoothEnabled: false,
  state: 'idle',
  capabilities: {},
  platform: Capacitor.getPlatform(),
};

const DEVICE_SESSION_KEY = 'hband-device-session';
const HISTORY_DEVICE_KEY = 'hband-history-device';
const BATTERY_REFRESH_INTERVAL_MS = 60_000;
const CONTROL_OPERATION_TIMEOUT_MS = 10_000;
const HISTORY_OPERATION_TIMEOUT_MS = 45_000;
const CATALOG_METRICS: MetricId[] = [
  'steps', 'heartRate', 'bloodPressure', 'oxygen', 'temperature',
  'bloodGlucose', 'ecg', 'bodyComposition', 'stress',
];

interface RememberedDeviceSession {
  deviceId: string;
  password: string;
}

class HBandOperationTimeoutError extends Error {
  readonly code = 'SDK_OPERATION_TIMEOUT';

  constructor(
    readonly operation: string,
    readonly timeoutMs: number,
  ) {
    super(`SDK_OPERATION_TIMEOUT: ${operation} (${timeoutMs}ms)`);
    this.name = 'HBandOperationTimeoutError';
  }
}

const METRIC_OPERATIONS = new Set([
  'measure.heartRate.start', 'measure.heartRate.stop',
  'measure.bloodPressure.start', 'measure.bloodPressure.stop',
  'measure.oxygen.start', 'measure.oxygen.stop',
  'measure.temperature.start', 'measure.temperature.stop',
  'measure.bloodGlucose.start', 'measure.bloodGlucose.stop',
  'measure.ecg.start', 'measure.ecg.stop',
  'measure.bodyComposition.start', 'measure.bodyComposition.stop',
  'measure.stress.start', 'measure.stress.stop',
  'history.activity.current',
  'history.metric',
  'history.daily',
  'history.manual.daily',
  'history.cached',
  'device.battery',
  'monitoring.read',
  'monitoring.set',
]);

@Injectable({ providedIn: 'root' })
export class HBandService {
  private readonly historyRepository = inject(HistoryRepositoryService);
  readonly isNative = Capacitor.isNativePlatform();
  readonly status = signal<HBandStatus>(INITIAL_STATUS);
  readonly battery = signal<HBandBatteryStatus | null>(null);
  readonly monitoringSettings = signal<Partial<Record<MetricId, HBandMonitoringSetting>>>({});
  readonly devices = signal<HBandDevice[]>([]);
  readonly data = signal<Partial<Record<MetricId, HBandDataEvent>>>({});
  readonly finalMeasurements = signal<Partial<Record<MetricId, HBandDataEvent>>>({});
  readonly history = signal<Partial<Record<MetricId, HBandDataEvent>>>({});
  readonly historyDates = signal<string[]>([]);
  readonly historyState = signal<HBandHistoryState>({
    date: this.localDate(new Date()),
    phase: 'idle',
    source: 'none',
    recordCount: 0,
  });
  readonly syncStatus = signal<HBandSyncStatus>({
    state: 'idle', completed: 0, total: 0, failed: 0,
  });
  readonly logs = signal<HBandLogEntry[]>([]);
  readonly busyOperation = signal<string | null>(null);
  readonly activeMeasurements = signal<Partial<Record<MetricId, boolean>>>({});
  readonly simulation = signal(!this.isNative);
  readonly connected = computed(() => this.status().state === 'connected');
  private queue: Promise<unknown> = Promise.resolve();
  private logSequence = 0;
  private readonly operationMetrics = new Map<string, MetricId>();
  private syncPromise: Promise<void> | null = null;
  private pendingSyncDate: string | null = null;
  private backgroundSyncPromise: Promise<void> | null = null;
  private backgroundSyncRequested = false;
  private monitoringRefreshPromise: Promise<void> | null = null;
  private selectedHistoryDate = this.localDate(new Date());
  private historyDeviceId = this.rememberedDeviceId();
  private batteryRefreshTimer: ReturnType<typeof setInterval> | null = null;
  /*
   * O adaptador mantém o proxy Capacitor substituível nos testes de timeout,
   * sem alterar o contrato público usado pela aplicação.
   */
  private nativeExecute = (operation: string, params: Record<string, unknown>) =>
    HBand.execute({ operation, params });

  /**
   * Liga os eventos uma única vez e recupera o estado da bridge nativa.
   */
  async initialize(): Promise<void> {
    if (this.simulation()) {
      this.seedSimulation();
      await this.refreshHistoryDates();
      return;
    }
    await HBand.addListener('deviceFound', (device) => this.upsertDevice(device));
    await HBand.addListener('statusChanged', (status) => this.storeStatus(status));
    await HBand.addListener('data', (event) => this.storeData(event));
    await HBand.addListener('log', (entry) => this.appendLog(entry));
    try {
      const status = await HBand.getStatus();
      this.storeStatus(status);
      await this.refreshHistoryDates();
      if (
        status.available
        && status.bluetoothEnabled
        && status.platform === 'android'
        && ['idle', 'disconnected', 'error'].includes(status.state)
      ) {
        await this.restoreDeviceSession();
      }
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
    this.historyDeviceId = deviceId;
    this.rememberHistoryDevice(deviceId);
    this.historyDates.set([]);
    await this.refreshHistoryDates();
    if (this.simulation()) {
      this.simulateConnection(deviceId);
      return;
    }
    await HBand.connect({ deviceId, password });
    this.rememberDeviceSession({ deviceId, password });
  }

  async disconnect(): Promise<void> {
    this.stopBatteryUpdates();
    if (this.simulation()) {
      this.status.update((status) => ({ ...status, state: 'disconnected', device: undefined }));
      this.activeMeasurements.set({});
      this.battery.set(null);
      this.monitoringSettings.set({});
      return;
    }
    localStorage.removeItem(DEVICE_SESSION_KEY);
    await HBand.disconnect();
    this.activeMeasurements.set({});
    this.battery.set(null);
    this.monitoringSettings.set({});
  }

  async refreshBattery(): Promise<void> {
    await this.execute('device.battery');
  }

  /**
   * Obtém uma única vez em simultâneo a configuração das métricas automáticas.
   * A bridge publica cada modelo real através do evento `monitoring`.
   */
  refreshMonitoringSettings(): Promise<void> {
    if (!this.connected()) {
      return Promise.resolve();
    }
    if (!this.monitoringRefreshPromise) {
      this.monitoringRefreshPromise = this.execute('monitoring.read').finally(() => {
        this.monitoringRefreshPromise = null;
      });
    }
    return this.monitoringRefreshPromise;
  }

  monitoringFor(metric: MetricId): HBandMonitoringSetting | undefined {
    return this.monitoringSettings()[metric];
  }

  /**
   * A alteração só é enviada para uma configuração que tenha sido realmente
   * devolvida pela pulseira, evitando mostrar controlos sem suporte efetivo.
   */
  async setMonitoring(metric: MetricId, enabled: boolean): Promise<void> {
    if (!this.monitoringFor(metric)) {
      throw new Error(`MONITORING_SETTING_NOT_AVAILABLE: ${metric}`);
    }
    await this.execute('monitoring.set', { metric, enabled });
  }

  /**
   * Apresenta imediatamente o arquivo local e tenta depois atualizá-lo a partir
   * do SDK. O pedido funciona também sem ligação, permitindo consultar dias que
   * já tenham sido guardados anteriormente neste telemóvel.
   */
  async synchroniseDate(date: string): Promise<void> {
    this.selectedHistoryDate = date;
    await this.loadStoredDate(date);
    if (!this.connected()) {
      this.setHistoryPhase('offline');
      return;
    }
    this.pendingSyncDate = date;
    if (this.syncPromise) {
      return this.syncPromise;
    }
    this.syncPromise = this.runPendingSynchronisations().finally(() => {
      this.syncPromise = null;
    });
    return this.syncPromise;
  }

  private async runPendingSynchronisations(): Promise<void> {
    while (this.pendingSyncDate) {
      const date = this.pendingSyncDate;
      this.pendingSyncDate = null;
      const canContinue = await this.synchroniseRequestedDate(date);
      if (!canContinue) {
        this.pendingSyncDate = null;
        break;
      }
    }
  }

  /**
   * Atualiza o dia pedido com uma única leitura diária nativa. No iOS, dias
   * fora da retenção da pulseira consultam apenas a base local do próprio SDK.
   */
  private async synchroniseRequestedDate(
    date: string,
    includeSessionOperations = true,
    reportProgress = true,
    forceCachedIOSRead = false,
  ): Promise<boolean> {
    const daysAgo = this.daysBetween(date, this.localDate(new Date()));
    const retentionDays = this.historyRetentionDays();
    const outsideRetention = daysAgo < 0 || daysAgo > retentionDays;
    const cachedIOSRead = this.status().platform === 'ios'
      && (outsideRetention || forceCachedIOSRead);
    if (outsideRetention && !cachedIOSRead) {
      if (date === this.selectedHistoryDate) {
        this.setHistoryPhase('outsideRetention');
      }
      return true;
    }
    const historyOperations: Array<{ operation: string; params: Record<string, unknown> }> =
      this.status().platform === 'android'
        ? [
          { operation: 'history.daily', params: { date } },
          ...(['ecg', 'bodyComposition'] as MetricId[])
            .filter((metric) => this.metricSupported(metric))
            .map((metric) => ({
              operation: 'history.metric',
              params: { metric, date },
            })),
        ]
        : [{
          operation: cachedIOSRead ? 'history.cached' : 'history.daily',
          params: { date },
        }];
    const operations: Array<{ operation: string; params?: Record<string, unknown> }> = [
      ...(includeSessionOperations
        ? [
          { operation: 'device.battery' },
          { operation: 'device.time' },
          { operation: 'history.activity.current' },
        ]
        : []),
      ...historyOperations,
    ];

    let completed = 0;
    let failed = 0;
    let operationTimedOut = false;
    let failedOperation: string | undefined;
    let failureReason: HBandSyncStatus['failureReason'];
    if (reportProgress) {
      this.setHistoryPhase('syncing');
      this.syncStatus.set({
        state: 'syncing', date, completed, total: operations.length, failed,
      });
    }
    for (const item of operations) {
      if (reportProgress) {
        this.syncStatus.update((status) => ({ ...status, current: item.operation }));
      }
      try {
        await this.execute(item.operation, item.params ?? {});
      } catch (error) {
        failed += 1;
        operationTimedOut = this.isOperationTimeout(error);
        failedOperation ??= item.operation === 'history.metric' && typeof item.params?.['metric'] === 'string'
          ? `${item.operation}.${item.params['metric']}`
          : item.operation;
        failureReason ??= operationTimedOut ? 'timeout' : 'error';
      }
      completed += 1;
      if (reportProgress) {
        this.syncStatus.update((status) => ({ ...status, completed, failed }));
      }
      /*
       * Um timeout significa que o SDK não confirmou o fim do comando. Não são
       * enviados mais comandos BLE nessa sincronização para evitar sobreposição.
       */
      if (operationTimedOut) {
        break;
      }
    }
    if (date === this.selectedHistoryDate) {
      await this.loadStoredDate(date, true);
    }
    if (reportProgress) {
      this.syncStatus.update((status) => ({
        ...status,
        state: failed > 0 ? 'partial' : 'complete',
        current: undefined,
        failedOperation,
        failureReason,
      }));
      this.setHistoryPhase(
        failed > 0
          ? 'error'
          : this.historyState().recordCount > 0 ? 'ready' : 'empty',
      );
    }
    return !operationTimedOut;
  }

  /**
   * No Android, as métricas automáticas partilham o mesmo bloco de origem e
   * são lidas de uma vez. ECG e composição corporal conservam as operações
   * dedicadas porque usam áreas de armazenamento diferentes no dispositivo.
   */
  readHistory(metric: MetricId, date: string): Promise<void> {
    const originMetrics: MetricId[] = [
      'steps', 'heartRate', 'bloodPressure', 'oxygen', 'temperature',
      'bloodGlucose', 'stress',
    ];
    if (this.status().platform === 'android' && originMetrics.includes(metric)) {
      return this.execute('history.daily', { date });
    }
    return this.execute('history.metric', { metric, date });
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
          await this.executeNativeWithTimeout(operation, params);
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

  /**
   * Impõe um limite à Promise da bridge. O resultado tardio continua a poder
   * chegar ao SDK nativo, mas deixa de alterar o estado desta operação e não
   * mantém a fila Angular bloqueada indefinidamente.
   */
  private executeNativeWithTimeout(
    operation: string,
    params: Record<string, unknown>,
  ): Promise<void> {
    const timeoutMs = operation.startsWith('history.')
      ? HISTORY_OPERATION_TIMEOUT_MS
      : CONTROL_OPERATION_TIMEOUT_MS;
    return new Promise((resolve, reject) => {
      let settled = false;
      const timeout = setTimeout(() => {
        if (settled) {
          return;
        }
        settled = true;
        reject(new HBandOperationTimeoutError(operation, timeoutMs));
      }, timeoutMs);
      void this.nativeExecute(operation, params).then(
        () => {
          if (settled) {
            return;
          }
          settled = true;
          clearTimeout(timeout);
          resolve();
        },
        (error) => {
          if (settled) {
            return;
          }
          settled = true;
          clearTimeout(timeout);
          reject(error);
        },
      );
    });
  }

  private isOperationTimeout(error: unknown): boolean {
    return error instanceof HBandOperationTimeoutError
      || (error instanceof Error && error.message.includes('SDK_OPERATION_TIMEOUT'));
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
   * Remove o resultado da sessão anterior antes de iniciar uma nova leitura.
   * O histórico persistente não é alterado até existir confirmação explícita.
   */
  private prepareMeasurement(metric: MetricId): void {
    this.data.update((data) => ({ ...data, [metric]: undefined }));
    this.finalMeasurements.update((measurements) => ({
      ...measurements,
      [metric]: undefined,
    }));
  }

  /**
   * Alterna a mesma ação visual entre os contratos nativos de início e fim.
   * Ao iniciar, o estado fica ativo antes do comando para que um callback final
   * muito rápido não volte a deixar a interface presa numa medição terminada.
   */
  async toggleMeasurement(metric: MetricId, startOperation: string, stopOperation: string): Promise<void> {
    const active = this.measurementActive(metric);
    if (active) {
      await this.execute(stopOperation);
      this.activeMeasurements.update((measurements) => ({
        ...measurements,
        [metric]: false,
      }));
      return;
    }

    this.prepareMeasurement(metric);
    this.activeMeasurements.update((measurements) => ({
      ...measurements,
      [metric]: true,
    }));
    try {
      await this.execute(startOperation);
    } catch (error) {
      this.activeMeasurements.update((measurements) => ({
        ...measurements,
        [metric]: false,
      }));
      throw error;
    }
  }

  /**
   * Guarda uma medição manual apenas depois da confirmação da pessoa. O
   * timestamp real define o dia, impedindo que uma leitura atual seja
   * acidentalmente associada a um dia antigo que estivesse aberto.
   */
  async archiveCurrentMeasurement(metric: MetricId): Promise<string | null> {
    const event = this.finalFor(metric) ?? this.latestFor(metric);
    const deviceId = this.historyDeviceId;
    if (!event || !deviceId) {
      return null;
    }
    const parsedTimestamp = new Date(event.timestamp);
    const timestamp = Number.isNaN(parsedTimestamp.getTime())
      ? new Date().toISOString()
      : event.timestamp;
    const date = this.localDate(new Date(timestamp));
    const archivedEvent: HBandDataEvent = {
      type: 'history',
      metric,
      date,
      timestamp: new Date().toISOString(),
      values: { records: 1 },
      records: [{
        timestamp,
        values: { ...event.values },
        samples: event.samples?.length ? [...event.samples] : undefined,
        source: 'manual',
      }],
      samples: event.samples?.length ? [...event.samples] : undefined,
      raw: event.raw,
    };
    const merged = await this.historyRepository.mergeEvent(deviceId, archivedEvent);
    this.historyDates.update((dates) => [...new Set([...dates, date])].sort());
    /*
     * A confirmação muda imediatamente o arquivo ativo para o dia real da
     * leitura. Carregar o retrato completo evita misturar as outras métricas
     * do dia anteriormente aberto com o novo resultado.
     */
    this.selectedHistoryDate = date;
    const snapshot = await this.historyRepository.loadDate(deviceId, date);
    this.history.set({ ...snapshot.events, [metric]: merged });
    this.historyState.set({
      date,
      phase: 'ready',
      source: 'local',
      recordCount: snapshot.recordCount,
      lastUpdatedAt: snapshot.updatedAt ?? new Date().toISOString(),
      retentionDays: this.historyRetentionDays(),
    });
    return date;
  }

  private storeStatus(status: HBandStatus): void {
    const previous = this.status();
    const wasConnected = previous.state === 'connected';
    const previousDeviceId = this.historyDeviceId;
    if (status.device?.id) {
      this.historyDeviceId = status.device.id;
      this.rememberHistoryDevice(status.device.id);
    }
    this.status.set(status);
    const retentionChanged = previous.historyRetentionDays !== status.historyRetentionDays;
    const deviceChanged = previousDeviceId !== this.historyDeviceId;
    if (deviceChanged) {
      this.historyDates.set([]);
      void this.refreshHistoryDates();
    }
    if (status.state === 'connected' && (!wasConnected || retentionChanged || deviceChanged)) {
      this.startBatteryUpdates();
      void this.synchroniseRetainedHistory();
    }
    if (status.state === 'disconnected' || status.state === 'unavailable') {
      this.stopBatteryUpdates();
      this.activeMeasurements.set({});
      this.battery.set(null);
      this.monitoringSettings.set({});
    }
  }

  /**
   * Atualiza a bateria em segundo plano sem introduzir comandos BLE durante
   * uma medição ou sincronização já em curso.
   */
  private startBatteryUpdates(): void {
    if (this.batteryRefreshTimer !== null) {
      return;
    }
    this.batteryRefreshTimer = setInterval(() => {
      const measurementRunning = Object.values(this.activeMeasurements()).some(Boolean);
      if (
        this.connected()
        && !measurementRunning
        && this.busyOperation() === null
        && this.syncStatus().state !== 'syncing'
      ) {
        void this.refreshBattery().catch(() => undefined);
      }
    }, BATTERY_REFRESH_INTERVAL_MS);
  }

  private stopBatteryUpdates(): void {
    if (this.batteryRefreshTimer === null) {
      return;
    }
    clearInterval(this.batteryRefreshTimer);
    this.batteryRefreshTimer = null;
  }

  private metricSupported(metric: MetricId): boolean {
    const capability = metric === 'oxygen' ? 'bloodOxygen' : metric;
    return this.capability(capability) !== 'unsupported';
  }

  private localDate(date: Date): string {
    const offset = date.getTimezoneOffset() * 60_000;
    return new Date(date.getTime() - offset).toISOString().slice(0, 10);
  }

  /**
   * Preenche automaticamente todos os dias que a pulseira ainda consegue
   * fornecer. No iOS, a primeira leitura atualiza a base Veepoo e os restantes
   * dias são consultados nessa base, evitando repetir downloads BLE completos.
   */
  private synchroniseRetainedHistory(): Promise<void> {
    if (this.backgroundSyncPromise) {
      this.backgroundSyncRequested = true;
      return this.backgroundSyncPromise;
    }
    this.backgroundSyncRequested = false;
    this.backgroundSyncPromise = (async () => {
      const today = this.localDate(new Date());
      const retentionDays = this.historyRetentionDays();
      let completedWithoutTimeout = true;
      for (let offset = 0; offset <= retentionDays; offset += 1) {
        if (!this.connected()) {
          break;
        }
        const date = this.dateWithOffset(today, -offset);
        const canContinue = await this.synchroniseRequestedDate(
          date,
          offset === 0,
          date === this.selectedHistoryDate,
          this.status().platform === 'ios' && offset > 0,
        );
        if (!canContinue) {
          completedWithoutTimeout = false;
          break;
        }
      }
      /*
       * Não volta a carregar o arquivo depois de um timeout: isso substituiria
       * o estado de erro por "sem dados" e esconderia a causa ao utilizador.
       */
      if (this.selectedHistoryDate && completedWithoutTimeout) {
        await this.loadStoredDate(this.selectedHistoryDate, true);
      }
    })()
      .catch((error) => this.fail('history.backgroundSync', error))
      .finally(() => {
        this.backgroundSyncPromise = null;
        if (this.backgroundSyncRequested && this.connected()) {
          void this.synchroniseRetainedHistory();
        }
      });
    return this.backgroundSyncPromise;
  }

  /**
   * Carrega apenas o arquivo associado à pulseira atual. A mudança de data ou
   * uma ligação inexistente nunca reutilizam dados de outra MF91.
   */
  private async loadStoredDate(date: string, preserveSource = false): Promise<void> {
    const deviceId = this.historyDeviceId;
    this.historyState.update((state) => ({
      ...state,
      date,
      phase: 'loading',
      retentionDays: this.historyRetentionDays(),
    }));
    if (!deviceId) {
      if (date === this.selectedHistoryDate) {
        this.history.set({});
        this.historyState.set({
          date,
          phase: 'empty',
          source: 'none',
          recordCount: 0,
          retentionDays: this.historyRetentionDays(),
        });
      }
      return;
    }
    const snapshot = await this.historyRepository.loadDate(deviceId, date);
    if (date !== this.selectedHistoryDate) {
      return;
    }
    const current = this.historyState();
    this.history.set(snapshot.events);
    this.historyState.set({
      date,
      phase: snapshot.recordCount > 0 ? 'ready' : 'empty',
      source: preserveSource && current.source !== 'none'
        ? current.source
        : snapshot.recordCount > 0 ? 'local' : 'none',
      recordCount: snapshot.recordCount,
      lastUpdatedAt: snapshot.updatedAt,
      retentionDays: this.historyRetentionDays(),
    });
  }

  private setHistoryPhase(phase: HBandHistoryState['phase']): void {
    this.historyState.update((state) => ({
      ...state,
      phase,
      retentionDays: this.historyRetentionDays(),
    }));
  }

  private historyRetentionDays(): number {
    const value = this.status().historyRetentionDays;
    return typeof value === 'number' && Number.isFinite(value)
      ? Math.max(0, Math.floor(value))
      : 0;
  }

  private daysBetween(from: string, to: string): number {
    const start = Date.parse(`${from}T00:00:00Z`);
    const end = Date.parse(`${to}T00:00:00Z`);
    return Number.isFinite(start) && Number.isFinite(end)
      ? Math.round((end - start) / 86_400_000)
      : Number.POSITIVE_INFINITY;
  }

  private dateWithOffset(date: string, offset: number): string {
    const value = new Date(`${date}T12:00:00Z`);
    value.setUTCDate(value.getUTCDate() + offset);
    return value.toISOString().slice(0, 10);
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
    if (event.type === 'battery') {
      const percent = event.values['percent'];
      if (typeof percent === 'number' && Number.isFinite(percent)) {
        this.battery.set({
          percent: Math.min(100, Math.max(0, percent)),
          lowBattery: event.values['lowBattery'] === true,
          updatedAt: event.timestamp || new Date().toISOString(),
        });
      }
      return;
    }
    if (event.type === 'monitoring') {
      const metricValue = event.values['metric'];
      const metric = typeof metricValue === 'string'
        ? this.metricForType(metricValue)
        : undefined;
      const enabled = event.values['enabled'];
      if (!metric || typeof enabled !== 'boolean') {
        return;
      }
      const numeric = (key: string): number => {
        const value = event.values[key];
        return typeof value === 'number' && Number.isFinite(value) ? value : 0;
      };
      this.monitoringSettings.update((settings) => ({
        ...settings,
        [metric]: {
          metric,
          enabled,
          intervalMinutes: numeric('intervalMinutes'),
          startMinute: numeric('startMinute'),
          endMinute: numeric('endMinute'),
          intervalEditable: event.values['intervalEditable'] === true,
          windowEditable: event.values['windowEditable'] === true,
          minimumStepMinutes: numeric('minimumStepMinutes'),
          scheduleAvailable: event.values['scheduleAvailable'] !== false,
        },
      }));
      this.appendLog({
        id: this.nextLogId(event.type),
        timestamp: event.timestamp || new Date().toISOString(),
        level: 'success',
        message: event.type,
        metric,
        kind: 'data',
        data: { ...event, metric },
      });
      return;
    }
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
      if (!event.date || !event.records?.length) {
        return;
      }
      /*
       * A origem é emitida pela bridge porque só a operação nativa sabe se o
       * bloco veio da monitorização ou de uma tabela de medições manuais.
       */
      const readingSource = event.readingSource
        ?? (metric === 'ecg' || metric === 'bodyComposition' ? 'manual' : 'automatic');
      const sourcedEvent: HBandDataEvent = {
        ...event,
        metric,
        readingSource,
        records: event.records.map((record) => ({
          ...record,
          source: record.source ?? readingSource,
        })),
      };
      if (event.date === this.selectedHistoryDate) {
        this.history.update((history) => ({
          ...history,
          [metric]: this.mergeHistoryEvents(history[metric], sourcedEvent),
        }));
      }
      const deviceId = this.historyDeviceId;
      if (deviceId) {
        void this.historyRepository.mergeEvent(deviceId, sourcedEvent).then((merged) => {
          this.historyDates.update((dates) =>
            [...new Set([...dates, event.date as string])].sort());
          if (event.date !== this.selectedHistoryDate) {
            return;
          }
          this.history.update((history) => ({ ...history, [metric]: merged }));
          const current = this.historyState();
          const source = current.source === 'local' || current.source === 'merged'
            ? 'merged'
            : 'device';
          this.historyState.set({
            ...current,
            phase: 'ready',
            source,
            recordCount: Object.values(this.history()).reduce(
              (total, item) => total + (item?.records?.length ?? 0),
              0,
            ),
            lastUpdatedAt: new Date().toISOString(),
          });
        }).catch((error) => this.fail('history.persist', error, metric));
      }
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
    return false;
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
      'ecg', 'bodyComposition', 'stress', 'steps',
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
    if (operation === 'history.activity.current') {
      return 'steps';
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
      'ecg', 'bodyComposition', 'stress', 'steps', 'autoMeasure',
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
    this.storeStatus({
      ...this.status(),
      historyRetentionDays: 3,
      state: 'connected',
      device: { ...device, firmware: 'simulation' },
    });
    this.battery.set({ percent: 82, lowBattery: false, updatedAt: new Date().toISOString() });
  }

  private simulateOperation(operation: string, params: Record<string, unknown>): void {
    const now = new Date().toISOString();
    const live: Record<string, HBandDataEvent> = {
      'measure.heartRate.start': { type: 'heartRate', metric: 'heartRate', timestamp: now, values: { bpm: 74 }, samples: [69, 71, 73, 72, 75, 77, 74] },
      'measure.bloodPressure.start': { type: 'bloodPressure', metric: 'bloodPressure', timestamp: now, values: { systolic: 121, diastolic: 78, pulseBpm: 73 } },
      'measure.oxygen.start': { type: 'oxygen', metric: 'oxygen', timestamp: now, values: { percent: 97 }, samples: [96, 97, 97, 98, 97] },
      'measure.temperature.start': { type: 'temperature', metric: 'temperature', timestamp: now, values: { celsius: 36.4 }, samples: [36.1, 36.2, 36.3, 36.4] },
      'measure.bloodGlucose.start': { type: 'bloodGlucose', metric: 'bloodGlucose', timestamp: now, values: { mmolL: 5.2 }, samples: [4.9, 5.1, 5.2] },
      'measure.ecg.start': { type: 'ecg', metric: 'ecg', timestamp: now, values: { bpm: 72 }, samples: [0, 12, 45, -18, -8, 2, 4, 42, -20, -7, 1, 3] },
      'measure.bodyComposition.start': { type: 'bodyComposition', metric: 'bodyComposition', timestamp: now, values: { bmi: 22.4, bodyFatPercent: 18.8, waterPercent: 58.2, muscleMassKg: 49.6, boneMassKg: 2.8, basalMetabolismKcal: 1540 } },
      'measure.stress.start': { type: 'stress', metric: 'stress', timestamp: now, values: { score: 38 }, samples: [32, 36, 44, 40, 38] },
      'history.activity.current': { type: 'steps', metric: 'steps', timestamp: now, values: { steps: 6842, distanceKm: 4.7, caloriesKcal: 286 } },
    };
    if (operation === 'device.battery') {
      this.storeData({
        type: 'battery',
        timestamp: now,
        values: { percent: 82, lowBattery: false },
      });
      return;
    }
    if (operation === 'monitoring.read') {
      for (const metric of [
        'heartRate', 'bloodPressure', 'oxygen', 'temperature', 'bloodGlucose', 'stress',
      ] as MetricId[]) {
        this.storeData({
          type: 'monitoring',
          timestamp: now,
          values: {
            metric,
            enabled: metric === 'heartRate' || metric === 'oxygen',
            intervalMinutes: 10,
            startMinute: 0,
            endMinute: 1439,
            intervalEditable: true,
            windowEditable: true,
            minimumStepMinutes: 5,
            scheduleAvailable: true,
          },
        });
      }
      return;
    }
    if (operation === 'monitoring.set') {
      const metric = params['metric'] as MetricId;
      const current = this.monitoringFor(metric);
      if (current) {
        this.storeData({
          type: 'monitoring',
          timestamp: now,
          values: { ...current, enabled: params['enabled'] === true },
        });
      }
      return;
    }
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
    if (operation === 'history.daily' || operation === 'history.cached') {
      const date = String(params['date']);
      for (const metric of CATALOG_METRICS.filter((item) => this.metricSupported(item))) {
        const records = this.simulatedHistory(metric, date);
        this.storeData({
          type: 'history',
          metric,
          date,
          timestamp: now,
          values: { records: records.length },
          records,
          samples: records.flatMap(
            (record) => record.samples ?? this.numericValues(record.values),
          ),
        });
      }
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
      ecg: [{ bpm: 71 }, { bpm: 73 }],
      bodyComposition: [{ bmi: 22.4, bodyFatPercent: 18.8, waterPercent: 58.2, muscleMassKg: 49.6 }],
      stress: [{ score: 31 }, { score: 44 }, { score: 38 }],
      steps: [
        { steps: 740, distanceKm: 0.5, caloriesKcal: 31 },
        { steps: 1860, distanceKm: 1.3, caloriesKcal: 78 },
        { steps: 2340, distanceKm: 1.6, caloriesKcal: 98 },
        { steps: 1902, distanceKm: 1.3, caloriesKcal: 79 },
      ],
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

  /**
   * Conserva apenas os dados necessários para retomar a mesma sessão BLE
   * depois de a aplicação ser reaberta.
   */
  private rememberDeviceSession(session: RememberedDeviceSession): void {
    localStorage.setItem(DEVICE_SESSION_KEY, JSON.stringify(session));
  }

  /**
   * Conserva apenas o identificador não secreto do arquivo selecionado. Assim,
   * desligar voluntariamente a pulseira não torna o histórico inacessível
   * depois de reiniciar a aplicação.
   */
  private rememberHistoryDevice(deviceId: string): void {
    localStorage.setItem(HISTORY_DEVICE_KEY, deviceId);
  }

  /**
   * Recupera apenas o identificador da última pulseira para permitir consultar
   * o respetivo arquivo antes de a ligação BLE ser restabelecida.
   */
  private rememberedDeviceId(): string | null {
    try {
      const historyDevice = localStorage.getItem(HISTORY_DEVICE_KEY);
      if (historyDevice) {
        return historyDevice;
      }
      const serialized = localStorage.getItem(DEVICE_SESSION_KEY);
      if (!serialized) {
        return null;
      }
      const session = JSON.parse(serialized) as Partial<RememberedDeviceSession>;
      return typeof session.deviceId === 'string' ? session.deviceId : null;
    } catch {
      return null;
    }
  }

  /**
   * Reconstitui os dias destacados a partir do arquivo da última pulseira,
   * incluindo quando a aplicação arranca sem ligação BLE.
   */
  private async refreshHistoryDates(): Promise<void> {
    const deviceId = this.historyDeviceId;
    if (!deviceId) {
      this.historyDates.set([]);
      return;
    }
    this.historyDates.set(await this.historyRepository.listDates(deviceId));
  }

  /**
   * Tenta ligar diretamente ao último endereço autenticado. A bridge nativa
   * continua responsável pelas novas tentativas se a pulseira estiver
   * temporariamente fora de alcance.
   */
  private async restoreDeviceSession(): Promise<void> {
    const serialized = localStorage.getItem(DEVICE_SESSION_KEY);
    if (!serialized) {
      return;
    }
    try {
      const session = JSON.parse(serialized) as Partial<RememberedDeviceSession>;
      if (typeof session.deviceId !== 'string' || typeof session.password !== 'string') {
        localStorage.removeItem(DEVICE_SESSION_KEY);
        return;
      }
      await HBand.connect({
        deviceId: session.deviceId,
        password: session.password,
      });
    } catch (error) {
      this.fail('session.restore', error);
    }
  }

  /**
   * Junta dados automáticos de cinco minutos e medições manuais sem duplicar
   * o mesmo instante/payload quando o SDK repete um bloco durante a leitura.
   */
  private mergeHistoryRecords(
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
      (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime(),
    );
  }

  private mergeHistoryEvents(
    previous: HBandDataEvent | undefined,
    incoming: HBandDataEvent,
  ): HBandDataEvent {
    const defaultSource: HBandReadingSource = incoming.metric === 'ecg'
      || incoming.metric === 'bodyComposition' ? 'manual' : 'automatic';
    const records = this.mergeHistoryRecords(
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
}
