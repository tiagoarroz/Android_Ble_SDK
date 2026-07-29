import { Injectable, computed, signal } from '@angular/core';
import { Capacitor } from '@capacitor/core';

import { HBand } from './hband.plugin';
import type {
  HBandDataEvent,
  HBandDevice,
  HBandLogEntry,
  HBandStatus,
} from './hband.types';

const INITIAL_STATUS: HBandStatus = {
  available: false,
  bluetoothEnabled: false,
  state: 'idle',
  capabilities: {},
  platform: Capacitor.getPlatform(),
};

const COMMON_NATIVE_OPERATIONS = new Set([
  'session.authenticate',
  'session.disconnect',
  'device.battery',
  'device.rssi',
  'device.time',
  'device.profile',
  'measure.heartRate.start',
  'measure.heartRate.stop',
  'measure.bloodPressure.start',
  'measure.bloodPressure.stop',
  'measure.oxygen.start',
  'measure.oxygen.stop',
  'measure.breathing.start',
  'measure.breathing.stop',
  'measure.temperature.start',
  'measure.temperature.stop',
  'measure.fatigue.start',
  'measure.fatigue.stop',
  'measure.hrv.start',
  'measure.hrv.stop',
  'measure.stress.start',
  'measure.stress.stop',
  'measure.gsr.start',
  'measure.gsr.stop',
  'measure.bloodGlucose.start',
  'measure.bloodGlucose.stop',
]);

const ANDROID_OPERATIONS = new Set([
  ...COMMON_NATIVE_OPERATIONS,
  'history.activity.current',
  'history.sleep',
  'history.origin',
  'history.oxygen',
  'history.hrv',
  'history.temperature',
]);

const IOS_OPERATIONS = new Set([
  ...COMMON_NATIVE_OPERATIONS,
  'measure.ecg.start',
  'measure.ecg.stop',
  'measure.bodyComposition.start',
  'measure.bodyComposition.stop',
  'measure.bloodComposition.start',
  'measure.bloodComposition.stop',
  'measure.miniCheckup.start',
]);

@Injectable({ providedIn: 'root' })
export class HBandService {
  readonly isNative = Capacitor.isNativePlatform();
  readonly status = signal<HBandStatus>(INITIAL_STATUS);
  readonly devices = signal<HBandDevice[]>([]);
  readonly data = signal<Record<string, HBandDataEvent>>({});
  readonly logs = signal<HBandLogEntry[]>([]);
  readonly busyOperation = signal<string | null>(null);
  readonly simulation = signal(!this.isNative);
  readonly connected = computed(() => this.status().state === 'connected');
  private queue: Promise<unknown> = Promise.resolve();

  /**
   * Liga os eventos do plugin uma única vez e recupera o estado nativo atual.
   */
  async initialize(): Promise<void> {
    if (this.simulation()) {
      this.seedSimulation();
      return;
    }

    await HBand.addListener('deviceFound', (device) => this.upsertDevice(device));
    await HBand.addListener('statusChanged', (status) => this.status.set(status));
    await HBand.addListener('data', (event) => this.storeData(event));
    await HBand.addListener('log', (entry) => this.appendLog(entry));

    try {
      this.status.set(await HBand.getStatus());
    } catch (error) {
      this.fail('session.initialize', error);
    }
  }

  /**
   * Solicita permissões e inicia uma pesquisa limitada no tempo.
   */
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

  async stopScan(): Promise<void> {
    if (this.simulation()) {
      this.status.update((status) => ({ ...status, state: 'idle' }));
      return;
    }
    await HBand.stopScan();
  }

  /**
   * A autenticação usa a password predefinida do SDK apenas quando o utilizador
   * não fornece outra; nenhum comando funcional é enviado antes da confirmação.
   */
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
      return;
    }
    await HBand.disconnect();
  }

  /**
   * Serializa todas as operações porque o SDK H Band não aceita interações
   * demoradas em paralelo com o mesmo periférico.
   */
  execute(operation: string, params: Record<string, unknown> = {}): Promise<void> {
    const task = async () => {
      this.busyOperation.set(operation);
      try {
        if (this.simulation()) {
          this.simulateOperation(operation);
        } else {
          await HBand.execute({ operation, params });
        }
      } catch (error) {
        this.fail(operation, error);
        throw error;
      } finally {
        this.busyOperation.set(null);
      }
    };
    this.queue = this.queue.then(task, task);
    return this.queue.then(() => undefined);
  }

  capability(name?: string): 'supported' | 'unsupported' | 'unknown' {
    if (!name) {
      return 'unknown';
    }
    return this.status().capabilities[name] ?? 'unknown';
  }

  /**
   * Distingue o catálogo integral do SDK dos comandos já ligados ao bridge de
   * cada plataforma, evitando apresentar uma operação inerte como executável.
   */
  operationAvailable(operation: string): boolean {
    if (this.simulation() || operation === 'session.scan') {
      return true;
    }
    const platform = this.status().platform ?? Capacitor.getPlatform();
    return platform === 'android'
      ? ANDROID_OPERATIONS.has(operation)
      : platform === 'ios' && IOS_OPERATIONS.has(operation);
  }

  latestFor(featureId: string): HBandDataEvent | undefined {
    const aliases: Record<string, string[]> = {
      'device-status': ['battery', 'rssi'],
      'heart-rate': ['heartRate'],
      'blood-pressure': ['bloodPressure'],
      oxygen: ['oxygen'],
      breathing: ['breathing'],
      temperature: ['temperature'],
      hrv: ['hrv'],
      ecg: ['ecg'],
      'blood-glucose': ['bloodGlucose'],
      'fatigue-stress': ['fatigue', 'stress'],
      'body-composition': ['bodyComposition'],
      'blood-composition': ['bloodComposition'],
      'gsr-mini': ['gsr', 'miniCheckup'],
      'daily-activity': ['activity'],
      sleep: ['sleep'],
      'health-history': ['allHealth', 'origin'],
      'sport-history': ['sport'],
      'clinical-history': ['manual', 'rr'],
    };
    return aliases[featureId]?.map((type) => this.data()[type]).find(Boolean);
  }

  setSimulation(enabled: boolean): void {
    if (this.isNative) {
      this.simulation.set(enabled);
    }
    if (enabled) {
      this.seedSimulation();
    }
  }

  private upsertDevice(device: HBandDevice): void {
    this.devices.update((devices) => {
      const next = devices.filter((item) => item.id !== device.id);
      return [...next, device].sort((a, b) => (b.rssi ?? -100) - (a.rssi ?? -100));
    });
  }

  private storeData(event: HBandDataEvent): void {
    this.data.update((data) => ({ ...data, [event.type]: event }));
  }

  private appendLog(entry: HBandLogEntry): void {
    this.logs.update((logs) => [entry, ...logs].slice(0, 120));
  }

  private fail(operation: string, error: unknown): void {
    const message = error instanceof Error ? error.message : String(error);
    this.appendLog({
      id: `${Date.now()}-${operation}`,
      timestamp: new Date().toISOString(),
      level: 'error',
      message,
      operation,
    });
  }

  private seedSimulation(): void {
    const capabilities: HBandStatus['capabilities'] = Object.fromEntries([
      'heartRate', 'bloodPressure', 'bloodOxygen', 'sleep', 'sport', 'display',
      'findDevice', 'camera', 'weather', 'alarms', 'autoMeasure', 'lowPower',
    ].map((capability) => [capability, 'supported' as const]));
    this.status.set({
      available: true,
      bluetoothEnabled: true,
      state: 'idle',
      capabilities,
      platform: 'web',
      sdkVersion: 'simulation',
    });
    this.storeData({
      type: 'heartRate',
      timestamp: new Date().toISOString(),
      values: { bpm: 72 },
      samples: [68, 70, 69, 73, 76, 74, 72, 71, 72],
    });
    this.storeData({
      type: 'activity',
      timestamp: new Date().toISOString(),
      values: { steps: 6842, distanceKm: 4.7, caloriesKcal: 328 },
      samples: [220, 480, 760, 1120, 680, 930, 1290, 842, 520],
    });
    this.storeData({
      type: 'sleep',
      timestamp: new Date().toISOString(),
      values: { totalMinutes: 438, deepMinutes: 112, lightMinutes: 286, awakeMinutes: 40 },
      samples: [0, 1, 1, 2, 2, 1, 2, 1, 0, 1, 2, 2, 1, 0],
    });
  }

  private simulateScan(): void {
    this.status.update((status) => ({ ...status, state: 'scanning' }));
    this.devices.set([]);
    window.setTimeout(() => {
      this.upsertDevice({ id: 'MF91-DEMO', name: 'MF91', rssi: -48, model: 'MF91' });
      this.status.update((status) => ({ ...status, state: 'idle' }));
    }, 450);
  }

  private simulateConnection(deviceId: string): void {
    const device = this.devices().find((item) => item.id === deviceId)
      ?? { id: deviceId, name: 'MF91', model: 'MF91', rssi: -48 };
    this.status.update((status) => ({ ...status, state: 'connecting' }));
    window.setTimeout(() => {
      this.status.update((status) => ({
        ...status,
        state: 'connected',
        device: { ...device, firmware: 'demo-1.0.0', hardware: 'MF91' },
      }));
      this.appendLog({
        id: `${Date.now()}-connected`,
        timestamp: new Date().toISOString(),
        level: 'success',
        message: 'MF91 simulation connected',
      });
    }, 650);
  }

  private simulateOperation(operation: string): void {
    const now = new Date().toISOString();
    const simulated: Record<string, HBandDataEvent> = {
      'device.battery': { type: 'battery', timestamp: now, values: { percent: 78, charging: false } },
      'device.rssi': { type: 'rssi', timestamp: now, values: { dbm: -51 } },
      'measure.heartRate.start': { type: 'heartRate', timestamp: now, values: { bpm: 74 }, samples: [69, 71, 73, 72, 75, 77, 74] },
      'measure.bloodPressure.start': { type: 'bloodPressure', timestamp: now, values: { systolic: 121, diastolic: 78, progress: 100 } },
      'measure.oxygen.start': { type: 'oxygen', timestamp: now, values: { percent: 97, pulseBpm: 73 }, samples: [96, 97, 97, 98, 97] },
      'measure.breathing.start': { type: 'breathing', timestamp: now, values: { breathsPerMinute: 16 }, samples: [15, 16, 16, 17, 16] },
      'measure.temperature.start': { type: 'temperature', timestamp: now, values: { celsius: 36.4 }, samples: [36.1, 36.2, 36.3, 36.4] },
      'measure.hrv.start': { type: 'hrv', timestamp: now, values: { milliseconds: 54 }, samples: [48, 52, 51, 57, 54] },
      'measure.fatigue.start': { type: 'fatigue', timestamp: now, values: { score: 32, progress: 100 } },
      'measure.stress.start': { type: 'stress', timestamp: now, values: { score: 38 }, samples: [32, 36, 44, 40, 38] },
      'measure.bloodGlucose.start': { type: 'bloodGlucose', timestamp: now, values: { mmolL: 5.2 }, samples: [4.9, 5.1, 5.2] },
      'history.activity.current': { type: 'activity', timestamp: now, values: { steps: 6842, distanceKm: 4.7, caloriesKcal: 328 }, samples: [220, 480, 760, 1120, 680, 930, 1290, 842, 520] },
    };
    const event = simulated[operation];
    if (event) {
      this.storeData(event);
    }
    this.appendLog({
      id: `${Date.now()}-${operation}`,
      timestamp: now,
      level: event ? 'success' : 'info',
      message: event ? 'Simulated data received' : 'Simulated operation accepted',
      operation,
    });
  }
}
