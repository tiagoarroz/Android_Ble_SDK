import type { FeatureDefinition } from './hband.types';

const liveAndHistory = (metric: FeatureDefinition['metric'], prefix: string) => [
  {
    id: 'measurement',
    operation: `${prefix}.start`,
    stopOperation: `${prefix}.stop`,
    labelKey: 'actions.start',
    activeLabelKey: 'actions.stop',
  },
  { id: 'history', operation: 'history.metric', labelKey: 'actions.history', tone: 'secondary' as const, metric },
];

/**
 * Mantém o demonstrador limitado às métricas clínicas, fisiológicas e de
 * atividade pedidas.
 */
export const FEATURE_CATALOG: FeatureDefinition[] = [
  {
    id: 'heart-rate', metric: 'heartRate', titleKey: 'features.heartRate.title',
    descriptionKey: 'features.heartRate.description', icon: 'heart-outline',
    capability: 'heartRate', visualization: 'line',
    actions: liveAndHistory('heartRate', 'measure.heartRate'),
  },
  {
    id: 'blood-pressure', metric: 'bloodPressure', titleKey: 'features.bloodPressure.title',
    descriptionKey: 'features.bloodPressure.description', icon: 'speedometer-outline',
    capability: 'bloodPressure', visualization: 'bars',
    actions: liveAndHistory('bloodPressure', 'measure.bloodPressure'),
  },
  {
    id: 'oxygen', metric: 'oxygen', titleKey: 'features.oxygen.title',
    descriptionKey: 'features.oxygen.description', icon: 'water-outline',
    capability: 'bloodOxygen', visualization: 'gauge',
    actions: liveAndHistory('oxygen', 'measure.oxygen'),
  },
  {
    id: 'temperature', metric: 'temperature', titleKey: 'features.temperature.title',
    descriptionKey: 'features.temperature.description', icon: 'thermometer-outline',
    capability: 'temperature', visualization: 'line',
    actions: liveAndHistory('temperature', 'measure.temperature'),
  },
  {
    id: 'blood-glucose', metric: 'bloodGlucose', titleKey: 'features.bloodGlucose.title',
    descriptionKey: 'features.bloodGlucose.description', icon: 'medical-outline',
    capability: 'bloodGlucose', visualization: 'line',
    actions: liveAndHistory('bloodGlucose', 'measure.bloodGlucose'),
  },
  {
    id: 'ecg', metric: 'ecg', titleKey: 'features.ecg.title',
    descriptionKey: 'features.ecg.description', icon: 'fitness-outline',
    capability: 'ecg', visualization: 'ecg',
    actions: liveAndHistory('ecg', 'measure.ecg'),
  },
  {
    id: 'body-composition', metric: 'bodyComposition', titleKey: 'features.bodyComposition.title',
    descriptionKey: 'features.bodyComposition.description', icon: 'body-outline',
    capability: 'bodyComposition', visualization: 'composition',
    actions: liveAndHistory('bodyComposition', 'measure.bodyComposition'),
  },
  {
    id: 'steps', metric: 'steps', titleKey: 'features.steps.title',
    descriptionKey: 'features.steps.description', icon: 'footsteps-outline',
    capability: 'steps', visualization: 'activity',
    actions: [
      {
        id: 'current',
        operation: 'history.activity.current',
        labelKey: 'actions.current',
        metric: 'steps',
      },
      {
        id: 'history',
        operation: 'history.metric',
        labelKey: 'actions.history',
        tone: 'secondary',
        metric: 'steps',
      },
    ],
  },
  {
    id: 'stress', metric: 'stress', titleKey: 'features.stress.title',
    descriptionKey: 'features.stress.description', icon: 'leaf-outline',
    capability: 'stress', visualization: 'gauge',
    actions: liveAndHistory('stress', 'measure.stress'),
  },
];
