import { CommonModule } from '@angular/common';
import { Component, Input, inject } from '@angular/core';

import type { HBandDataEvent, VisualizationType } from '../../core/hband.types';
import { I18nService } from '../../core/i18n.service';

interface ActivityPoint {
  timestamp: string;
  steps: number;
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

  entries(): Array<[string, string | number | boolean | null]> {
    const latest = this.event.records?.at(-1);
    return Object.entries(latest?.values ?? this.event.values).filter(([field]) => field !== 'records');
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
