import { ComponentFixture, TestBed } from '@angular/core/testing';

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
});
