import {
  ComponentFixture, TestBed, fakeAsync, flushMicrotasks, tick,
} from '@angular/core/testing';

import type { HBandDataEvent } from '../core/hband.types';
import { HomePage } from './home.page';

describe('HomePage', () => {
  let component: HomePage;
  let fixture: ComponentFixture<HomePage>;

  beforeEach(async () => {
    localStorage.removeItem('hband-device-session');
    fixture = TestBed.createComponent(HomePage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should expose one measurement button that alternates its operation', async () => {
    const feature = component.features.find((item) => item.metric === 'heartRate')!;
    const measurement = feature.actions.find((action) => action.stopOperation)!;

    expect(feature.actions.filter((action) => action.stopOperation).length).toBe(1);
    expect(component.actionOperation(measurement, feature)).toBe('measure.heartRate.start');

    await component.run(measurement, feature);

    expect(component.hband.measurementActive(feature.metric)).toBeTrue();
    expect(component.actionOperation(measurement, feature)).toBe('measure.heartRate.stop');

    await component.run(measurement, feature);

    expect(component.hband.measurementActive(feature.metric)).toBeFalse();
  });

  it('should include operation and real payload entries in the metric log', async () => {
    const feature = component.features.find((item) => item.metric === 'heartRate')!;
    const measurement = feature.actions.find((action) => action.stopOperation)!;

    await component.run(measurement, feature);

    const logs = component.hband.metricLogs(feature.metric);
    const dataEntry = logs.find((entry) => entry.kind === 'data');

    expect(logs.some((entry) => entry.kind === 'operation')).toBeTrue();
    expect(dataEntry?.data?.values['bpm']).toBe(74);
    expect(component.formatLogPayload(dataEntry!)).toContain('"bpm": 74');
  });

  it('should keep heart rate active when STATE_HEART_NORMAL carries a bpm', async () => {
    const feature = component.features.find((item) => item.metric === 'heartRate')!;
    const measurement = feature.actions.find((action) => action.stopOperation)!;

    await component.run(measurement, feature);
    feedDeviceData({
      type: 'heartRate',
      metric: 'heartRate',
      timestamp: new Date().toISOString(),
      values: { bpm: 72, state: 'STATE_HEART_NORMAL' },
    });

    expect(component.hband.measurementActive('heartRate')).toBeTrue();
    expect(component.actionOperation(measurement, feature)).toBe('measure.heartRate.stop');
  });

  it('should finish finite measurements only when full progress is received', async () => {
    const feature = component.features.find((item) => item.metric === 'stress')!;
    const measurement = feature.actions.find((action) => action.stopOperation)!;

    await component.run(measurement, feature);
    expect(component.hband.measurementActive('stress')).toBeTrue();

    feedDeviceData({
      type: 'stress',
      metric: 'stress',
      timestamp: new Date().toISOString(),
      values: { progress: 100, score: 32 },
    });

    expect(component.hband.measurementActive('stress')).toBeFalse();
    expect(component.actionOperation(measurement, feature)).toBe('measure.stress.start');
  });

  it('should accumulate a rolling ECG sample window from native ADC packets', () => {
    const timestamp = new Date().toISOString();
    feedDeviceData({
      type: 'ecg',
      metric: 'ecg',
      timestamp,
      values: { progress: 1 },
      samples: [1, 3, 2],
    });
    feedDeviceData({
      type: 'ecg',
      metric: 'ecg',
      timestamp,
      values: { progress: 2 },
      samples: [4, 2],
    });

    expect(component.hband.latestFor('ecg')?.samples).toEqual([1, 3, 2, 4, 2]);
  });

  it('should update steps through synchronisation without a manual action', () => {
    const feature = component.features.find((item) => item.metric === 'steps')!;

    expect(feature.actions).toEqual([]);
  });

  it('should expose only history from today on the home cards', () => {
    component.hband.history.set({
      heartRate: {
        type: 'history',
        metric: 'heartRate',
        date: component.today,
        timestamp: new Date().toISOString(),
        values: { records: 1 },
        records: [{
          timestamp: `${component.today}T08:00:00`,
          values: { bpm: 68 },
          source: 'automatic',
        }],
      },
    });

    expect(component.todayHistoryFor('heartRate')?.records?.length).toBe(1);

    component.hband.history.update((history) => ({
      ...history,
      heartRate: { ...history.heartRate!, date: '2026-06-08' },
    }));

    expect(component.todayHistoryFor('heartRate')).toBeUndefined();
  });

  it('should summarise daily activity and the latest clinical reading on home cards', () => {
    component.hband.history.set({
      steps: {
        type: 'history',
        metric: 'steps',
        date: component.today,
        timestamp: new Date().toISOString(),
        values: { records: 2 },
        records: [
          {
            timestamp: `${component.today}T08:00:00`,
            values: { steps: 700, distanceKm: 0.5, caloriesKcal: 30 },
          },
          {
            timestamp: `${component.today}T11:00:00`,
            values: { steps: 800, distanceKm: 0.6, caloriesKcal: 35 },
          },
        ],
      },
      bloodPressure: {
        type: 'history',
        metric: 'bloodPressure',
        date: component.today,
        timestamp: new Date().toISOString(),
        values: { records: 2 },
        records: [
          {
            timestamp: `${component.today}T12:00:00`,
            values: { systolic: 121, diastolic: 79, pulseBpm: 73 },
          },
          {
            timestamp: `${component.today}T09:00:00`,
            values: { systolic: 118, diastolic: 76 },
          },
        ],
      },
    });

    const steps = component.homeMetricSummary(
      component.features.find((feature) => feature.metric === 'steps')!,
    );
    const bloodPressure = component.homeMetricSummary(
      component.features.find((feature) => feature.metric === 'bloodPressure')!,
    );

    expect(steps?.primary.replace(/\D/g, '')).toBe('1500');
    expect(steps?.secondary).toContain('65 kcal');
    expect(bloodPressure?.primary).toBe('121/79');
    expect(bloodPressure?.timestamp).toContain('T12:00:00');
  });

  it('should expose and toggle only monitoring settings returned by the device', async () => {
    const heartRate = component.features.find((item) => item.metric === 'heartRate')!;
    component.hband.status.update((status) => ({
      ...status,
      state: 'connected',
      capabilities: { ...status.capabilities, autoMeasure: 'supported' },
    }));

    await component.openFeature(heartRate);
    const initial = component.hband.monitoringFor('heartRate');

    expect(initial?.enabled).toBeTrue();
    expect(initial?.intervalMinutes).toBe(10);
    expect(component.hband.monitoringFor('steps')).toBeUndefined();

    await component.toggleMonitoring(initial!);

    expect(component.hband.monitoringFor('heartRate')?.enabled).toBeFalse();
  });

  it('should retain the battery percentage reported outside metric events', () => {
    feedDeviceData({
      type: 'battery',
      timestamp: new Date().toISOString(),
      values: { percent: 75, lowBattery: false },
    });

    expect(component.hband.battery()?.percent).toBe(75);
    expect(component.batteryPercent()).toBe(75);
  });

  it('should open the connected band details and save its edited name', async () => {
    await component.hband.connect('MF91-TEST');

    component.openDeviceDetails();
    component.startDeviceNameEdit();
    component.updateRenameDeviceName('Saude');
    await component.submitDeviceRename();

    expect(component.hband.status().device?.name).toBe('Saude');
    expect(component.deviceDetailsOpen()).toBeTrue();
    expect(component.editingDeviceName()).toBeFalse();
    expect(component.renameDeviceError()).toBeNull();
    expect(component.deviceModelName()).toBe('MF91');
    expect(JSON.parse(localStorage.getItem('hband-device-session') ?? '{}')).toEqual(
      jasmine.objectContaining({
        deviceId: 'MF91-TEST',
        name: 'Saude',
        model: 'MF91',
      }),
    );
  });

  it('should limit the band name to eight ASCII-safe characters', async () => {
    component.hband.status.update((status) => ({
      ...status,
      state: 'connected',
      device: { id: 'MF91-TEST', name: 'MF91' },
    }));
    component.openDeviceDetails();
    component.startDeviceNameEdit();
    component.updateRenameDeviceName('Saúde!1234');

    expect(component.renameDeviceName()).toBe('Saúde!12');
    expect(component.renameDeviceCharacterCount()).toBe(8);
    expect(component.deviceNameCharactersValid()).toBeFalse();
    expect(component.renameDeviceCanSave()).toBeFalse();

    await component.submitDeviceRename();
    expect(component.renameDeviceError()).toBe('invalid');

    component.updateRenameDeviceName('Saude 12');
    expect(component.deviceNameCharactersValid()).toBeTrue();
    expect(component.renameDeviceCanSave()).toBeTrue();
  });

  it('should disconnect from the details modal and close it', async () => {
    component.hband.status.update((status) => ({
      ...status,
      state: 'connected',
      device: { id: 'MF91-TEST', name: 'MF91', model: 'MF91' },
    }));
    component.openDeviceDetails();

    await component.disconnectFromDeviceDetails();

    expect(component.deviceDetailsOpen()).toBeFalse();
    expect(component.hband.connected()).toBeFalse();
  });

  it('should migrate an old session by restoring the advertised name instead of the MAC', async () => {
    localStorage.setItem('hband-device-session', JSON.stringify({
      deviceId: '1B:F0:06:E2:86:FC',
      password: '0000',
    }));
    component.hband.simulation.set(false);
    let connectOptions: {
      deviceId: string;
      password?: string;
      name?: string;
      model?: string;
    } | undefined;
    const testableService = component.hband as unknown as {
      nativeConnect(options: {
        deviceId: string;
        password?: string;
        name?: string;
        model?: string;
      }): Promise<void>;
      discoverRememberedDevice(deviceId: string): Promise<{
        id: string;
        name: string;
        model?: string;
      } | undefined>;
      restoreDeviceSession(): Promise<void>;
    };
    testableService.discoverRememberedDevice = async (deviceId) => ({
      id: deviceId,
      name: 'Saude 1',
      model: 'MF91',
    });
    testableService.nativeConnect = async (options) => {
      connectOptions = options;
      component.hband.status.update((status) => ({
        ...status,
        state: 'connected',
        device: {
          id: options.deviceId,
          name: options.name ?? '',
          model: options.model,
        },
      }));
    };

    await testableService.restoreDeviceSession();

    expect(connectOptions).toEqual(jasmine.objectContaining({
      deviceId: '1B:F0:06:E2:86:FC',
      name: 'Saude 1',
      model: 'MF91',
    }));
    expect(component.hband.status().device?.name).toBe('Saude 1');
    expect(JSON.parse(localStorage.getItem('hband-device-session') ?? '{}')).toEqual(
      jasmine.objectContaining({ name: 'Saude 1', model: 'MF91' }),
    );
  });

  it('should highlight every date stored for the current band', () => {
    component.hband.historyDates.set(['2026-06-08', '2026-06-11']);

    expect(component.historyHighlightedDates()).toEqual([
      jasmine.objectContaining({ date: '2026-06-08' }),
      jasmine.objectContaining({ date: '2026-06-11' }),
    ]);
  });

  it('should change the history day from an open measurement modal', async () => {
    const heartRate = component.features.find((item) => item.metric === 'heartRate')!;
    component.selectedFeature.set(heartRate);
    component.historyCalendarOpen.set(true);
    let finishSynchronisation!: () => void;
    const synchronisation = new Promise<void>((resolve) => {
      finishSynchronisation = resolve;
    });
    const syncDate = spyOn(component.hband, 'synchroniseDate').and.returnValue(synchronisation);

    const selection = component.selectCalendarDate('2026-06-08');

    expect(component.historyCalendarOpen()).toBeFalse();
    expect(component.selectedDate()).toBe('2026-06-08');
    expect(component.selectedFeature()).toBe(heartRate);
    expect(syncDate).toHaveBeenCalledOnceWith('2026-06-08');

    finishSynchronisation();
    await selection;
  });

  it('should reset the selected day when returning to the home page', () => {
    const heartRate = component.features.find((item) => item.metric === 'heartRate')!;
    component.selectedFeature.set(heartRate);
    component.featureView.set('allData');
    component.selectedDate.set('2026-06-08');
    const syncDate = spyOn(component.hband, 'synchroniseDate').and.resolveTo();

    component.closeFeature();

    expect(component.selectedFeature()).toBeNull();
    expect(component.featureView()).toBe('summary');
    expect(component.selectedDate()).toBe(component.today);
    expect(syncDate).toHaveBeenCalledOnceWith(component.today);
  });

  it('should separate automatic readings from manual measurements', () => {
    const heartRate = component.features.find((item) => item.metric === 'heartRate')!;
    component.hband.history.set({
      heartRate: {
        type: 'history',
        metric: 'heartRate',
        date: component.today,
        timestamp: new Date().toISOString(),
        values: { records: 2 },
        records: [
          {
            timestamp: `${component.today}T08:00:00`,
            values: { bpm: 68 },
            source: 'automatic',
          },
          {
            timestamp: `${component.today}T08:30:00`,
            values: { bpm: 74 },
            source: 'manual',
          },
        ],
      },
    });

    expect(component.historyRecordsBySource(heartRate, 'automatic').map(
      (record) => record.values['bpm'],
    )).toEqual([68]);
    expect(component.historyRecordsBySource(heartRate, 'manual').map(
      (record) => record.values['bpm'],
    )).toEqual([74]);
  });

  it('should open all data on the manual measurements tab', () => {
    component.allDataSource.set('automatic');

    component.openAllData();

    expect(component.featureView()).toBe('allData');
    expect(component.allDataSource()).toBe('manual');

    component.selectAllDataSource('automatic');
    expect(component.allDataSource()).toBe('automatic');
  });

  it('should open a focused live view and ask to save after stopping', async () => {
    const feature = component.features.find((item) => item.metric === 'heartRate')!;
    component.selectedFeature.set(feature);

    await component.openMeasurement(feature);
    fixture.detectChanges();

    expect(component.selectedFeature()).toBe(feature);
    expect(component.measurementFeature()).toBe(feature);
    expect(component.hband.measurementActive(feature.metric)).toBeTrue();
    expect(component.hband.latestFor(feature.metric)?.values['bpm']).toBe(74);

    await component.toggleLiveMeasurement(feature);
    fixture.detectChanges();

    expect(component.hband.measurementActive(feature.metric)).toBeFalse();
    expect(component.savePromptOpen()).toBeTrue();
  });

  it('should archive the confirmed result and return to the daily history', async () => {
    const feature = component.features.find((item) => item.metric === 'heartRate')!;
    const testableService = component.hband as unknown as {
      historyDeviceId: string;
    };
    testableService.historyDeviceId = 'MF91-TEST';
    component.selectedDate.set('2026-06-08');

    await component.openMeasurement(feature);
    fixture.detectChanges();
    await component.toggleLiveMeasurement(feature);
    fixture.detectChanges();
    await component.saveMeasurement(feature);

    const archived = component.hband.historyFor(feature.metric);
    expect(component.measurementFeature()).toBeNull();
    expect(component.savePromptOpen()).toBeFalse();
    expect(component.selectedDate()).toBe(component.today);
    expect(archived?.records?.some((record) => record.values['bpm'] === 74)).toBeTrue();
    expect(archived?.records?.some((record) => record.source === 'manual')).toBeTrue();
    expect(component.hband.historyDates()).toContain(component.today);
  });

  it('should ask to save when a finite measurement reaches full progress', async () => {
    const feature = component.features.find((item) => item.metric === 'stress')!;

    await component.openMeasurement(feature);
    fixture.detectChanges();
    feedDeviceData({
      type: 'stress',
      metric: 'stress',
      timestamp: new Date().toISOString(),
      values: { progress: 100, score: 32 },
    });
    fixture.detectChanges();

    expect(component.hband.measurementActive(feature.metric)).toBeFalse();
    expect(component.savePromptOpen()).toBeTrue();
  });

  it('should indicate when the selected metric day is loading', () => {
    component.hband.historyState.set({
      date: component.selectedDate(),
      phase: 'syncing',
      source: 'local',
      recordCount: 2,
    });

    expect(component.historyLoading()).toBeTrue();

    component.hband.historyState.update((state) => ({ ...state, phase: 'ready' }));
    expect(component.historyLoading()).toBeFalse();
  });

  it('should release the measurement controls when a native operation times out', fakeAsync(() => {
    component.hband.simulation.set(false);
    const testableService = component.hband as unknown as {
      nativeExecute(
        operation: string,
        params: Record<string, unknown>,
      ): Promise<{ operation: string; accepted: boolean }>;
    };
    testableService.nativeExecute = () => new Promise(() => undefined);
    let rejectedError: unknown;

    void component.hband.execute('device.battery').catch((error) => {
      rejectedError = error;
    });
    flushMicrotasks();

    expect(component.hband.busyOperation()).toBe('device.battery');

    tick(10_000);
    flushMicrotasks();

    expect(component.hband.busyOperation()).toBeNull();
    expect((rejectedError as Error).message).toContain('SDK_OPERATION_TIMEOUT');
  }));

  it('should stop the current synchronisation after its first timeout', async () => {
    const execute = spyOn(component.hband, 'execute').and.rejectWith(
      new Error('SDK_OPERATION_TIMEOUT: history.daily (45000ms)'),
    );
    component.hband.status.update((status) => ({
      ...status,
      historyRetentionDays: 3,
      platform: 'android',
    }));
    const testableService = component.hband as unknown as {
      synchroniseRequestedDate(
        date: string,
        includeSessionOperations: boolean,
        reportProgress: boolean,
      ): Promise<boolean>;
    };
    const yesterday = new Date(Date.now() - 86_400_000).toISOString().slice(0, 10);

    const canContinue = await testableService.synchroniseRequestedDate(
      yesterday,
      false,
      false,
    );

    expect(canContinue).toBeFalse();
    expect(execute).toHaveBeenCalledTimes(1);
  });

  it('should not request the unsupported grouped manual reader during Android sync', async () => {
    const execute = spyOn(component.hband, 'execute').and.resolveTo();
    component.hband.status.update((status) => ({
      ...status,
      platform: 'android',
      capabilities: {
        ...status.capabilities,
        ecg: 'supported',
        bodyComposition: 'supported',
      },
    }));
    const testableService = component.hband as unknown as {
      synchroniseRequestedDate(
        date: string,
        includeSessionOperations: boolean,
        reportProgress: boolean,
      ): Promise<boolean>;
    };

    await testableService.synchroniseRequestedDate(component.today, true, true);

    const operations = execute.calls.allArgs().map(([operation]) => operation);
    expect(operations).toEqual([
      'device.battery',
      'device.time',
      'history.activity.current',
      'history.daily',
      'history.metric',
      'history.metric',
    ]);
    expect(operations).not.toContain('history.manual.daily');
    expect(component.hband.syncStatus()).toEqual(jasmine.objectContaining({
      state: 'complete',
      completed: 6,
      total: 6,
      failed: 0,
    }));
  });

  it('should explain a timed-out reading without showing an active sync', () => {
    component.i18n.setLanguage('pt');
    component.hband.historyState.set({
      date: component.today,
      phase: 'error',
      source: 'none',
      recordCount: 0,
    });
    component.hband.syncStatus.set({
      state: 'partial',
      date: component.today,
      completed: 5,
      total: 6,
      failed: 1,
      failedOperation: 'history.metric.ecg',
      failureReason: 'timeout',
    });

    expect(component.historyStatusLabel()).toBe('Não foi possível atualizar este dia');
    expect(component.historyMetadata()).toContain('5 de 6 leituras processadas');
    expect(component.historyMetadata()).toContain('histórico de ECG');
    expect(component.historyStatusIcon()).toBe('close-circle-outline');
  });

  /**
   * Injeta callbacks equivalentes aos eventos nativos sem expor a operação
   * interna do serviço no contrato de produção.
   */
  function feedDeviceData(event: HBandDataEvent): void {
    const testableService = component.hband as unknown as {
      storeData(data: HBandDataEvent): void;
    };
    testableService.storeData(event);
  }
});
