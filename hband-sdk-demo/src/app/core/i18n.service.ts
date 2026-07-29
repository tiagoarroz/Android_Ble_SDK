import { DOCUMENT } from '@angular/common';
import { Injectable, inject, signal } from '@angular/core';

import br from '../../assets/i18n/br.json';
import en from '../../assets/i18n/en.json';
import es from '../../assets/i18n/es.json';
import fr from '../../assets/i18n/fr.json';
import pt from '../../assets/i18n/pt.json';

type Language = 'pt' | 'br' | 'en' | 'es' | 'fr';
type TranslationValue = string | TranslationTree;
interface TranslationTree {
  [key: string]: TranslationValue;
}

const translations: Record<Language, TranslationTree> = { pt, br, en, es, fr };

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly document = inject(DOCUMENT);
  readonly language = signal<Language>(this.resolveInitialLanguage());

  constructor() {
    this.document.documentElement.lang = this.language();
  }

  /**
   * Traduz uma chave pontuada e substitui parâmetros no formato {{nome}}.
   */
  translate(key: string, params: Record<string, string | number> = {}): string {
    const value = key.split('.').reduce<TranslationValue | undefined>((current, segment) => {
      if (typeof current !== 'object' || current === null) {
        return undefined;
      }
      return current[segment];
    }, translations[this.language()]);

    const text = typeof value === 'string' ? value : key;
    return Object.entries(params).reduce(
      (result, [name, replacement]) => result.replaceAll(`{{${name}}}`, String(replacement)),
      text,
    );
  }

  /**
   * Altera o idioma sem reiniciar a sessão BLE ou perder os dados visíveis.
   */
  setLanguage(language: string): void {
    if (!this.isLanguage(language)) {
      return;
    }
    this.language.set(language);
    this.document.documentElement.lang = language;
    localStorage.setItem('hband-language', language);
  }

  private resolveInitialLanguage(): Language {
    const saved = localStorage.getItem('hband-language');
    if (saved && this.isLanguage(saved)) {
      return saved;
    }
    const browserLanguage = navigator.language.toLowerCase();
    if (browserLanguage === 'pt-br') {
      return 'br';
    }
    const shortLanguage = browserLanguage.split('-')[0];
    return this.isLanguage(shortLanguage) ? shortLanguage : 'pt';
  }

  private isLanguage(value: string): value is Language {
    return ['pt', 'br', 'en', 'es', 'fr'].includes(value);
  }
}
