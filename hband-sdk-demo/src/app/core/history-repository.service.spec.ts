import { TestBed } from '@angular/core/testing';

import { HistoryRepositoryService } from './history-repository.service';
import type { HBandDataEvent } from './hband.types';

describe('HistoryRepositoryService', () => {
  let repository: HistoryRepositoryService;

  beforeEach(() => {
    repository = TestBed.inject(HistoryRepositoryService);
  });

  it('should replace a repeated timestamp instead of duplicating it', async () => {
    const deviceId = `MF91-TEST-${Date.now()}`;
    const date = '2026-06-08';
    const first = historyEvent(date, '2026-06-08T14:30:00', 69);
    const corrected = historyEvent(date, '2026-06-08T14:30:00', 71);

    await repository.mergeEvent(deviceId, first);
    await repository.mergeEvent(deviceId, corrected);
    const snapshot = await repository.loadDate(deviceId, date);

    expect(snapshot.recordCount).toBe(1);
    expect(snapshot.events.heartRate?.records?.[0].values['bpm']).toBe(71);
  });

  it('should keep archives from different bands isolated', async () => {
    const date = '2026-06-11';
    await repository.mergeEvent('MF91-A', historyEvent(date, `${date}T08:00:00`, 64));
    await repository.mergeEvent('MF91-B', historyEvent(date, `${date}T08:00:00`, 82));

    const firstBand = await repository.loadDate('MF91-A', date);
    const secondBand = await repository.loadDate('MF91-B', date);

    expect(firstBand.events.heartRate?.records?.[0].values['bpm']).toBe(64);
    expect(secondBand.events.heartRate?.records?.[0].values['bpm']).toBe(82);
  });

  it('should list only dates stored for the requested band', async () => {
    const deviceId = `MF91-DATES-${Date.now()}`;
    await repository.mergeEvent(
      deviceId,
      historyEvent('2026-06-08', '2026-06-08T08:00:00', 66),
    );
    await repository.mergeEvent(
      deviceId,
      historyEvent('2026-06-11', '2026-06-11T09:00:00', 72),
    );
    await repository.mergeEvent(
      `${deviceId}-OTHER`,
      historyEvent('2026-06-09', '2026-06-09T09:00:00', 80),
    );

    expect(await repository.listDates(deviceId)).toEqual(['2026-06-08', '2026-06-11']);
  });

  function historyEvent(date: string, timestamp: string, bpm: number): HBandDataEvent {
    return {
      type: 'history',
      metric: 'heartRate',
      date,
      timestamp: new Date().toISOString(),
      values: { records: 1 },
      records: [{ timestamp, values: { bpm } }],
    };
  }
});
