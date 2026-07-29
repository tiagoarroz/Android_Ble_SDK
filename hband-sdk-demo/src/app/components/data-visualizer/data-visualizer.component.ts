import { CommonModule } from '@angular/common';
import { Component, Input, inject } from '@angular/core';

import type { HBandDataEvent, VisualizationType } from '../../core/hband.types';
import { I18nService } from '../../core/i18n.service';

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
    return Object.entries(this.event.values);
  }

  primaryEntry(): [string, string | number | boolean | null] | undefined {
    return this.entries()[0];
  }

  chartPoints(): string {
    const samples = this.event.samples ?? [];
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

  gaugeValue(): number {
    const value = this.primaryEntry()?.[1];
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
