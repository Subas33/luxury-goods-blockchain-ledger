import { CommonModule } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { AssetSummary } from '../../../core/models/luxury-goods.models';
import {
  assetDisplayName,
  blockchainBadge,
  estimateValue,
  hashPreview,
  inspectionLabel,
  inspectionTone,
  titleCase
} from '../../../core/utils/luxury-asset.presenter';

@Component({
  selector: 'app-asset-card',
  imports: [CommonModule, RouterLink],
  templateUrl: './asset-card.html',
  styleUrl: './asset-card.scss'
})
export class AssetCardComponent {
  readonly asset = input.required<AssetSummary>();

  readonly displayName = computed(() => assetDisplayName(this.asset()));
  readonly valueLabel = computed(() => estimateValue(this.asset()));
  readonly statusLabel = computed(() => inspectionLabel(this.asset().latestInspectionStatus));
  readonly statusTone = computed(() => inspectionTone(this.asset().latestInspectionStatus));
  readonly hashLabel = computed(() => hashPreview(this.asset().latestInspectionHash));
  readonly blockchainLabel = computed(() => blockchainBadge(this.asset()));

  protected readonly titleCase = titleCase;
}
