import { CommonModule } from '@angular/common';
import { Component, Input, inject } from '@angular/core';

import type { HBandDataEvent, VisualizationType } from '../../core/hband.types';
import { I18nService } from '../../core/i18n.service';

type MetIntensity = 'rest' | 'light' | 'moderate' | 'vigorous';

interface MetPoint {
  timestamp: string;
  value: number;
  intensity: MetIntensity;
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

  primaryEntry(): [string, string | number | boolean | null] | undefined {
    return this.entries()[0];
  }

  chartPoints(): string {
    const samples = this.chartSamples();
    if (samples.length < 2) {
      return '';
    }
    const max = Math.max(...samples);
    const min = Math.min(...samples);
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

  isMetVisual(): boolean {
    return this.event.metric === 'met' || this.event.type === 'met';
  }

  /**
   * Normaliza os registos MET num único formato para desenhar a evolução
   * temporal sem alterar os valores recebidos do SDK.
   */
  metPoints(): MetPoint[] {
    const records = this.historyRecords()
      .map((record) => ({ timestamp: record.timestamp, value: record.values['met'] }))
      .filter((record): record is { timestamp: string; value: number } =>
        typeof record.value === 'number' && record.value > 0);

    if (records.length) {
      return records.map((record) => ({
        ...record,
        intensity: this.metIntensity(record.value),
      }));
    }

    const value = this.event.values['met'];
    return typeof value === 'number' && value > 0
      ? [{ timestamp: this.event.timestamp, value, intensity: this.metIntensity(value) }]
      : [];
  }

  /**
   * Mantém uma escala comparável entre dias: nunca usa menos de 6 MET como
   * máximo visual, porque esse é o início da atividade vigorosa.
   */
  metBarHeight(value: number): number {
    const scaleMaximum = Math.max(6, ...this.metPoints().map((point) => point.value));
    return Math.max(6, Math.min(100, (value / scaleMaximum) * 100));
  }

  /**
   * Classifica a intensidade segundo os intervalos MET apresentados na
   * legenda. Valores inferiores a 1,6 ficam no patamar de repouso.
   */
  metIntensity(value: number): MetIntensity {
    if (value < 1.6) {
      return 'rest';
    }
    if (value < 3) {
      return 'light';
    }
    if (value < 6) {
      return 'moderate';
    }
    return 'vigorous';
  }

  formatMet(value: number): string {
    const locale = this.i18n.language() === 'br' ? 'pt-BR' : this.i18n.language();
    const formatted = new Intl.NumberFormat(locale, {
      minimumFractionDigits: 1,
      maximumFractionDigits: 1,
    }).format(value);
    return this.i18n.translate('data.met.value', { value: formatted });
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

  formatValue(value: string | number | boolean | null): string {
    if (typeof value === 'boolean') {
      return this.i18n.translate(value ? 'common.yes' : 'common.no');
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
}
