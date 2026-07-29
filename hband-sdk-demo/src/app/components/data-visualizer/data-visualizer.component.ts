import { CommonModule } from '@angular/common';
import { Component, Input, inject } from '@angular/core';

import type {
  HBandDataEvent, HBandHistoryRecord, MetricId, VisualizationType,
} from '../../core/hband.types';
import { I18nService } from '../../core/i18n.service';

interface ActivityPoint {
  timestamp: string;
  steps: number;
}

interface ReferenceItem {
  label: string;
  tone: 'calm' | 'normal' | 'attention' | 'high';
}

interface DailyBarPoint {
  height: number;
  tone: ReferenceItem['tone'];
  width: number;
  x: number;
  y: number;
}

@Component({
  selector: 'app-data-visualizer',
  templateUrl: './data-visualizer.component.html',
  imports: [CommonModule],
})
export class DataVisualizerComponent {
  readonly i18n = inject(I18nService);
  @Input({ required: true }) event!: HBandDataEvent;
  @Input({ required: true }) type!: VisualizationType;
  @Input() compact = false;
  temperatureHistoryField: 'celsius' | 'baselineCelsius' = 'celsius';

  entries(): Array<[string, string | number | boolean | null]> {
    const latest = this.event.records?.at(-1);
    return Object.entries(latest?.values ?? this.event.values).filter(([field]) => field !== 'records');
  }

  isHistory(): boolean {
    return this.event.type === 'history';
  }

  usesDailyOverview(): boolean {
    return this.isHistory()
      && !['activity', 'ecg', 'composition'].includes(this.type);
  }

  historyPrimaryField(): string {
    const fields: Partial<Record<MetricId, string>> = {
      heartRate: 'bpm',
      bloodPressure: 'systolic',
      oxygen: 'percent',
      temperature: this.temperatureHistoryField,
      bloodGlucose: 'mmolL',
      stress: 'score',
      steps: 'steps',
    };
    return fields[this.metric()] ?? this.firstNumericHistoryField();
  }

  historySecondaryField(): string | null {
    return this.metric() === 'bloodPressure' ? 'diastolic' : null;
  }

  usesTemperatureTabs(): boolean {
    return this.isHistory()
      && this.metric() === 'temperature'
      && this.numericHistoryValues('baselineCelsius').length > 0;
  }

  setTemperatureHistoryField(field: 'celsius' | 'baselineCelsius'): void {
    this.temperatureHistoryField = field;
  }

  usesStressBarChart(): boolean {
    return this.isHistory() && this.metric() === 'stress';
  }

  /**
   * Coloca cada barra de stress na hora real do dia e conserva a escala
   * clínica de 0 a 100 usada na leitura diária.
   */
  historyBarPoints(): DailyBarPoint[] {
    const values = this.historyRecords()
      .map((record) => ({
        date: new Date(record.timestamp),
        value: this.numericValue(record.values[this.historyPrimaryField()]),
      }))
      .filter((point): point is { date: Date; value: number } =>
        point.value !== null && !Number.isNaN(point.date.getTime()));
    const width = Math.max(0.8, Math.min(2.4, 72 / Math.max(values.length, 1)));
    return values.map(({ date, value }) => {
      const minutes = date.getHours() * 60 + date.getMinutes();
      const centre = Math.min(100 - width / 2, Math.max(width / 2, (minutes / 1_440) * 100));
      const height = Math.max(1, (Math.min(100, Math.max(0, value)) / 100) * 32);
      return {
        x: centre - width / 2,
        y: 42 - height,
        width,
        height,
        tone: value < 30 ? 'calm' : value < 60 ? 'normal' : value < 80 ? 'attention' : 'high',
      };
    });
  }

  intervalAverage(field = this.historyPrimaryField()): number | null {
    const records = this.recordsInLatestInterval();
    return this.average(records, field);
  }

  dailyAverage(field = this.historyPrimaryField()): number | null {
    return this.average(this.historyRecords(), field);
  }

  dailyMinimum(field = this.historyPrimaryField()): number | null {
    const values = this.numericHistoryValues(field);
    return values.length ? Math.min(...values) : null;
  }

  dailyMaximum(field = this.historyPrimaryField()): number | null {
    const values = this.numericHistoryValues(field);
    return values.length ? Math.max(...values) : null;
  }

  /**
   * Distribui cada ponto pela hora real do dia, em vez de o espaçar por índice.
   * Isto conserva períodos sem dados e replica a leitura temporal 0–24 h.
   */
  historyChartPoints(field: string): string {
    const points = this.historyRecords()
      .map((record) => ({
        record,
        value: this.numericValue(record.values[field]),
      }))
      .filter((point): point is { record: HBandHistoryRecord; value: number } =>
        point.value !== null);
    if (points.length < 2) {
      return '';
    }
    const scaleValues = [
      ...this.numericHistoryValues(field),
      ...(this.historySecondaryField()
        ? this.numericHistoryValues(this.historySecondaryField() as string)
        : []),
    ];
    const minimum = Math.min(...scaleValues);
    const maximum = Math.max(...scaleValues);
    const range = Math.max(maximum - minimum, 1);
    return points.map(({ record, value }) => {
      const date = new Date(record.timestamp);
      const minutes = date.getHours() * 60 + date.getMinutes();
      const x = Math.min(100, Math.max(0, (minutes / 1_440) * 100));
      const y = 42 - ((value - minimum) / range) * 32;
      return `${x.toFixed(2)},${y.toFixed(2)}`;
    }).join(' ');
  }

  intervalLabel(): string {
    const latest = this.latestHistoryRecord();
    if (!latest) {
      return '—';
    }
    const date = new Date(latest.timestamp);
    if (Number.isNaN(date.getTime())) {
      return '—';
    }
    const duration = this.intervalMinutes();
    const startMinutes = Math.floor((date.getHours() * 60 + date.getMinutes()) / duration) * duration;
    const start = new Date(date);
    start.setHours(Math.floor(startMinutes / 60), startMinutes % 60, 0, 0);
    const end = new Date(start.getTime() + (duration - 1) * 60_000);
    return `${this.shortTime(start)}–${this.shortTime(end)}`;
  }

  classification(): string | null {
    if (
      this.metric() === 'temperature'
      && this.isHistory()
      && this.temperatureHistoryField === 'baselineCelsius'
    ) {
      return null;
    }
    const primary = this.isHistory()
      ? this.intervalAverage()
      : this.numericValue(this.primaryEntry()?.[1]);
    if (primary === null) {
      return null;
    }
    switch (this.metric()) {
      case 'heartRate':
        return this.referenceClassification(primary, 60, 100);
      case 'bloodPressure': {
        const diastolic = this.isHistory()
          ? this.intervalAverage('diastolic')
          : this.numericValue(this.event.values['diastolic']);
        return diastolic !== null
          && primary >= 90 && primary <= 139
          && diastolic >= 60 && diastolic <= 89
          ? this.i18n.translate('data.classifications.normal')
          : this.i18n.translate('data.classifications.outsideReference');
      }
      case 'oxygen':
        return primary >= 95 && primary <= 100
          ? this.i18n.translate('data.classifications.normal')
          : this.i18n.translate('data.classifications.belowReference');
      case 'temperature':
        return primary <= 37.2
          ? this.i18n.translate('data.classifications.normal')
          : primary < 38
            ? this.i18n.translate('data.classifications.elevated')
            : this.i18n.translate('data.classifications.high');
      case 'stress':
        return primary < 30
          ? this.i18n.translate('data.classifications.relaxed')
          : primary < 60
            ? this.i18n.translate('data.classifications.normal')
            : primary < 80
              ? this.i18n.translate('data.classifications.moderate')
              : this.i18n.translate('data.classifications.high');
      case 'bloodGlucose':
        return this.glucoseRiskLabel();
      default:
        return null;
    }
  }

  classificationTone(): string {
    const primary = this.isHistory()
      ? this.intervalAverage()
      : this.numericValue(this.primaryEntry()?.[1]);
    if (primary === null) {
      return 'neutral';
    }
    if (this.metric() === 'stress') {
      return primary < 30 ? 'calm' : primary < 60 ? 'normal' : primary < 80 ? 'attention' : 'high';
    }
    if (this.metric() === 'temperature') {
      return primary <= 37.2 ? 'normal' : primary < 38 ? 'attention' : 'high';
    }
    return this.classification() === this.i18n.translate('data.classifications.normal')
      ? 'normal'
      : 'attention';
  }

  referenceItems(): ReferenceItem[] {
    if (
      this.metric() === 'temperature'
      && this.isHistory()
      && this.temperatureHistoryField === 'baselineCelsius'
    ) {
      return [];
    }
    const references: Partial<Record<MetricId, ReferenceItem[]>> = {
      heartRate: [
        { label: '< 60 bpm', tone: 'calm' },
        { label: '60–100 bpm', tone: 'normal' },
        { label: '> 100 bpm', tone: 'attention' },
      ],
      bloodPressure: [
        { label: '90–139 / 60–89 mmHg', tone: 'normal' },
      ],
      oxygen: [
        { label: '< 95%', tone: 'attention' },
        { label: '95–100%', tone: 'normal' },
      ],
      temperature: [
        { label: '≤ 37.2°C', tone: 'normal' },
        { label: '37.3–37.9°C', tone: 'attention' },
        { label: '≥ 38.0°C', tone: 'high' },
      ],
      stress: [
        { label: '1–29', tone: 'calm' },
        { label: '30–59', tone: 'normal' },
        { label: '60–79', tone: 'attention' },
        { label: '80–99', tone: 'high' },
      ],
    };
    return references[this.metric()] ?? [];
  }

  /**
   * Separa os valores clínicos dos campos de controlo do protocolo.
   */
  resultEntries(): Array<[string, string | number | boolean | null]> {
    return this.entries().filter(([field]) =>
      !['progress', 'state', 'samples', 'sampleCount', 'diagnosis'].includes(field));
  }

  primaryEntry(): [string, string | number | boolean | null] | undefined {
    return this.resultEntries()[0] ?? this.entries()[0];
  }

  chartPoints(): string {
    const samples = this.chartSamples();
    if (samples.length < 2) {
      return '';
    }
    const max = Math.max(...samples);
    const min = Math.min(...samples);
    // Um sinal constante continua a ser informação válida e deve ficar visível no centro do gráfico.
    if (max === min) {
      return samples.map((_, index) => {
        const x = (index / (samples.length - 1)) * 100;
        return `${x.toFixed(2)},24.00`;
      }).join(' ');
    }
    const range = Math.max(max - min, 1);
    return samples.map((value, index) => {
      const x = (index / (samples.length - 1)) * 100;
      const y = 44 - ((value - min) / range) * 36;
      return `${x.toFixed(2)},${y.toFixed(2)}`;
    }).join(' ');
  }

  barHeight(value: string | number | boolean | null): number {
    const numeric = typeof value === 'number' ? value : 0;
    const values = this.entries().map((entry) => typeof entry[1] === 'number' ? entry[1] : 0);
    const max = Math.max(...values, 1);
    return Math.max(8, (numeric / max) * 100);
  }

  isBloodPressureVisual(): boolean {
    return this.event.metric === 'bloodPressure' || this.event.type === 'bloodPressure';
  }

  isBodyCompositionVisual(): boolean {
    return this.event.metric === 'bodyComposition' || this.event.type === 'bodyComposition';
  }

  showsSeparateProgress(): boolean {
    return !this.compact
      && !this.isHistory()
      && this.progressValue() !== null
      && !['bloodPressure', 'bodyComposition', 'bloodGlucose'].includes(this.metric());
  }

  /**
   * A leitura atual já contém totais. No histórico Android os dados chegam em
   * blocos de atividade, pelo que são somados sem alterar os registos brutos.
   */
  activityValue(field: 'steps' | 'distanceKm' | 'caloriesKcal'): number {
    if (this.event.type !== 'history') {
      const value = this.event.values[field];
      return typeof value === 'number' ? value : 0;
    }
    return this.historyRecords().reduce((total, record) => {
      const value = record.values[field];
      return total + (typeof value === 'number' ? value : 0);
    }, 0);
  }

  activityPoints(): ActivityPoint[] {
    return this.historyRecords()
      .map((record) => ({
        timestamp: record.timestamp,
        steps: typeof record.values['steps'] === 'number' ? record.values['steps'] : 0,
      }))
      .filter((point) => point.steps > 0);
  }

  activityBarHeight(steps: number): number {
    const maximum = Math.max(1, ...this.activityPoints().map((point) => point.steps));
    return Math.max(8, (steps / maximum) * 100);
  }

  activityPointLeft(timestamp: string): number {
    const date = new Date(timestamp);
    const minutes = date.getHours() * 60 + date.getMinutes();
    return Math.min(98, Math.max(0, (minutes / 1_440) * 100));
  }

  latestActivityPoint(): ActivityPoint | undefined {
    return [...this.activityPoints()].sort(
      (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime(),
    ).at(-1);
  }

  progressValue(): number | null {
    const value = this.event.values['progress'];
    return typeof value === 'number' && Number.isFinite(value)
      ? Math.min(100, Math.max(0, value))
      : null;
  }

  gaugeValue(): number {
    const preferred = this.entries().find(([field]) => field === 'percent' || field === 'score');
    const value = preferred?.[1] ?? this.primaryEntry()?.[1];
    return typeof value === 'number' ? Math.min(Math.max(value, 0), 100) : 0;
  }

  gaugeDash(): string {
    const circumference = 251.2;
    return `${(this.gaugeValue() / 100) * circumference} ${circumference}`;
  }

  formatValue(value: string | number | boolean | null, field?: string): string {
    if (typeof value === 'boolean') {
      return this.i18n.translate(value ? 'common.yes' : 'common.no');
    }
    if (
      typeof value === 'number'
      && ['celsius', 'surfaceCelsius', 'baselineCelsius'].includes(field ?? '')
    ) {
      return new Intl.NumberFormat(this.locale(), {
        minimumFractionDigits: 1,
        maximumFractionDigits: 1,
      }).format(value);
    }
    if (typeof value === 'number' && field === 'mmolL') {
      return new Intl.NumberFormat(this.locale(), {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }).format(value);
    }
    if (typeof value === 'number' && !Number.isInteger(value)) {
      return new Intl.NumberFormat(this.locale(), {
        minimumFractionDigits: 1,
        maximumFractionDigits: 1,
      }).format(value);
    }
    return value === null ? '—' : String(value);
  }

  historyRecords() {
    return this.event.records ?? [];
  }

  /**
   * Usa amostras explícitas (por exemplo ECG) e, quando estas não existem,
   * constrói a série com o primeiro valor numérico de cada registo diário.
   */
  private chartSamples(): number[] {
    if (this.event.samples?.length) {
      return this.event.samples;
    }
    return this.historyRecords().flatMap((record) => {
      const value = Object.values(record.values).find((item) => typeof item === 'number');
      return typeof value === 'number' ? [value] : [];
    });
  }

  private metric(): MetricId {
    return (this.event.metric ?? this.event.type) as MetricId;
  }

  private latestHistoryRecord(): HBandHistoryRecord | undefined {
    return [...this.historyRecords()].sort(
      (a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime(),
    ).at(-1);
  }

  private recordsInLatestInterval(): HBandHistoryRecord[] {
    const latest = this.latestHistoryRecord();
    if (!latest) {
      return [];
    }
    const duration = this.intervalMinutes();
    const latestDate = new Date(latest.timestamp);
    const bucket = Math.floor(
      (latestDate.getHours() * 60 + latestDate.getMinutes()) / duration,
    );
    return this.historyRecords().filter((record) => {
      const date = new Date(record.timestamp);
      return Math.floor((date.getHours() * 60 + date.getMinutes()) / duration) === bucket;
    });
  }

  private intervalMinutes(): number {
    switch (this.metric()) {
      case 'oxygen':
        return 10;
      case 'bloodPressure':
      case 'temperature':
        return 60;
      default:
        return 30;
    }
  }

  private average(records: HBandHistoryRecord[], field: string): number | null {
    const values = records
      .map((record) => this.numericValue(record.values[field]))
      .filter((value): value is number => value !== null);
    return values.length
      ? values.reduce((total, value) => total + value, 0) / values.length
      : null;
  }

  private numericHistoryValues(field: string): number[] {
    return this.historyRecords()
      .map((record) => this.numericValue(record.values[field]))
      .filter((value): value is number => value !== null);
  }

  private numericValue(value: unknown): number | null {
    return typeof value === 'number' && Number.isFinite(value) ? value : null;
  }

  private firstNumericHistoryField(): string {
    const record = this.historyRecords().find((item) =>
      Object.values(item.values).some((value) => typeof value === 'number'));
    return Object.entries(record?.values ?? {})
      .find(([, value]) => typeof value === 'number')?.[0] ?? '';
  }

  private shortTime(date: Date): string {
    return new Intl.DateTimeFormat(this.locale(), {
      hour: '2-digit',
      minute: '2-digit',
      hour12: false,
    }).format(date);
  }

  private referenceClassification(value: number, minimum: number, maximum: number): string {
    if (value < minimum) {
      return this.i18n.translate('data.classifications.belowReference');
    }
    if (value > maximum) {
      return this.i18n.translate('data.classifications.aboveReference');
    }
    return this.i18n.translate('data.classifications.normal');
  }

  private glucoseRiskLabel(): string | null {
    const source = this.latestHistoryRecord()?.values ?? this.event.values;
    const risk = String(source['risk'] ?? source['riskLevel'] ?? '').toUpperCase();
    if (!risk || risk === 'NONE') {
      return null;
    }
    const key = risk.includes('LOW')
      ? 'lowRisk'
      : risk.includes('MIDDLE') || risk.includes('MEDIUM')
        ? 'mediumRisk'
        : risk.includes('HIGH') ? 'highRisk' : null;
    return key ? this.i18n.translate(`data.classifications.${key}`) : null;
  }

  /**
   * Apresenta nomes localizados para os campos conhecidos e conserva o nome
   * técnico quando o SDK devolve um campo novo ainda não catalogado.
   */
  fieldLabel(field: string): string {
    const key = `data.fields.${field}`;
    const translated = this.i18n.translate(key);
    return translated === key ? field : translated;
  }

  private locale(): string {
    return this.i18n.language() === 'br' ? 'pt-BR' : this.i18n.language();
  }
}
