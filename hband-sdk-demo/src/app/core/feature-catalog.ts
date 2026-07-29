import type { FeatureDefinition } from './hband.types';

const startStop = (prefix: string) => [
  { id: 'start', operation: `${prefix}.start`, labelKey: 'actions.start' },
  { id: 'stop', operation: `${prefix}.stop`, labelKey: 'actions.stop', tone: 'secondary' as const },
];

const readConfigure = (prefix: string) => [
  { id: 'read', operation: `${prefix}.read`, labelKey: 'actions.read' },
  { id: 'configure', operation: `${prefix}.configure`, labelKey: 'actions.configure', tone: 'secondary' as const },
];

/**
 * Catálogo funcional comum aos dois SDKs. As capacidades reais são sobrepostas
 * após a autenticação, para que modelos como a MF91 não recebam comandos que
 * o respetivo firmware não declarou suportar.
 */
export const FEATURE_CATALOG: FeatureDefinition[] = [
  {
    id: 'device-session', category: 'connection', titleKey: 'features.deviceSession.title',
    descriptionKey: 'features.deviceSession.description', icon: 'bluetooth-outline',
    visualization: 'log', actions: [
      { id: 'scan', operation: 'session.scan', labelKey: 'actions.scan' },
      { id: 'disconnect', operation: 'session.disconnect', labelKey: 'actions.disconnect', tone: 'secondary' },
    ],
  },
  {
    id: 'authentication', category: 'connection', titleKey: 'features.authentication.title',
    descriptionKey: 'features.authentication.description', icon: 'key-outline',
    visualization: 'log', actions: [
      { id: 'authenticate', operation: 'session.authenticate', labelKey: 'actions.authenticate' },
      { id: 'profile', operation: 'device.profile', labelKey: 'actions.sync' },
    ],
  },
  {
    id: 'device-status', category: 'connection', titleKey: 'features.deviceStatus.title',
    descriptionKey: 'features.deviceStatus.description', icon: 'battery-half-outline',
    visualization: 'metric', actions: [
      { id: 'battery', operation: 'device.battery', labelKey: 'actions.readBattery' },
      { id: 'rssi', operation: 'device.rssi', labelKey: 'actions.readSignal', tone: 'secondary' },
      { id: 'time', operation: 'device.time', labelKey: 'actions.syncTime', tone: 'secondary' },
    ],
  },
  {
    id: 'heart-rate', category: 'measurements', titleKey: 'features.heartRate.title',
    descriptionKey: 'features.heartRate.description', icon: 'heart-outline', capability: 'heartRate',
    visualization: 'line', actions: startStop('measure.heartRate'),
  },
  {
    id: 'blood-pressure', category: 'measurements', titleKey: 'features.bloodPressure.title',
    descriptionKey: 'features.bloodPressure.description', icon: 'speedometer-outline', capability: 'bloodPressure',
    visualization: 'bars', actions: [...startStop('measure.bloodPressure'), ...readConfigure('settings.bloodPressure')],
  },
  {
    id: 'oxygen', category: 'measurements', titleKey: 'features.oxygen.title',
    descriptionKey: 'features.oxygen.description', icon: 'water-outline', capability: 'bloodOxygen',
    visualization: 'gauge', actions: startStop('measure.oxygen'),
  },
  {
    id: 'breathing', category: 'measurements', titleKey: 'features.breathing.title',
    descriptionKey: 'features.breathing.description', icon: 'pulse-outline', capability: 'breathing',
    visualization: 'line', actions: startStop('measure.breathing'),
  },
  {
    id: 'temperature', category: 'measurements', titleKey: 'features.temperature.title',
    descriptionKey: 'features.temperature.description', icon: 'thermometer-outline', capability: 'temperature',
    visualization: 'line', actions: startStop('measure.temperature'),
  },
  {
    id: 'hrv', category: 'measurements', titleKey: 'features.hrv.title',
    descriptionKey: 'features.hrv.description', icon: 'analytics-outline', capability: 'hrv',
    visualization: 'line', actions: startStop('measure.hrv'),
  },
  {
    id: 'ecg', category: 'measurements', titleKey: 'features.ecg.title',
    descriptionKey: 'features.ecg.description', icon: 'fitness-outline', capability: 'ecg',
    visualization: 'line', actions: [
      ...startStop('measure.ecg'),
      { id: 'history', operation: 'history.ecg', labelKey: 'actions.history', tone: 'secondary' },
    ],
  },
  {
    id: 'blood-glucose', category: 'measurements', titleKey: 'features.bloodGlucose.title',
    descriptionKey: 'features.bloodGlucose.description', icon: 'medical-outline', capability: 'bloodGlucose',
    visualization: 'line', actions: [...startStop('measure.bloodGlucose'), ...readConfigure('settings.bloodGlucose')],
  },
  {
    id: 'fatigue-stress', category: 'measurements', titleKey: 'features.fatigueStress.title',
    descriptionKey: 'features.fatigueStress.description', icon: 'leaf-outline', capability: 'fatigue',
    visualization: 'gauge', actions: [
      ...startStop('measure.fatigue'),
      { id: 'stress', operation: 'measure.stress.start', labelKey: 'actions.measureStress', tone: 'secondary' },
    ],
  },
  {
    id: 'body-composition', category: 'measurements', titleKey: 'features.bodyComposition.title',
    descriptionKey: 'features.bodyComposition.description', icon: 'body-outline', capability: 'bodyComposition',
    visualization: 'bars', actions: [...startStop('measure.bodyComposition'), { id: 'read', operation: 'history.bodyComposition', labelKey: 'actions.history' }],
  },
  {
    id: 'blood-composition', category: 'measurements', titleKey: 'features.bloodComposition.title',
    descriptionKey: 'features.bloodComposition.description', icon: 'flask-outline', capability: 'bloodComposition',
    visualization: 'bars', actions: [...startStop('measure.bloodComposition'), ...readConfigure('settings.bloodComposition')],
  },
  {
    id: 'gsr-mini', category: 'measurements', titleKey: 'features.gsrMini.title',
    descriptionKey: 'features.gsrMini.description', icon: 'flash-outline', capability: 'gsr',
    visualization: 'line', actions: [
      ...startStop('measure.gsr'),
      { id: 'mini', operation: 'measure.miniCheckup.start', labelKey: 'actions.miniCheckup', tone: 'secondary' },
    ],
  },
  {
    id: 'daily-activity', category: 'history', titleKey: 'features.dailyActivity.title',
    descriptionKey: 'features.dailyActivity.description', icon: 'walk-outline',
    visualization: 'bars', actions: [
      { id: 'current', operation: 'history.activity.current', labelKey: 'actions.current' },
      { id: 'history', operation: 'history.activity', labelKey: 'actions.history', tone: 'secondary' },
    ],
  },
  {
    id: 'sleep', category: 'history', titleKey: 'features.sleep.title',
    descriptionKey: 'features.sleep.description', icon: 'moon-outline', capability: 'sleep',
    visualization: 'sleep', actions: [{ id: 'read', operation: 'history.sleep', labelKey: 'actions.read' }],
  },
  {
    id: 'health-history', category: 'history', titleKey: 'features.healthHistory.title',
    descriptionKey: 'features.healthHistory.description', icon: 'calendar-outline',
    visualization: 'timeline', actions: [
      { id: 'all', operation: 'history.allHealth', labelKey: 'actions.readAll' },
      { id: 'raw', operation: 'history.origin', labelKey: 'actions.rawData', tone: 'secondary' },
    ],
  },
  {
    id: 'sport-history', category: 'history', titleKey: 'features.sportHistory.title',
    descriptionKey: 'features.sportHistory.description', icon: 'bicycle-outline', capability: 'sport',
    visualization: 'timeline', actions: [
      { id: 'read', operation: 'history.sport', labelKey: 'actions.history' },
      { id: 'start', operation: 'sport.start', labelKey: 'actions.start' },
      { id: 'stop', operation: 'sport.stop', labelKey: 'actions.stop', tone: 'secondary' },
    ],
  },
  {
    id: 'clinical-history', category: 'history', titleKey: 'features.clinicalHistory.title',
    descriptionKey: 'features.clinicalHistory.description', icon: 'file-tray-full-outline',
    visualization: 'table', actions: [
      { id: 'oxygen', operation: 'history.oxygen', labelKey: 'features.oxygen.title' },
      { id: 'hrv', operation: 'history.hrv', labelKey: 'features.hrv.title' },
      { id: 'temperature', operation: 'history.temperature', labelKey: 'features.temperature.title' },
      { id: 'manual', operation: 'history.manual', labelKey: 'actions.manual' },
      { id: 'rr', operation: 'history.rr', labelKey: 'actions.rrIntervals' },
    ],
  },
  {
    id: 'automatic-monitoring', category: 'automation', titleKey: 'features.automaticMonitoring.title',
    descriptionKey: 'features.automaticMonitoring.description', icon: 'timer-outline', capability: 'autoMeasure',
    visualization: 'timeline', actions: readConfigure('settings.autoMeasure'),
  },
  {
    id: 'alarms', category: 'automation', titleKey: 'features.alarms.title',
    descriptionKey: 'features.alarms.description', icon: 'alarm-outline', capability: 'alarms',
    visualization: 'timeline', actions: readConfigure('settings.alarms'),
  },
  {
    id: 'text-alarms', category: 'automation', titleKey: 'features.textAlarms.title',
    descriptionKey: 'features.textAlarms.description', icon: 'chatbox-ellipses-outline', capability: 'textAlarms',
    visualization: 'timeline', actions: readConfigure('settings.textAlarms'),
  },
  {
    id: 'health-reminders', category: 'automation', titleKey: 'features.healthReminders.title',
    descriptionKey: 'features.healthReminders.description', icon: 'notifications-outline',
    visualization: 'table', actions: readConfigure('settings.healthReminders'),
  },
  {
    id: 'wear-behaviour', category: 'automation', titleKey: 'features.wearBehaviour.title',
    descriptionKey: 'features.wearBehaviour.description', icon: 'hand-left-outline',
    visualization: 'table', actions: [
      ...readConfigure('settings.sedentary'),
      { id: 'raise', operation: 'settings.raiseWrist.read', labelKey: 'actions.raiseWrist' },
      { id: 'wear', operation: 'settings.wearDetection.read', labelKey: 'actions.wearDetection' },
    ],
  },
  {
    id: 'power-functions', category: 'automation', titleKey: 'features.powerFunctions.title',
    descriptionKey: 'features.powerFunctions.description', icon: 'power-outline', capability: 'lowPower',
    visualization: 'table', actions: [
      ...readConfigure('settings.lowPower'),
      { id: 'functions', operation: 'settings.functionSwitch.read', labelKey: 'actions.functionSwitch' },
    ],
  },
  {
    id: 'female-health', category: 'automation', titleKey: 'features.femaleHealth.title',
    descriptionKey: 'features.femaleHealth.description', icon: 'flower-outline', capability: 'femaleHealth',
    visualization: 'timeline', actions: readConfigure('settings.femaleHealth'),
  },
  {
    id: 'display', category: 'interaction', titleKey: 'features.display.title',
    descriptionKey: 'features.display.description', icon: 'watch-outline', capability: 'display',
    visualization: 'table', actions: readConfigure('settings.display'),
  },
  {
    id: 'language-units', category: 'interaction', titleKey: 'features.languageUnits.title',
    descriptionKey: 'features.languageUnits.description', icon: 'language-outline',
    visualization: 'table', actions: [
      { id: 'language', operation: 'settings.language', labelKey: 'actions.language' },
      { id: 'units', operation: 'settings.units', labelKey: 'actions.units', tone: 'secondary' },
    ],
  },
  {
    id: 'find', category: 'interaction', titleKey: 'features.find.title',
    descriptionKey: 'features.find.description', icon: 'locate-outline', capability: 'findDevice',
    visualization: 'metric', actions: [
      { id: 'device', operation: 'interaction.findDevice', labelKey: 'actions.findDevice' },
      { id: 'phone', operation: 'interaction.findPhone', labelKey: 'actions.findPhone', tone: 'secondary' },
    ],
  },
  {
    id: 'camera', category: 'interaction', titleKey: 'features.camera.title',
    descriptionKey: 'features.camera.description', icon: 'camera-outline', capability: 'camera',
    visualization: 'log', actions: startStop('interaction.camera'),
  },
  {
    id: 'notifications-music', category: 'interaction', titleKey: 'features.notificationsMusic.title',
    descriptionKey: 'features.notificationsMusic.description', icon: 'musical-notes-outline',
    visualization: 'log', actions: [
      ...readConfigure('settings.notifications'),
      { id: 'music', operation: 'interaction.music', labelKey: 'actions.music' },
      { id: 'volume', operation: 'interaction.volume', labelKey: 'actions.volume', tone: 'secondary' },
    ],
  },
  {
    id: 'weather', category: 'interaction', titleKey: 'features.weather.title',
    descriptionKey: 'features.weather.description', icon: 'partly-sunny-outline', capability: 'weather',
    visualization: 'timeline', actions: readConfigure('settings.weather'),
  },
  {
    id: 'contacts-sos', category: 'interaction', titleKey: 'features.contactsSos.title',
    descriptionKey: 'features.contactsSos.description', icon: 'people-outline', capability: 'contacts',
    visualization: 'table', actions: [
      ...readConfigure('settings.contacts'),
      { id: 'sos', operation: 'settings.sos.read', labelKey: 'actions.sos', tone: 'secondary' },
    ],
  },
  {
    id: 'clocks-countdown', category: 'interaction', titleKey: 'features.clocksCountdown.title',
    descriptionKey: 'features.clocksCountdown.description', icon: 'time-outline',
    visualization: 'timeline', actions: [
      ...readConfigure('settings.worldClocks'),
      { id: 'countdown', operation: 'settings.countdown.read', labelKey: 'actions.countdown', tone: 'secondary' },
    ],
  },
  {
    id: 'watch-faces', category: 'advanced', titleKey: 'features.watchFaces.title',
    descriptionKey: 'features.watchFaces.description', icon: 'images-outline', capability: 'watchFaces',
    visualization: 'table', advanced: true, actions: [
      { id: 'builtIn', operation: 'advanced.watchFace.builtIn', labelKey: 'actions.builtIn' },
      { id: 'custom', operation: 'advanced.watchFace.custom', labelKey: 'actions.custom' },
      { id: 'server', operation: 'advanced.watchFace.server', labelKey: 'actions.server' },
    ],
  },
  {
    id: 'gps', category: 'advanced', titleKey: 'features.gps.title',
    descriptionKey: 'features.gps.description', icon: 'navigate-outline', capability: 'gps',
    visualization: 'timeline', advanced: true, actions: [
      { id: 'gps', operation: 'advanced.gps', labelKey: 'actions.gps' },
      { id: 'gnss', operation: 'advanced.gnss', labelKey: 'actions.gnss' },
      { id: 'agps', operation: 'advanced.agps', labelKey: 'actions.agps' },
    ],
  },
  {
    id: 'firmware', category: 'advanced', titleKey: 'features.firmware.title',
    descriptionKey: 'features.firmware.description', icon: 'cloud-download-outline',
    visualization: 'log', advanced: true, actions: [
      { id: 'ota', operation: 'advanced.firmware', labelKey: 'actions.firmware', tone: 'danger' },
    ],
  },
  {
    id: 'raw-sensors', category: 'advanced', titleKey: 'features.rawSensors.title',
    descriptionKey: 'features.rawSensors.description', icon: 'code-working-outline',
    visualization: 'line', advanced: true, actions: [
      { id: 'ppg', operation: 'advanced.ppg', labelKey: 'actions.ppg' },
      { id: 'acceleration', operation: 'advanced.acceleration', labelKey: 'actions.acceleration' },
      { id: 'logs', operation: 'advanced.rawLogs', labelKey: 'actions.rawData' },
    ],
  },
  {
    id: 'special-projects', category: 'advanced', titleKey: 'features.specialProjects.title',
    descriptionKey: 'features.specialProjects.description', icon: 'hardware-chip-outline',
    visualization: 'log', advanced: true, actions: [
      { id: 'special', operation: 'advanced.specialProjects', labelKey: 'actions.inspect' },
    ],
  },
  {
    id: 'connected-services', category: 'advanced', titleKey: 'features.connectedServices.title',
    descriptionKey: 'features.connectedServices.description', icon: 'globe-outline',
    visualization: 'log', advanced: true, actions: [
      { id: 'bt', operation: 'advanced.classicBluetooth', labelKey: 'actions.classicBluetooth' },
      { id: '4g', operation: 'advanced.4g', labelKey: 'actions.network4g' },
      { id: 'ai', operation: 'advanced.ai', labelKey: 'actions.ai' },
    ],
  },
  {
    id: 'content-transfer', category: 'advanced', titleKey: 'features.contentTransfer.title',
    descriptionKey: 'features.contentTransfer.description', icon: 'send-outline',
    visualization: 'log', advanced: true, actions: [
      { id: 'textImage', operation: 'advanced.textImage', labelKey: 'actions.textImage' },
      { id: 'qr', operation: 'advanced.qr', labelKey: 'actions.qrCode' },
    ],
  },
  {
    id: 'therapies-custom', category: 'advanced', titleKey: 'features.therapiesCustom.title',
    descriptionKey: 'features.therapiesCustom.description', icon: 'magnet-outline',
    visualization: 'log', advanced: true, actions: [
      { id: 'magnetic', operation: 'advanced.magneticTherapy', labelKey: 'actions.magneticTherapy' },
      { id: 'tcm', operation: 'advanced.tcm', labelKey: 'actions.tcm' },
      { id: 'ptt', operation: 'advanced.ptt', labelKey: 'actions.ptt' },
    ],
  },
];
