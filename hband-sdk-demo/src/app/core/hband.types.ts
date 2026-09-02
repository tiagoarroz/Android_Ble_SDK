import type { PluginListenerHandle } from '@capacitor/core';

export type ConnectionState =
  | 'unavailable' | 'idle' | 'scanning' | 'connecting' | 'authenticating'
  | 'connected' | 'disconnected' | 'error';
export type CapabilityState = 'supported' | 'unsupported' | 'unknown';
export type MetricId =
  | 'heartRate' | 'bloodPressure' | 'oxygen' | 'temperature' | 'bloodGlucose'
  | 'ecg' | 'bodyComposition' | 'stress' | 'steps';
export type VisualizationType = 'gauge' | 'line' | 'bars' | 'ecg' | 'composition' | 'activity';
export type DataValue = string | number | boolean | null;
export type HBandReadingSource = 'automatic' | 'manual';

export interface HBandDevice {
  id: string;
  name: string;
  rssi?: number;
  model?: string;
  firmware?: string;
  hardware?: string;
}

export interface HBandStatus {
  available: boolean;
  bluetoothEnabled: boolean;
  state: ConnectionState;
  device?: HBandDevice;
  capabilities: Record<string, CapabilityState>;
  historyRetentionDays?: number;
  sdkVersion?: string;
  platform?: string;
}

export interface HBandBatteryStatus {
  percent: number;
  lowBattery: boolean;
  updatedAt: string;
}

export interface HBandMonitoringSetting {
  metric: MetricId;
  enabled: boolean;
  intervalMinutes: number;
  startMinute: number;
  endMinute: number;
  intervalEditable: boolean;
  windowEditable: boolean;
  minimumStepMinutes: number;
  scheduleAvailable: boolean;
}

export interface HBandSyncStatus {
  state: 'idle' | 'syncing' | 'complete' | 'partial';
  date?: string;
  current?: string;
  failedOperation?: string;
  failureReason?: 'timeout' | 'error';
  completed: number;
  total: number;
  failed: number;
}

export type HBandHistorySource = 'none' | 'local' | 'device' | 'merged';
export type HBandHistoryPhase =
  | 'idle' | 'loading' | 'syncing' | 'ready' | 'offline'
  | 'outsideRetention' | 'empty' | 'error';

export interface HBandHistoryState {
  date: string;
  phase: HBandHistoryPhase;
  source: HBandHistorySource;
  recordCount: number;
  lastUpdatedAt?: string;
  retentionDays?: number;
}

export interface HBandHistoryRecord {
  timestamp: string;
  values: Record<string, DataValue>;
  samples?: number[];
  source?: HBandReadingSource;
}

export interface HBandDataEvent {
  type: string;
  timestamp: string;
  date?: string;
  metric?: MetricId;
  values: Record<string, DataValue>;
  samples?: number[];
  records?: HBandHistoryRecord[];
  readingSource?: HBandReadingSource;
  raw?: string;
}

export interface HBandLogEntry {
  id: string;
  timestamp: string;
  level: 'info' | 'success' | 'warning' | 'error';
  message: string;
  operation?: string;
  metric?: MetricId;
  kind?: 'bridge' | 'operation' | 'data';
  data?: HBandDataEvent;
}

export interface HBandOperationOptions {
  operation: string;
  params?: Record<string, unknown>;
}

/**
 * Resultado da sessão temporária usada para registar por NFC uma pulseira que
 * ainda não pertence à sessão atual. `signalling` distingue uma pulseira que
 * confirmou o pedido de vibração de uma que apenas aceitou a ligação.
 */
export interface HBandBandSignal {
  deviceId: string;
  signalling: boolean;
  findSupported: boolean;
}

export interface HBandOperationResult {
  operation: string;
  accepted: boolean;
  data?: Record<string, unknown>;
}

export interface HBandPlugin {
  getStatus(): Promise<HBandStatus>;
  requestPermissions(): Promise<{ granted: boolean }>;
  startScan(options?: { timeoutMs?: number }): Promise<void>;
  stopScan(): Promise<void>;
  connect(options: {
    deviceId: string;
    password?: string;
    name?: string;
    model?: string;
  }): Promise<void>;
  disconnect(): Promise<void>;
  execute(options: HBandOperationOptions): Promise<HBandOperationResult>;
  addListener(eventName: 'deviceFound', listener: (device: HBandDevice) => void): Promise<PluginListenerHandle>;
  addListener(eventName: 'statusChanged', listener: (status: HBandStatus) => void): Promise<PluginListenerHandle>;
  addListener(eventName: 'data', listener: (event: HBandDataEvent) => void): Promise<PluginListenerHandle>;
  addListener(eventName: 'log', listener: (entry: HBandLogEntry) => void): Promise<PluginListenerHandle>;
  removeAllListeners(): Promise<void>;
}

export interface FeatureAction {
  id: string;
  operation: string;
  stopOperation?: string;
  labelKey: string;
  activeLabelKey?: string;
  tone?: 'primary' | 'secondary' | 'danger';
  metric?: MetricId;
}

export interface FeatureDefinition {
  id: string;
  metric: MetricId;
  titleKey: string;
  descriptionKey: string;
  icon: string;
  capability: string;
  visualization: VisualizationType;
  actions: FeatureAction[];
}
