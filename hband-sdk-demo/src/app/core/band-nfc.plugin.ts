import { registerPlugin } from '@capacitor/core';

export interface BandNfcStatus {
  supported: boolean;
  enabled: boolean;
}

export interface BandNfcReadResult {
  text: string;
}

export interface BandNfcPlugin {
  getStatus(): Promise<BandNfcStatus>;
  read(options: { alertMessage: string }): Promise<BandNfcReadResult>;
  write(options: {
    text: string;
    alertMessage: string;
    successMessage: string;
  }): Promise<void>;
  cancel(): Promise<void>;
}

/** Regista a bridge NDEF comum às implementações nativas Android e iOS. */
export const BandNfc = registerPlugin<BandNfcPlugin>('BandNfc');
