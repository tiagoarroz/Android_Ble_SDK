import type { PluginListenerHandle } from '@capacitor/core';

export type ConnectionState =
  | 'unavailable'
  | 'idle'
  | 'scanning'
  | 'connecting'
  | 'authenticating'
  | 'connected'
  | 'disconnected'
  | 'error';

export type CapabilityState = 'supported' | 'unsupported' | 'unknown';
export type FeatureCategory =
  | 'connection'
  | 'measurements'
  | 'history'
  | 'automation'
  | 'interaction'
  | 'advanced';
export type VisualizationType =
  | 'metric'
  | 'gauge'
  | 'line'
  | 'bars'
  | 'sleep'
  | 'timeline'
  | 'table'
  | 'log';

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
  sdkVersion?: string;
  platform?: string;
}

export interface HBandDataEvent {
  type: string;
  timestamp: string;
  values: Record<string, string | number | boolean | null>;
  samples?: number[];
  raw?: string;
}

export interface HBandLogEntry {
  id: string;
  timestamp: string;
  level: 'info' | 'success' | 'warning' | 'error';
  message: string;
  operation?: string;
}

export interface HBandOperationOptions {
  operation: string;
  params?: Record<string, unknown>;
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
  connect(options: { deviceId: string; password?: string }): Promise<void>;
  disconnect(): Promise<void>;
  execute(options: HBandOperationOptions): Promise<HBandOperationResult>;
  addListener(
    eventName: 'deviceFound',
    listener: (device: HBandDevice) => void,
  ): Promise<PluginListenerHandle>;
  addListener(
    eventName: 'statusChanged',
    listener: (status: HBandStatus) => void,
  ): Promise<PluginListenerHandle>;
  addListener(
    eventName: 'data',
    listener: (event: HBandDataEvent) => void,
  ): Promise<PluginListenerHandle>;
  addListener(
    eventName: 'log',
    listener: (entry: HBandLogEntry) => void,
  ): Promise<PluginListenerHandle>;
  removeAllListeners(): Promise<void>;
}

export interface FeatureAction {
  id: string;
  operation: string;
  labelKey: string;
  tone?: 'primary' | 'secondary' | 'danger';
}

export interface FeatureDefinition {
  id: string;
  category: FeatureCategory;
  titleKey: string;
  descriptionKey: string;
  icon: string;
  capability?: string;
  visualization: VisualizationType;
  actions: FeatureAction[];
  advanced?: boolean;
}
