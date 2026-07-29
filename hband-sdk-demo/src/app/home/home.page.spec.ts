import { ComponentFixture, TestBed } from '@angular/core/testing';

import type { HBandDataEvent } from '../core/hband.types';
import { HomePage } from './home.page';

describe('HomePage', () => {
  let component: HomePage;
  let fixture: ComponentFixture<HomePage>;

  beforeEach(async () => {
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

  it('should read current daily steps with distance and calories', async () => {
    const feature = component.features.find((item) => item.metric === 'steps')!;
    const current = feature.actions.find((action) => action.id === 'current')!;

    await component.run(current, feature);

    expect(component.hband.latestFor('steps')?.values).toEqual({
      steps: 6842,
      distanceKm: 4.7,
      caloriesKcal: 286,
    });
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
