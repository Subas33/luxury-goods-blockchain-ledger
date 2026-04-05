import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { distinctUntilChanged, filter, firstValueFrom, map } from 'rxjs';

import {
  AssetHistoryEntry,
  AssetSummary,
  InspectionReport
} from '../../core/models/luxury-goods.models';
import { LuxuryGoodsApiService } from '../../core/services/luxury-goods-api.service';
import {
  assetDisplayName,
  estimateValue,
  eventHeadline,
  eventSubline,
  hashPreview,
  inspectionLabel,
  inspectionTone,
  readApiError,
  titleCase
} from '../../core/utils/luxury-asset.presenter';

@Component({
  selector: 'app-asset-detail-page',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './asset-detail-page.html',
  styleUrl: './asset-detail-page.scss'
})
export class AssetDetailPage {
  private readonly api = inject(LuxuryGoodsApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder);

  readonly assetId = signal('');
  readonly asset = signal<AssetSummary | null>(null);
  readonly history = signal<AssetHistoryEntry[]>([]);
  readonly inspections = signal<InspectionReport[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly transfering = signal(false);
  readonly recordingInspection = signal(false);
  readonly actionMessage = signal<string | null>(null);

  readonly transferForm = this.formBuilder.nonNullable.group({
    newOwner: ['', [Validators.required, Validators.maxLength(100)]]
  });

  readonly inspectionForm = this.formBuilder.nonNullable.group({
    inspector: ['House Authentication Desk', [Validators.required, Validators.maxLength(100)]],
    status: ['PASSED', [Validators.required, Validators.maxLength(50)]],
    location: ['Geneva Client Lounge', [Validators.required, Validators.maxLength(150)]],
    notes: [
      'Packaging, physical markers, and archival references were reviewed and reconciled.',
      [Validators.required, Validators.maxLength(4000)]
    ],
    inspectedAt: [this.defaultInspectionTime(), [Validators.required]],
    serialMatch: ['true'],
    condition: ['excellent'],
    specialist: ['Lead Authenticator']
  });

  readonly timeline = computed(() =>
    this.history().map((entry) => {
      const latestEvent = entry.asset?.eventHistory.at(-1);
      return {
        transactionId: entry.transactionId,
        timestamp: entry.timestamp,
        headline: eventHeadline(latestEvent),
        subline: eventSubline(latestEvent),
        hash: latestEvent?.reportHash ?? null
      };
    })
  );

  readonly assetTitle = computed(() => {
    const asset = this.asset();
    return asset ? assetDisplayName(asset) : 'Asset dossier';
  });

  readonly assetValue = computed(() => {
    const asset = this.asset();
    return asset ? estimateValue(asset) : '--';
  });

  readonly currentStatus = computed(() => inspectionLabel(this.asset()?.latestInspectionStatus ?? null));
  readonly currentTone = computed(() => inspectionTone(this.asset()?.latestInspectionStatus ?? null));

  constructor() {
    this.route.paramMap
      .pipe(
        map((params) => params.get('id') ?? ''),
        filter(Boolean),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((assetId) => {
        this.assetId.set(assetId);
        this.actionMessage.set(null);
        void this.loadAsset(assetId);
      });
  }

  async loadAsset(assetId: string): Promise<void> {
    this.loading.set(true);
    this.error.set(null);

    try {
      const [asset, history, inspections] = await Promise.all([
        firstValueFrom(this.api.getAsset(assetId)),
        firstValueFrom(this.api.getAssetHistory(assetId)),
        firstValueFrom(this.api.getInspectionReports(assetId))
      ]);

      this.asset.set(asset);
      this.history.set(history.history);
      this.inspections.set(inspections);
    } catch (error) {
      this.error.set(readApiError(error, 'Unable to open this asset dossier.'));
    } finally {
      this.loading.set(false);
    }
  }

  async submitTransfer(): Promise<void> {
    if (this.transferForm.invalid) {
      this.transferForm.markAllAsTouched();
      return;
    }

    this.transfering.set(true);
    this.actionMessage.set(null);

    try {
      await firstValueFrom(this.api.transferOwnership(this.assetId(), this.transferForm.getRawValue()));
      this.transferForm.reset({ newOwner: '' });
      await this.loadAsset(this.assetId());
      this.actionMessage.set('Ownership transfer committed to Fabric and mirrored back to the storefront.');
    } catch (error) {
      this.actionMessage.set(readApiError(error, 'Ownership transfer failed.'));
    } finally {
      this.transfering.set(false);
    }
  }

  async submitInspection(): Promise<void> {
    if (this.inspectionForm.invalid) {
      this.inspectionForm.markAllAsTouched();
      return;
    }

    this.recordingInspection.set(true);
    this.actionMessage.set(null);

    try {
      const { inspector, status, location, notes, inspectedAt, serialMatch, condition, specialist } =
        this.inspectionForm.getRawValue();

      await firstValueFrom(
        this.api.addInspectionReport(this.assetId(), {
          inspector,
          status,
          location,
          notes,
          inspectedAt: new Date(inspectedAt).toISOString(),
          metadata: {
            serialMatch,
            condition,
            specialist
          }
        })
      );

      await this.loadAsset(this.assetId());
      this.actionMessage.set('Inspection dossier stored in MongoDB and hashed onto the ledger.');
    } catch (error) {
      this.actionMessage.set(readApiError(error, 'Inspection recording failed.'));
    } finally {
      this.recordingInspection.set(false);
    }
  }

  protected readonly hashPreview = hashPreview;
  protected readonly inspectionLabel = inspectionLabel;
  protected readonly inspectionTone = inspectionTone;
  protected readonly titleCase = titleCase;

  private defaultInspectionTime(): string {
    const now = new Date();
    now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
    return now.toISOString().slice(0, 16);
  }
}
