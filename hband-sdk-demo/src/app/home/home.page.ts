import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  IonBadge, IonButton, IonButtons, IonChip, IonContent, IonHeader, IonIcon,
  IonDatetime, IonInput, IonItem, IonLabel, IonList, IonModal, IonNote, IonProgressBar,
  IonSelect, IonSelectOption, IonSpinner, IonTitle, IonToolbar,
} from '@ionic/angular/standalone';
import type { DatetimeHighlight } from '@ionic/core';
import { addIcons } from 'ionicons';
import {
  albumsOutline, analyticsOutline, batteryDeadOutline, batteryFullOutline, batteryHalfOutline, bluetoothOutline,
  bodyOutline, calendarOutline, checkmarkCircle,
  closeCircleOutline, cloudOfflineOutline, fitnessOutline, flashOutline, heartOutline,
  footstepsOutline, helpCircleOutline, informationCircleOutline, leafOutline, medicalOutline, pulseOutline,
  refreshOutline, speedometerOutline, syncOutline, thermometerOutline, watchOutline,
  waterOutline,
} from 'ionicons/icons';

import { DataVisualizerComponent } from '../components/data-visualizer/data-visualizer.component';
import { FEATURE_CATALOG } from '../core/feature-catalog';
import { HBandService } from '../core/hband.service';
import type {
  FeatureAction, FeatureDefinition, HBandDataEvent, HBandLogEntry,
} from '../core/hband.types';
import { I18nService } from '../core/i18n.service';

@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  imports: [
    CommonModule, FormsModule, DataVisualizerComponent, IonBadge, IonButton,
    IonButtons, IonChip, IonContent, IonDatetime, IonHeader, IonIcon, IonInput, IonItem,
    IonLabel, IonList, IonModal, IonNote, IonProgressBar, IonSelect,
    IonSelectOption, IonSpinner, IonTitle, IonToolbar,
  ],
})
export class HomePage implements OnInit {
  readonly hband = inject(HBandService);
  readonly i18n = inject(I18nService);
  readonly features = FEATURE_CATALOG;
  readonly selectedFeature = signal<FeatureDefinition | null>(null);
  readonly scanOpen = signal(false);
  readonly historyCalendarOpen = signal(false);
  readonly password = signal('0000');
  readonly selectedDate = signal(this.localDate(new Date()));
  readonly today = this.localDate(new Date());

  constructor() {
    addIcons({
      albumsOutline, analyticsOutline, batteryDeadOutline, batteryFullOutline, batteryHalfOutline, bluetoothOutline,
      bodyOutline, calendarOutline, checkmarkCircle,
      closeCircleOutline, cloudOfflineOutline, fitnessOutline, flashOutline, heartOutline,
      footstepsOutline, helpCircleOutline, informationCircleOutline, leafOutline, medicalOutline, pulseOutline,
      refreshOutline, speedometerOutline, syncOutline, thermometerOutline, watchOutline,
      waterOutline,
    });
  }

  async ngOnInit(): Promise<void> {
    await this.hband.initialize();
    if (!this.hband.connected()) {
      await this.hband.synchroniseDate(this.selectedDate());
    }
  }

  async openScan(): Promise<void> {
    this.scanOpen.set(true);
    await this.hband.scan();
  }

  async connect(deviceId: string): Promise<void> {
    await this.hband.connect(deviceId, this.password());
    this.scanOpen.set(false);
  }

  syncPercent(): number {
    const sync = this.hband.syncStatus();
    return sync.total > 0 ? Math.round((sync.completed / sync.total) * 100) : 0;
  }

  /**
   * Alterna as medições com um único controlo ou executa uma leitura pontual,
   * sem alterar os contratos confirmados da bridge.
   */
  async run(action: FeatureAction, feature: FeatureDefinition): Promise<void> {
    if (action.stopOperation) {
      await this.hband.toggleMeasurement(feature.metric, action.operation, action.stopOperation);
      return;
    }
    await this.hband.execute(action.operation);
  }

  actionOperation(action: FeatureAction, feature: FeatureDefinition): string {
    return action.stopOperation && this.hband.measurementActive(feature.metric)
      ? action.stopOperation
      : action.operation;
  }

  actionLabel(action: FeatureAction, feature: FeatureDefinition): string {
    const labelKey = action.stopOperation
      && this.hband.measurementActive(feature.metric)
      && action.activeLabelKey
      ? action.activeLabelKey
      : action.labelKey;
    return this.i18n.translate(labelKey);
  }

  /**
   * Expõe o payload integral recebido da bridge dentro de uma área com scroll,
   * incluindo amostras, registos históricos e valor bruto quando disponíveis.
   */
  formatLogPayload(entry: HBandLogEntry): string {
    if (!entry.data) {
      return entry.message;
    }
    return JSON.stringify({
      values: entry.data.values,
      samples: entry.data.samples,
      records: entry.data.records,
      raw: entry.data.raw,
    }, null, 2);
  }

  logTitle(entry: HBandLogEntry): string {
    if (entry.kind === 'data') {
      return this.i18n.translate('measurementLogs.deviceData');
    }
    if (entry.kind === 'operation') {
      const key = entry.level === 'success'
        ? 'measurementLogs.operationAccepted'
        : entry.level === 'error'
          ? 'measurementLogs.operationFailed'
          : 'measurementLogs.operationRequested';
      return this.i18n.translate(key);
    }
    return this.i18n.translate('measurementLogs.bridge');
  }

  /**
   * Atualiza a data apresentada e abre imediatamente o arquivo desse dia.
   * Se existir ligação, o serviço tenta também obter dados mais recentes.
   */
  async setDate(value: string | number | null | undefined): Promise<void> {
    const date = String(value ?? '').slice(0, 10);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date) || date === this.selectedDate()) {
      return;
    }
    this.selectedDate.set(date);
    await this.hband.synchroniseDate(date);
  }

  /**
   * Fecha o calendário depois de selecionar um dia e reutiliza o mesmo fluxo
   * de carregamento local e atualização BLE do seletor anterior.
   */
  async selectCalendarDate(value: string | string[] | null | undefined): Promise<void> {
    const selected = Array.isArray(value) ? value[0] : value;
    await this.setDate(selected);
    this.historyCalendarOpen.set(false);
  }

  /**
   * O `ion-datetime` recebe uma lista derivada do índice persistente da
   * pulseira atual, não apenas das métricas carregadas no ecrã.
   */
  historyHighlightedDates(): DatetimeHighlight[] {
    return this.hband.historyDates().map((date) => ({
      date,
      backgroundColor: '#13795b',
      textColor: '#ffffff',
      border: '1px solid #0b5f46',
    }));
  }

  formattedSelectedDate(): string {
    return new Intl.DateTimeFormat(this.locale(), { dateStyle: 'medium' })
      .format(new Date(`${this.selectedDate()}T12:00:00`));
  }

  /**
   * Explica se o dia veio do arquivo local, da pulseira ou da união de ambos,
   * distinguindo a ausência de ligação da retenção já ultrapassada.
   */
  historyStatusLabel(): string {
    const state = this.hband.historyState();
    const hasData = state.recordCount > 0;
    const key = state.phase === 'offline'
      ? hasData ? 'history.status.offlineWithData' : 'history.status.offlineEmpty'
      : state.phase === 'outsideRetention'
        ? hasData ? 'history.status.outsideRetentionWithData' : 'history.status.outsideRetentionEmpty'
        : state.phase === 'error'
          ? hasData ? 'history.status.errorWithData' : 'history.status.errorEmpty'
          : `history.status.${state.phase}`;
    return this.i18n.translate(key);
  }

  historyStatusIcon(): string {
    const phase = this.hband.historyState().phase;
    return phase === 'syncing' || phase === 'loading'
      ? 'sync-outline'
      : phase === 'offline' ? 'cloud-offline-outline' : 'albums-outline';
  }

  historyMetadata(): string {
    const state = this.hband.historyState();
    const parts: string[] = [];
    if (state.source !== 'none') {
      parts.push(this.i18n.translate(`history.sources.${state.source}`));
    }
    parts.push(this.i18n.translate('history.records', { count: state.recordCount }));
    if (state.lastUpdatedAt) {
      parts.push(this.i18n.translate('history.updatedAt', {
        date: new Intl.DateTimeFormat(this.locale(), {
          dateStyle: 'short',
          timeStyle: 'short',
        }).format(new Date(state.lastUpdatedAt)),
      }));
    }
    return parts.join(' · ');
  }

  historyRetentionLabel(): string {
    return this.i18n.translate('history.retention', {
      days: this.hband.historyState().retentionDays ?? 0,
    });
  }

  measurementProgress(event: HBandDataEvent | undefined): number {
    const value = event?.values['progress'];
    return typeof value === 'number' && Number.isFinite(value)
      ? Math.min(100, Math.max(0, value))
      : 0;
  }

  formatGlucose(event: HBandDataEvent): string {
    const value = event.values['mmolL'];
    if (typeof value !== 'number') {
      return '—';
    }
    const locale = this.i18n.language() === 'br' ? 'pt-BR' : this.i18n.language();
    return new Intl.NumberFormat(locale, {
      minimumFractionDigits: 1,
      maximumFractionDigits: 2,
    }).format(value);
  }

  capability(feature: FeatureDefinition): 'supported' | 'unsupported' | 'unknown' {
    return this.hband.capability(feature.capability);
  }

  statusLabel(): string {
    return this.i18n.translate(`connectionStates.${this.hband.status().state}`);
  }

  statusIcon(feature: FeatureDefinition): string {
    const state = this.capability(feature);
    return state === 'supported'
      ? 'checkmark-circle'
      : state === 'unsupported' ? 'close-circle-outline' : 'help-circle-outline';
  }

  batteryPercent(): number {
    return Math.min(100, Math.max(0, this.hband.battery()?.percent ?? 0));
  }

  /**
   * Escolhe um ícone que permite reconhecer visualmente a autonomia sem
   * depender apenas do valor percentual.
   */
  batteryIcon(): string {
    const level = this.batteryLevel();
    return level === 'low'
      ? 'battery-dead-outline'
      : level === 'medium' ? 'battery-half-outline' : 'battery-full-outline';
  }

  /**
   * Agrupa a carga em três estados visuais e respeita também o aviso de
   * bateria fraca enviado diretamente pelo dispositivo.
   */
  batteryLevel(): 'low' | 'medium' | 'high' {
    const battery = this.hband.battery();
    const percent = this.batteryPercent();
    if (battery?.lowBattery || percent <= 20) {
      return 'low';
    }
    return percent <= 60 ? 'medium' : 'high';
  }

  private localDate(date: Date): string {
    const offset = date.getTimezoneOffset() * 60_000;
    return new Date(date.getTime() - offset).toISOString().slice(0, 10);
  }

  locale(): string {
    return this.i18n.language() === 'br' ? 'pt-BR' : this.i18n.language();
  }
}
