import { registerPlugin } from '@capacitor/core';

import type { HBandPlugin } from './hband.types';

/**
 * Regista a bridge nativa com o mesmo contrato em Android e iOS.
 * A implementação web é deliberadamente tratada pelo serviço de demonstração.
 */
export const HBand = registerPlugin<HBandPlugin>('HBand');
