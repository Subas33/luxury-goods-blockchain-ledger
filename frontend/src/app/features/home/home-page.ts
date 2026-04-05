import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

import { SHOWCASE_ENTRIES } from '../../core/data/showcase-assets';
import { AssetSummary } from '../../core/models/luxury-goods.models';
import { LuxuryGoodsApiService } from '../../core/services/luxury-goods-api.service';
import { readApiError, titleCase } from '../../core/utils/luxury-asset.presenter';
import { AssetCardComponent } from '../../shared/components/asset-card/asset-card';

@Component({
  selector: 'app-home-page',
  imports: [CommonModule, ReactiveFormsModule, AssetCardComponent],
  templateUrl: './home-page.html',
  styleUrl: './home-page.scss'
})
export class HomePage {
  private readonly api = inject(LuxuryGoodsApiService);
  private readonly formBuilder = inject(FormBuilder);

  readonly assets = signal<AssetSummary[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly actionMessage = signal<string | null>(null);
  readonly searchTerm = signal('');
  readonly activeType = signal('All');
  readonly registering = signal(false);
  readonly seedingShowcase = signal(false);

  readonly registerForm = this.formBuilder.nonNullable.group({
    assetId: ['', [Validators.required, Validators.maxLength(100)]],
    type: ['watch', [Validators.required, Validators.maxLength(100)]],
    brand: ['Rolex', [Validators.required, Validators.maxLength(100)]],
    owner: ['Maison Aurelia', [Validators.required, Validators.maxLength(100)]]
  });

  readonly typeFilters = computed(() => {
    const types = new Set(this.assets().map((asset) => titleCase(asset.type)));
    return ['All', ...Array.from(types)];
  });

  readonly filteredAssets = computed(() => {
    const normalizedQuery = this.searchTerm().trim().toLowerCase();
    const selectedType = this.activeType();

    return this.assets().filter((asset) => {
      const matchesType = selectedType === 'All' || titleCase(asset.type) === selectedType;
      const haystack = [asset.assetId, asset.brand, asset.type, asset.owner].join(' ').toLowerCase();
      const matchesQuery = !normalizedQuery || haystack.includes(normalizedQuery);
      return matchesType && matchesQuery;
    });
  });

  readonly stats = computed(() => {
    const assets = this.assets();
    const ownerCount = new Set(assets.map((asset) => asset.owner)).size;
    const anchoredCount = assets.filter((asset) => Boolean(asset.latestInspectionHash)).length;
    const inspectionVolume = assets.reduce((total, asset) => total + asset.inspectionCount, 0);

    return [
      { label: 'Pieces in collection', value: String(assets.length).padStart(2, '0') },
      { label: 'Active custodians', value: String(ownerCount).padStart(2, '0') },
      { label: 'Ledger-anchored assets', value: String(anchoredCount).padStart(2, '0') },
      { label: 'Inspection attestations', value: String(inspectionVolume).padStart(2, '0') }
    ];
  });

  constructor() {
    void this.loadAssets();
  }

  async loadAssets(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);

    try {
      const assets = await firstValueFrom(this.api.listAssets());
      this.assets.set(assets);
    } catch (error) {
      this.error.set(readApiError(error, 'Unable to load the catalog from the middleware.'));
    } finally {
      this.loading.set(false);
    }
  }

  setSearch(term: string): void {
    this.searchTerm.set(term);
  }

  selectType(type: string): void {
    this.activeType.set(type);
  }

  async submitRegister(): Promise<void> {
    if (this.registerForm.invalid) {
      this.registerForm.markAllAsTouched();
      return;
    }

    this.registering.set(true);
    this.actionMessage.set(null);

    try {
      await firstValueFrom(this.api.registerAsset(this.registerForm.getRawValue()));
      this.actionMessage.set('Asset registered on Fabric and reflected in MongoDB.');
      this.registerForm.patchValue({ assetId: '' });
      await this.loadAssets();
    } catch (error) {
      this.actionMessage.set(readApiError(error, 'Asset registration failed.'));
    } finally {
      this.registering.set(false);
    }
  }

  async seedShowcase(): Promise<void> {
    this.seedingShowcase.set(true);
    this.actionMessage.set(null);

    let createdCount = 0;

    for (const entry of SHOWCASE_ENTRIES) {
      try {
        await firstValueFrom(this.api.registerAsset(entry.asset));
        createdCount += 1;
      } catch (error) {
        const message = readApiError(error, 'Failed to register showcase asset.');
        if (!message.toLowerCase().includes('already exists')) {
          this.actionMessage.set(message);
          this.seedingShowcase.set(false);
          return;
        }
      }

      try {
        await firstValueFrom(this.api.addInspectionReport(entry.asset.assetId, entry.inspection));
      } catch (error) {
        const message = readApiError(error, 'Failed to add showcase inspection.');
        if (!message.toLowerCase().includes('already exists')) {
          this.actionMessage.set(message);
          this.seedingShowcase.set(false);
          return;
        }
      }
    }

    this.actionMessage.set(
      createdCount > 0
        ? `Showcase inventory seeded. ${createdCount} new assets were created and authenticated.`
        : 'Showcase inventory was already present. Nothing new needed to be minted.'
    );
    await this.loadAssets();
    this.seedingShowcase.set(false);
  }

  trackByAssetId(_: number, asset: AssetSummary): string {
    return asset.assetId;
  }
}
