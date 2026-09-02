import { Injectable, inject, signal } from '@angular/core';
import { Capacitor } from '@capacitor/core';

import { BandNfc } from './band-nfc.plugin';
import { I18nService } from './i18n.service';

const BAND_TAG_PREFIX = 'HITECOSYSTEM-HBAND';
const BAND_TAG_VERSION = '1';
const MAC_ADDRESS_PATTERN = /^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$/;

export type BandNfcPhase =
  | 'idle'
  | 'discovering'
  | 'waitingRead'
  | 'waitingWrite'
  | 'connecting'
  | 'success'
  | 'error';

@Injectable({ providedIn: 'root' })
export class BandNfcService {
  private readonly i18n = inject(I18nService);
  readonly phase = signal<BandNfcPhase>('idle');
  readonly errorCode = signal<string | null>(null);
  readonly isNative = Capacitor.isNativePlatform();

  readonly busy = () => [
    'discovering', 'waitingRead', 'waitingWrite', 'connecting',
  ].includes(this.phase());

  /**
   * O texto NDEF é deliberadamente curto e versionado. O prefixo impede que
   * uma tag de outra aplicação seja interpretada como uma pulseira H Band.
   */
  encodeBand(macAddress: string): string {
    if (!MAC_ADDRESS_PATTERN.test(macAddress)) {
      throw new Error('INVALID_BAND_MAC');
    }
    return `${BAND_TAG_PREFIX}|${BAND_TAG_VERSION}|${macAddress}`;
  }

  /** Aceita apenas a versão conhecida e devolve o MAC sem alterar o seu valor. */
  decodeBand(text: string): string {
    const parts = text.split('|');
    if (
      parts.length !== 3
      || parts[0] !== BAND_TAG_PREFIX
      || parts[1] !== BAND_TAG_VERSION
      || !MAC_ADDRESS_PATTERN.test(parts[2])
    ) {
      throw new Error('INVALID_BAND_TAG');
    }
    return parts[2];
  }

  /**
   * O texto do alerta nativo é parametrizável porque o registo de uma pulseira
   * já ligada e o de uma pulseira apenas sinalizada pedem gestos diferentes à
   * pessoa que aproxima a tag.
   */
  async registerBand(
    macAddress: string,
    alertMessageKey = 'nfc.native.writePrompt',
  ): Promise<void> {
    this.begin('waitingWrite');
    try {
      await this.ensureAvailable();
      await BandNfc.write({
        text: this.encodeBand(macAddress),
        alertMessage: this.i18n.translate(alertMessageKey),
        successMessage: this.i18n.translate('nfc.native.writeSuccess'),
      });
      this.phase.set('success');
    } catch (error) {
      this.fail(error);
      throw error;
    }
  }

  async readBand(): Promise<string> {
    this.begin('waitingRead');
    try {
      await this.ensureAvailable();
      const result = await BandNfc.read({
        alertMessage: this.i18n.translate('nfc.native.readPrompt'),
      });
      return this.decodeBand(result.text);
    } catch (error) {
      this.fail(error);
      throw error;
    }
  }

  markConnecting(): void {
    this.phase.set('connecting');
    this.errorCode.set(null);
  }

  /** Assinala a pesquisa Bluetooth que precede o registo de uma pulseira. */
  markDiscovering(): void {
    this.phase.set('discovering');
    this.errorCode.set(null);
  }

  markSuccess(): void {
    this.phase.set('success');
    this.errorCode.set(null);
  }

  markError(error: unknown): void {
    this.fail(error);
  }

  reset(): void {
    this.phase.set('idle');
    this.errorCode.set(null);
  }

  async cancel(): Promise<void> {
    if (this.isNative && this.busy()) {
      try {
        await BandNfc.cancel();
      } catch {
        // A sessão pode já ter sido terminada pelo sistema ou pela aproximação da tag.
      }
    }
    this.reset();
  }

  private begin(phase: Extract<BandNfcPhase, 'waitingRead' | 'waitingWrite'>): void {
    this.phase.set(phase);
    this.errorCode.set(null);
  }

  private async ensureAvailable(): Promise<void> {
    if (!this.isNative) {
      throw new Error('NFC_NOT_SUPPORTED');
    }
    const status = await BandNfc.getStatus();
    if (!status.supported) {
      throw new Error('NFC_NOT_SUPPORTED');
    }
    if (!status.enabled) {
      throw new Error('NFC_NOT_ENABLED');
    }
  }

  private fail(error: unknown): void {
    const raw = error instanceof Error ? error.message : String(error);
    const knownCode = [
      'NFC_NOT_SUPPORTED', 'NFC_NOT_ENABLED', 'NFC_TAG_NOT_NDEF',
      'NFC_TAG_READ_ONLY', 'NFC_TAG_TOO_SMALL', 'NFC_NO_TEXT_RECORD',
      'NFC_CANCELLED', 'NFC_SESSION_INTERRUPTED', 'NFC_IO_ERROR',
      'INVALID_BAND_TAG', 'INVALID_BAND_MAC',
      'BAND_NOT_FOUND', 'BLE_PERMISSION_DENIED',
      'NO_BAND_NEARBY', 'PROVISIONING_SESSION_BUSY', 'BAND_SIGNAL_FAILED',
    ].find((code) => raw.includes(code));
    this.errorCode.set(knownCode ?? 'NFC_UNKNOWN_ERROR');
    this.phase.set('error');
  }
}
