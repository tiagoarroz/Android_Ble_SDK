import { CommonModule } from '@angular/common';
import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  IonBadge, IonButton, IonButtons, IonCheckbox, IonContent, IonHeader, IonIcon,
  IonDatetime, IonInput, IonItem, IonLabel, IonList, IonModal, IonNote, IonProgressBar,
  IonSelect, IonSelectOption, IonSpinner, IonTitle, IonToolbar,
} from '@ionic/angular/standalone';
import type { DatetimeHighlight } from '@ionic/core';
import { addIcons } from 'ionicons';
import {
  albumsOutline, analyticsOutline, arrowBackOutline, batteryDeadOutline, batteryFullOutline, batteryHalfOutline, bluetoothOutline,
  bodyOutline, calendarOutline, chevronForwardOutline,
  closeCircleOutline, cloudOfflineOutline, fitnessOutline, flashOutline, heartOutline,
  footstepsOutline, informationCircleOutline, leafOutline, medicalOutline, pulseOutline,
  playOutline, refreshOutline, saveOutline, speedometerOutline, stopOutline, syncOutline, thermometerOutline, trashOutline, watchOutline,
  waterOutline,
} from 'ionicons/icons';

import { DataVisualizerComponent } from '../components/data-visualizer/data-visualizer.component';
import { FEATURE_CATALOG } from '../core/feature-catalog';
import { HBandService } from '../core/hband.service';
import type {
  DataValue, FeatureAction, FeatureDefinition, HBandDataEvent, HBandHistoryRecord,
  HBandLogEntry, HBandMonitoringSetting, HBandReadingSource, MetricId,
} from '../core/hband.types';
import { I18nService } from '../core/i18n.service';

@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  imports: [
    CommonModule, FormsModule, DataVisualizerComponent, IonBadge, IonButton,
    IonButtons, IonCheckbox, IonContent, IonDatetime, IonHeader, IonIcon, IonInput, IonItem,
    IonLabel, IonList, IonModal, IonNote, IonProgressBar, IonSelect,
    IonSelectOption, IonSpinner, IonTitle, IonToolbar,
  ],
})
export class HomePage implements OnInit {
  readonly hband = inject(HBandService);
  readonly i18n = inject(I18nService);
  readonly features = FEATURE_CATALOG;
  readonly selectedFeature = signal<FeatureDefinition | null>(null);
  readonly featureView = signal<'summary' | 'allData'>('summary');
  readonly allDataSource = signal<HBandReadingSource>('manual');
  readonly measurementFeature = signal<FeatureDefinition | null>(null);
  readonly savePromptOpen = signal(false);
  readonly savingMeasurement = signal(false);
  readonly scanOpen = signal(false);
  readonly historyCalendarOpen = signal(false);
  readonly password = signal('0000');
  readonly selectedDate = signal(this.localDate(new Date()));
  readonly today = this.localDate(new Date());
  private measurementWasActive = false;

  constructor() {
    addIcons({
      albumsOutline, analyticsOutline, arrowBackOutline, batteryDeadOutline, batteryFullOutline, batteryHalfOutline, bluetoothOutline,
      bodyOutline, calendarOutline, chevronForwardOutline,
      closeCircleOutline, cloudOfflineOutline, fitnessOutline, flashOutline, heartOutline,
      footstepsOutline, informationCircleOutline, leafOutline, medicalOutline, pulseOutline,
      playOutline, refreshOutline, saveOutline, speedometerOutline, stopOutline, syncOutline, thermometerOutline, trashOutline, watchOutline,
      waterOutline,
    });

    /*
     * Observa a transição ativa -> terminada produzida pelos callbacks nativos.
     * Assim, medições finitas abrem a confirmação sem depender de polling.
     */
    effect(() => {
      const feature = this.measurementFeature();
      const active = feature ? this.hband.measurementActive(feature.metric) : false;
      if (
        feature
        && this.measurementWasActive
        && !active
        && this.measurementResultAvailable(feature)
      ) {
        this.savePromptOpen.set(true);
      }
      this.measurementWasActive = active;
    });
  }

  async ngOnInit(): Promise<void> {
    await this.hband.initialize();
    if (!this.hband.connected()) {
      await this.hband.synchroniseDate(this.selectedDate());
    }
  }

  async openScan(): Promise<void> {
    this.scanOpen.set(true);
    await this.hband.scan();
  }

  async connect(deviceId: string): Promise<void> {
    await this.hband.connect(deviceId, this.password());
    this.scanOpen.set(false);
  }

  /**
   * Abre sempre a métrica no dia atual. Assim, a data pertence à navegação da
   * métrica e nunca se propaga de volta para o painel inicial.
   */
  async openFeature(feature: FeatureDefinition): Promise<void> {
    const needsToday = this.selectedDate() !== this.today;
    if (needsToday) {
      this.selectedDate.set(this.today);
    }
    this.featureView.set('summary');
    this.allDataSource.set('manual');
    this.selectedFeature.set(feature);
    try {
      if (needsToday) {
        await this.hband.synchroniseDate(this.today);
      }
      await this.hband.refreshMonitoringSettings();
    } catch {
      // O serviço conserva a falha técnica no registo de atividade.
    }
  }

  /**
   * Fecha a área da métrica e repõe o dia atual, incluindo o arquivo carregado
   * no serviço, para que a página inicial nunca conserve um contexto antigo.
   */
  closeFeature(): void {
    this.selectedFeature.set(null);
    this.featureView.set('summary');
    this.allDataSource.set('manual');
    this.historyCalendarOpen.set(false);
    if (this.selectedDate() === this.today) {
      return;
    }
    this.selectedDate.set(this.today);
    void this.hband.synchroniseDate(this.today);
  }

  openAllData(): void {
    this.allDataSource.set('manual');
    this.featureView.set('allData');
  }

  closeAllData(): void {
    this.featureView.set('summary');
  }

  selectAllDataSource(source: HBandReadingSource): void {
    this.allDataSource.set(source);
  }

  /**
   * Os cartões iniciais apresentam apenas registos já guardados para hoje.
   * Leituras antigas ou medições em tempo real ainda não confirmadas não são
   * promovidas para o resumo diário.
   */
  todayHistoryFor(metric: MetricId): HBandDataEvent | undefined {
    const history = this.hband.historyFor(metric);
    return history?.date === this.today && history.records?.length ? history : undefined;
  }

  /**
   * Ordena as leituras mais recentes primeiro e aplica uma classificação
   * compatível aos registos anteriores à introdução do campo `source`.
   */
  historyRecordsBySource(
    feature: FeatureDefinition,
    source: HBandReadingSource,
  ): HBandHistoryRecord[] {
    return [...(this.hband.historyFor(feature.metric)?.records ?? [])]
      .filter((record) => this.readingSource(record, feature) === source)
      .sort((left, right) =>
        new Date(right.timestamp).getTime() - new Date(left.timestamp).getTime());
  }

  historyRecordEntries(record: HBandHistoryRecord): Array<[string, DataValue]> {
    const controlFields = new Set([
      'progress', 'state', 'success', 'enabled', 'synchronised', 'lead',
    ]);
    return Object.entries(record.values).filter(([field]) => !controlFields.has(field));
  }

  historyFieldLabel(field: string): string {
    return this.i18n.translate(`data.fields.${field}`);
  }

  historyRecordValue(value: DataValue): string {
    if (typeof value === 'boolean') {
      return this.i18n.translate(value ? 'common.yes' : 'common.no');
    }
    if (typeof value === 'number') {
      return new Intl.NumberFormat(this.locale(), { maximumFractionDigits: 2 }).format(value);
    }
    return value === null ? '—' : String(value);
  }

  private readingSource(
    record: HBandHistoryRecord,
    feature: FeatureDefinition,
  ): HBandReadingSource {
    if (record.source) {
      return record.source;
    }
    return feature.metric === 'ecg' || feature.metric === 'bodyComposition'
      ? 'manual'
      : 'automatic';
  }

  async toggleMonitoring(
    setting: HBandMonitoringSetting,
    enabled = !setting.enabled,
  ): Promise<void> {
    if (enabled === setting.enabled) {
      return;
    }
    try {
      await this.hband.setMonitoring(setting.metric, enabled);
    } catch {
      // Mantém o estado confirmado pela pulseira quando a escrita falha.
    }
  }

  syncPercent(): number {
    const sync = this.hband.syncStatus();
    return sync.total > 0 ? Math.round((sync.completed / sync.total) * 100) : 0;
  }

  /**
   * Alterna as medições com um único controlo ou executa uma leitura pontual,
   * sem alterar os contratos confirmados da bridge.
   */
  async run(action: FeatureAction, feature: FeatureDefinition): Promise<void> {
    if (action.stopOperation) {
      await this.hband.toggleMeasurement(feature.metric, action.operation, action.stopOperation);
      return;
    }
    await this.hband.execute(action.operation);
  }

  measurementAction(feature: FeatureDefinition): FeatureAction | undefined {
    return feature.actions.find((action) => action.stopOperation);
  }

  /**
   * Abre a superfície dedicada e inicia logo a medição escolhida. Os erros
   * permanecem visíveis nos logs e deixam o botão disponível para nova tentativa.
   */
  async openMeasurement(feature: FeatureDefinition): Promise<void> {
    const action = this.measurementAction(feature);
    if (!action) {
      return;
    }
    this.measurementWasActive = false;
    this.savePromptOpen.set(false);
    this.measurementFeature.set(feature);
    try {
      await this.run(action, feature);
    } catch {
      // O serviço já registou o erro técnico no painel desta medição.
    }
  }

  /**
   * Alterna o único botão da vista de tempo real entre iniciar e parar.
   */
  async toggleLiveMeasurement(feature: FeatureDefinition): Promise<void> {
    const action = this.measurementAction(feature);
    if (!action || this.savePromptOpen()) {
      return;
    }
    try {
      await this.run(action, feature);
    } catch {
      // O serviço já registou o erro técnico no painel desta medição.
    }
  }

  /**
   * Ao sair de uma leitura ativa, termina primeiro o comando. Se já existirem
   * dados reais, a transição de estado abre a confirmação de gravação.
   */
  async requestMeasurementExit(feature: FeatureDefinition): Promise<void> {
    if (this.hband.measurementActive(feature.metric)) {
      await this.toggleLiveMeasurement(feature);
      if (this.measurementResultAvailable(feature)) {
        this.savePromptOpen.set(true);
        return;
      }
    }
    this.closeMeasurement();
  }

  /**
   * Aceita apenas valores clínicos ou amostras reais; progresso e estados do
   * protocolo, isoladamente, não constituem uma medição que possa ser guardada.
   */
  measurementResultAvailable(feature: FeatureDefinition): boolean {
    const event = this.hband.finalFor(feature.metric) ?? this.hband.latestFor(feature.metric);
    const controlFields = new Set([
      'progress', 'state', 'success', 'enabled', 'synchronised', 'lead',
    ]);
    return Boolean(
      event
      && (
        (event.samples?.length ?? 0) > 0
        || Object.entries(event.values).some(
          ([field, value]) => !controlFields.has(field) && value !== null,
        )
      )
    );
  }

  measurementResultDate(feature: FeatureDefinition): string {
    const event = this.hband.finalFor(feature.metric) ?? this.hband.latestFor(feature.metric);
    const timestamp = event?.timestamp ? new Date(event.timestamp) : new Date();
    const validDate = Number.isNaN(timestamp.getTime()) ? new Date() : timestamp;
    return new Intl.DateTimeFormat(this.locale(), { dateStyle: 'long' }).format(validDate);
  }

  /**
   * Persiste o resultado confirmado, muda para o respetivo dia real e regressa
   * ao histórico enquanto o arquivo local é recarregado.
   */
  async saveMeasurement(feature: FeatureDefinition): Promise<void> {
    this.savingMeasurement.set(true);
    try {
      const date = await this.hband.archiveCurrentMeasurement(feature.metric);
      if (!date) {
        return;
      }
      this.selectedDate.set(date);
      this.closeMeasurement();
      void this.hband.synchroniseDate(date);
    } finally {
      this.savingMeasurement.set(false);
    }
  }

  discardMeasurement(): void {
    this.closeMeasurement();
  }

  closeMeasurement(): void {
    this.savePromptOpen.set(false);
    this.measurementFeature.set(null);
    this.measurementWasActive = false;
  }

  actionOperation(action: FeatureAction, feature: FeatureDefinition): string {
    return action.stopOperation && this.hband.measurementActive(feature.metric)
      ? action.stopOperation
      : action.operation;
  }

  actionLabel(action: FeatureAction, feature: FeatureDefinition): string {
    const labelKey = action.stopOperation
      && this.hband.measurementActive(feature.metric)
      && action.activeLabelKey
      ? action.activeLabelKey
      : action.labelKey;
    return this.i18n.translate(labelKey);
  }

  /**
   * Expõe o payload integral recebido da bridge dentro de uma área com scroll,
   * incluindo amostras, registos históricos e valor bruto quando disponíveis.
   */
  formatLogPayload(entry: HBandLogEntry): string {
    if (!entry.data) {
      return entry.message;
    }
    return JSON.stringify({
      values: entry.data.values,
      samples: entry.data.samples,
      records: entry.data.records,
      raw: entry.data.raw,
    }, null, 2);
  }

  logTitle(entry: HBandLogEntry): string {
    if (entry.kind === 'data') {
      return this.i18n.translate('measurementLogs.deviceData');
    }
    if (entry.kind === 'operation') {
      const key = entry.level === 'success'
        ? 'measurementLogs.operationAccepted'
        : entry.level === 'error'
          ? 'measurementLogs.operationFailed'
          : 'measurementLogs.operationRequested';
      return this.i18n.translate(key);
    }
    return this.i18n.translate('measurementLogs.bridge');
  }

  /**
   * Atualiza a data apresentada e abre imediatamente o arquivo desse dia.
   * Se existir ligação, o serviço tenta também obter dados mais recentes.
   */
  async setDate(value: string | number | null | undefined): Promise<void> {
    const date = String(value ?? '').slice(0, 10);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(date) || date === this.selectedDate()) {
      return;
    }
    this.selectedDate.set(date);
    await this.hband.synchroniseDate(date);
  }

  /**
   * Ao selecionar um dia, fecha o calendário e reutiliza o mesmo fluxo de
   * carregamento local e atualização BLE do seletor principal.
   */
  async selectCalendarDate(value: string | string[] | null | undefined): Promise<void> {
    const selected = Array.isArray(value) ? value[0] : value;
    /*
     * Fecha primeiro o calendário para que a pessoa volte imediatamente à
     * métrica aberta e acompanhe aí o carregamento do novo dia.
     */
    this.historyCalendarOpen.set(false);
    await this.setDate(selected);
  }

  /**
   * O `ion-datetime` recebe uma lista derivada do índice persistente da
   * pulseira atual, não apenas das métricas carregadas no ecrã.
   */
  historyHighlightedDates(): DatetimeHighlight[] {
    return this.hband.historyDates().map((date) => ({
      date,
      backgroundColor: '#13795b',
      textColor: '#ffffff',
      border: '1px solid #0b5f46',
    }));
  }

  formattedSelectedDate(): string {
    return new Intl.DateTimeFormat(this.locale(), { dateStyle: 'medium' })
      .format(new Date(`${this.selectedDate()}T12:00:00`));
  }

  /**
   * Indica no seletor da modal que o arquivo do dia escolhido está a ser
   * carregado ou atualizado pela pulseira.
   */
  historyLoading(): boolean {
    const state = this.hband.historyState();
    return state.date === this.selectedDate()
      && (state.phase === 'loading' || state.phase === 'syncing');
  }

  /**
   * Explica se o dia veio do arquivo local, da pulseira ou da união de ambos,
   * distinguindo a ausência de ligação da retenção já ultrapassada.
   */
  historyStatusLabel(): string {
    const state = this.hband.historyState();
    const hasData = state.recordCount > 0;
    const key = state.phase === 'offline'
      ? hasData ? 'history.status.offlineWithData' : 'history.status.offlineEmpty'
      : state.phase === 'outsideRetention'
        ? hasData ? 'history.status.outsideRetentionWithData' : 'history.status.outsideRetentionEmpty'
        : state.phase === 'error'
          ? hasData ? 'history.status.errorWithData' : 'history.status.errorEmpty'
          : `history.status.${state.phase}`;
    return this.i18n.translate(key);
  }

  historyStatusIcon(): string {
    const phase = this.hband.historyState().phase;
    return phase === 'syncing' || phase === 'loading'
      ? 'sync-outline'
      : phase === 'offline'
        ? 'cloud-offline-outline'
        : phase === 'error' ? 'close-circle-outline' : 'albums-outline';
  }

  historyMetadata(): string {
    const state = this.hband.historyState();
    const parts: string[] = [];
    const sync = this.hband.syncStatus();
    if (sync.state === 'partial' && sync.date === state.date) {
      parts.push(this.i18n.translate('sync.partialProgress', {
        completed: sync.completed,
        total: sync.total,
      }));
      parts.push(this.i18n.translate(
        sync.failureReason === 'timeout' ? 'sync.timeout' : 'sync.failed',
        { operation: this.syncOperationLabel(sync.failedOperation) },
      ));
    }
    if (state.source !== 'none') {
      parts.push(this.i18n.translate(`history.sources.${state.source}`));
    }
    parts.push(this.i18n.translate('history.records', { count: state.recordCount }));
    if (state.lastUpdatedAt) {
      parts.push(this.i18n.translate('history.updatedAt', {
        date: new Intl.DateTimeFormat(this.locale(), {
          dateStyle: 'short',
          timeStyle: 'short',
        }).format(new Date(state.lastUpdatedAt)),
      }));
    }
    return parts.join(' · ');
  }

  /**
   * Converte o identificador interno da operação numa descrição legível, sem
   * expor ao utilizador os nomes técnicos usados pela bridge Capacitor.
   */
  private syncOperationLabel(operation?: string): string {
    const keyByOperation: Record<string, string> = {
      'device.battery': 'sync.operations.battery',
      'device.time': 'sync.operations.time',
      'history.activity.current': 'sync.operations.activity',
      'history.daily': 'sync.operations.daily',
      'history.manual.daily': 'sync.operations.manual',
      'history.metric.ecg': 'sync.operations.ecg',
      'history.metric.bodyComposition': 'sync.operations.bodyComposition',
    };
    const key = operation?.startsWith('history.metric')
      ? 'sync.operations.dedicated'
      : keyByOperation[operation ?? ''] ?? 'sync.operations.unknown';
    return this.i18n.translate(keyByOperation[operation ?? ''] ?? key);
  }

  historyRetentionLabel(): string {
    return this.i18n.translate('history.retention', {
      days: this.hband.historyState().retentionDays ?? 0,
    });
  }

  measurementProgress(event: HBandDataEvent | undefined): number {
    const value = event?.values['progress'];
    return typeof value === 'number' && Number.isFinite(value)
      ? Math.min(100, Math.max(0, value))
      : 0;
  }

  formatGlucose(event: HBandDataEvent): string {
    const value = event.values['mmolL'];
    if (typeof value !== 'number') {
      return '—';
    }
    const locale = this.i18n.language() === 'br' ? 'pt-BR' : this.i18n.language();
    return new Intl.NumberFormat(locale, {
      minimumFractionDigits: 1,
      maximumFractionDigits: 2,
    }).format(value);
  }

  capability(feature: FeatureDefinition): 'supported' | 'unsupported' | 'unknown' {
    return this.hband.capability(feature.capability);
  }

  statusLabel(): string {
    return this.i18n.translate(`connectionStates.${this.hband.status().state}`);
  }

  batteryPercent(): number {
    return Math.min(100, Math.max(0, this.hband.battery()?.percent ?? 0));
  }

  /**
   * Escolhe um ícone que permite reconhecer visualmente a autonomia sem
   * depender apenas do valor percentual.
   */
  batteryIcon(): string {
    const level = this.batteryLevel();
    return level === 'low'
      ? 'battery-dead-outline'
      : level === 'medium' ? 'battery-half-outline' : 'battery-full-outline';
  }

  /**
   * Agrupa a carga em três estados visuais e respeita também o aviso de
   * bateria fraca enviado diretamente pelo dispositivo.
   */
  batteryLevel(): 'low' | 'medium' | 'high' {
    const battery = this.hband.battery();
    const percent = this.batteryPercent();
    if (battery?.lowBattery || percent <= 20) {
      return 'low';
    }
    return percent <= 60 ? 'medium' : 'high';
  }

  private localDate(date: Date): string {
    const offset = date.getTimezoneOffset() * 60_000;
    return new Date(date.getTime() - offset).toISOString().slice(0, 10);
  }

  locale(): string {
    return this.i18n.language() === 'br' ? 'pt-BR' : this.i18n.language();
  }
}
