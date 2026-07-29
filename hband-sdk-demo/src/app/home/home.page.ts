import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  IonBadge, IonButton, IonButtons, IonChip, IonContent, IonHeader, IonIcon,
  IonInput, IonItem, IonLabel, IonList, IonModal, IonNote, IonProgressBar,
  IonSelect, IonSelectOption, IonSpinner, IonTitle, IonToolbar,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  analyticsOutline, bluetoothOutline, bodyOutline, calendarOutline, checkmarkCircle,
  chevronForwardOutline, closeCircleOutline, fitnessOutline, flashOutline, heartOutline,
  helpCircleOutline, informationCircleOutline, leafOutline, medicalOutline, pulseOutline,
  radioOutline, refreshOutline, speedometerOutline, thermometerOutline, watchOutline,
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
    IonButtons, IonChip, IonContent, IonHeader, IonIcon, IonInput, IonItem,
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
  readonly password = signal('0000');
  readonly selectedDate = signal(this.localDate(new Date()));
  readonly today = this.localDate(new Date());

  constructor() {
    addIcons({
      analyticsOutline, bluetoothOutline, bodyOutline, calendarOutline, checkmarkCircle,
      chevronForwardOutline, closeCircleOutline, fitnessOutline, flashOutline, heartOutline,
      helpCircleOutline, informationCircleOutline, leafOutline, medicalOutline, pulseOutline,
      radioOutline, refreshOutline, speedometerOutline, thermometerOutline, watchOutline,
      waterOutline,
    });
  }

  async ngOnInit(): Promise<void> {
    await this.hband.initialize();
  }

  async openScan(): Promise<void> {
    this.scanOpen.set(true);
    await this.hband.scan();
  }

  async connect(deviceId: string): Promise<void> {
    await this.hband.connect(deviceId, this.password());
    this.scanOpen.set(false);
  }

  /**
   * Alterna as medições com um único controlo e acrescenta a data apenas à
   * operação histórica, sem alterar os contratos confirmados da bridge.
   */
  async run(action: FeatureAction, feature: FeatureDefinition): Promise<void> {
    if (action.stopOperation) {
      await this.hband.toggleMeasurement(feature.metric, action.operation, action.stopOperation);
      return;
    }
    const params = action.operation === 'history.metric'
      ? { metric: action.metric ?? feature.metric, date: this.selectedDate() }
      : {};
    await this.hband.execute(action.operation, params);
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

  setDate(value: string | number | null | undefined): void {
    this.selectedDate.set(String(value ?? this.today).slice(0, 10));
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

  private localDate(date: Date): string {
    const offset = date.getTimezoneOffset() * 60_000;
    return new Date(date.getTime() - offset).toISOString().slice(0, 10);
  }
}
