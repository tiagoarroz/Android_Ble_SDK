import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  IonBadge,
  IonButton,
  IonButtons,
  IonChip,
  IonContent,
  IonHeader,
  IonIcon,
  IonInput,
  IonItem,
  IonLabel,
  IonList,
  IonModal,
  IonNote,
  IonProgressBar,
  IonSearchbar,
  IonSegment,
  IonSegmentButton,
  IonSelect,
  IonSelectOption,
  IonSpinner,
  IonTitle,
  IonToggle,
  IonToolbar,
} from '@ionic/angular/standalone';
import { addIcons } from 'ionicons';
import {
  alarmOutline,
  analyticsOutline,
  batteryHalfOutline,
  bicycleOutline,
  bluetoothOutline,
  bodyOutline,
  calendarOutline,
  cameraOutline,
  chatboxEllipsesOutline,
  checkmarkCircle,
  chevronForwardOutline,
  closeCircleOutline,
  cloudDownloadOutline,
  codeWorkingOutline,
  fileTrayFullOutline,
  fitnessOutline,
  flaskOutline,
  flashOutline,
  flowerOutline,
  globeOutline,
  handLeftOutline,
  hardwareChipOutline,
  heartOutline,
  helpCircleOutline,
  imagesOutline,
  informationCircleOutline,
  keyOutline,
  languageOutline,
  leafOutline,
  locateOutline,
  magnetOutline,
  medicalOutline,
  moonOutline,
  musicalNotesOutline,
  navigateOutline,
  notificationsOutline,
  partlySunnyOutline,
  peopleOutline,
  powerOutline,
  pulseOutline,
  radioOutline,
  refreshOutline,
  searchOutline,
  sendOutline,
  speedometerOutline,
  stopCircleOutline,
  thermometerOutline,
  timeOutline,
  timerOutline,
  walkOutline,
  watchOutline,
  waterOutline,
} from 'ionicons/icons';

import { DataVisualizerComponent } from '../components/data-visualizer/data-visualizer.component';
import { FEATURE_CATALOG } from '../core/feature-catalog';
import { HBandService } from '../core/hband.service';
import type { FeatureCategory, FeatureDefinition } from '../core/hband.types';
import { I18nService } from '../core/i18n.service';

@Component({
  selector: 'app-home',
  templateUrl: 'home.page.html',
  imports: [
    CommonModule,
    FormsModule,
    DataVisualizerComponent,
    IonBadge,
    IonButton,
    IonButtons,
    IonChip,
    IonContent,
    IonHeader,
    IonIcon,
    IonInput,
    IonItem,
    IonLabel,
    IonList,
    IonModal,
    IonNote,
    IonProgressBar,
    IonSearchbar,
    IonSegment,
    IonSegmentButton,
    IonSelect,
    IonSelectOption,
    IonSpinner,
    IonTitle,
    IonToggle,
    IonToolbar,
  ],
})
export class HomePage implements OnInit {
  readonly hband = inject(HBandService);
  readonly i18n = inject(I18nService);
  readonly categories: Array<{ id: 'all' | FeatureCategory; labelKey: string }> = [
    { id: 'all', labelKey: 'categories.all' },
    { id: 'connection', labelKey: 'categories.connection' },
    { id: 'measurements', labelKey: 'categories.measurements' },
    { id: 'history', labelKey: 'categories.history' },
    { id: 'automation', labelKey: 'categories.automation' },
    { id: 'interaction', labelKey: 'categories.interaction' },
    { id: 'advanced', labelKey: 'categories.advanced' },
  ];
  readonly selectedCategory = signal<'all' | FeatureCategory>('all');
  readonly searchTerm = signal('');
  readonly selectedFeature = signal<FeatureDefinition | null>(null);
  readonly scanOpen = signal(false);
  readonly password = signal('0000');
  readonly profileHeight = signal('');
  readonly profileWeight = signal('');
  readonly profileBirthYear = signal('');
  readonly profileAge = signal('');
  readonly profileTargetSteps = signal('');
  readonly profileSex = signal('');
  readonly profileError = signal(false);
  readonly filteredFeatures = computed(() => {
    const category = this.selectedCategory();
    const search = this.searchTerm().trim().toLocaleLowerCase(this.i18n.language());
    return FEATURE_CATALOG.filter((feature) => {
      const inCategory = category === 'all' || feature.category === category;
      const searchable = `${this.i18n.translate(feature.titleKey)} ${this.i18n.translate(feature.descriptionKey)}`.toLocaleLowerCase(this.i18n.language());
      return inCategory && (!search || searchable.includes(search));
    });
  });

  constructor() {
    addIcons({
      alarmOutline,
      analyticsOutline,
      batteryHalfOutline,
      bicycleOutline,
      bluetoothOutline,
      bodyOutline,
      calendarOutline,
      cameraOutline,
      chatboxEllipsesOutline,
      checkmarkCircle,
      chevronForwardOutline,
      closeCircleOutline,
      cloudDownloadOutline,
      codeWorkingOutline,
      fileTrayFullOutline,
      fitnessOutline,
      flaskOutline,
      flashOutline,
      flowerOutline,
      globeOutline,
      handLeftOutline,
      hardwareChipOutline,
      heartOutline,
      helpCircleOutline,
      imagesOutline,
      informationCircleOutline,
      keyOutline,
      languageOutline,
      leafOutline,
      locateOutline,
      magnetOutline,
      medicalOutline,
      moonOutline,
      musicalNotesOutline,
      navigateOutline,
      notificationsOutline,
      partlySunnyOutline,
      peopleOutline,
      powerOutline,
      pulseOutline,
      radioOutline,
      refreshOutline,
      searchOutline,
      sendOutline,
      speedometerOutline,
      stopCircleOutline,
      thermometerOutline,
      timeOutline,
      timerOutline,
      walkOutline,
      watchOutline,
      waterOutline,
    });
  }

  async ngOnInit(): Promise<void> {
    await this.hband.initialize();
  }

  setCategory(value: string | number | undefined): void {
    const category = String(value ?? 'all') as 'all' | FeatureCategory;
    this.selectedCategory.set(category);
  }

  setSearch(value: string | null | undefined): void {
    this.searchTerm.set(value ?? '');
  }

  openFeature(feature: FeatureDefinition): void {
    this.selectedFeature.set(feature);
  }

  closeFeature(): void {
    this.selectedFeature.set(null);
  }

  async openScan(): Promise<void> {
    this.scanOpen.set(true);
    await this.hband.scan();
  }

  async connect(deviceId: string): Promise<void> {
    await this.hband.connect(deviceId, this.password());
    this.scanOpen.set(false);
  }

  async run(operation: string): Promise<void> {
    if (operation === 'session.scan') {
      await this.openScan();
      return;
    }
    if (operation === 'session.disconnect') {
      await this.hband.disconnect();
      return;
    }
    if (operation === 'device.profile') {
      const heightCm = Number(this.profileHeight());
      const weightKg = Number(this.profileWeight());
      const birthYear = Number(this.profileBirthYear());
      const age = Number(this.profileAge());
      const targetSteps = Number(this.profileTargetSteps());
      const sex = this.profileSex();
      if (
        !Number.isInteger(heightCm)
        || !Number.isInteger(weightKg)
        || !Number.isInteger(birthYear)
        || !Number.isInteger(age)
        || !Number.isInteger(targetSteps)
        || !['male', 'female'].includes(sex)
      ) {
        this.profileError.set(true);
        return;
      }
      this.profileError.set(false);
      await this.hband.execute(operation, {
        heightCm,
        weightKg,
        birthYear,
        age,
        targetSteps,
        sex,
      });
      return;
    }
    await this.hband.execute(operation);
  }

  capability(feature: FeatureDefinition): 'supported' | 'unsupported' | 'unknown' {
    return this.hband.capability(feature.capability);
  }

  statusLabel(): string {
    return this.i18n.translate(`connectionStates.${this.hband.status().state}`);
  }

  statusIcon(feature: FeatureDefinition): string {
    const state = this.capability(feature);
    if (state === 'supported') {
      return 'checkmark-circle';
    }
    if (state === 'unsupported') {
      return 'close-circle-outline';
    }
    return 'help-circle-outline';
  }
}
