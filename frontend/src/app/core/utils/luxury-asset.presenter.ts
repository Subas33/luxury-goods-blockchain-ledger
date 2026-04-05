import { ApiErrorResponse, AssetEvent, AssetSummary } from '../models/luxury-goods.models';

const BRAND_BASE_VALUE: Record<string, number> = {
  rolex: 28500,
  hermes: 22500,
  cartier: 19600,
  louisvuitton: 9100,
  patekphilippe: 68000,
  chanel: 12800
};

const TYPE_MULTIPLIER: Record<string, number> = {
  watch: 1.28,
  handbag: 1.0,
  jewelry: 1.42,
  luggage: 1.16,
  apparel: 0.74
};

export function titleCase(value: string): string {
  return value
    .split(/[\s_-]+/)
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1).toLowerCase())
    .join(' ');
}

export function assetDisplayName(asset: Pick<AssetSummary, 'brand' | 'type'>): string {
  return `${titleCase(asset.brand)} ${titleCase(asset.type)}`;
}

export function estimateValue(asset: Pick<AssetSummary, 'brand' | 'type' | 'assetId'>): string {
  const brandKey = asset.brand.replace(/\s+/g, '').toLowerCase();
  const typeKey = asset.type.toLowerCase();
  const baseValue = BRAND_BASE_VALUE[brandKey] ?? 14200;
  const multiplier = TYPE_MULTIPLIER[typeKey] ?? 1;
  const checksum = asset.assetId.split('').reduce((sum, char) => sum + char.charCodeAt(0), 0) % 7000;
  const amount = Math.round((baseValue * multiplier) + checksum);

  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0
  }).format(amount);
}

export function inspectionTone(status: string | null): 'verified' | 'watch' | 'pending' {
  const normalized = (status ?? '').toUpperCase();
  if (normalized === 'PASSED' || normalized === 'VERIFIED') {
    return 'verified';
  }
  if (normalized === 'FAILED' || normalized === 'FLAGGED') {
    return 'watch';
  }
  return 'pending';
}

export function inspectionLabel(status: string | null): string {
  return status ? titleCase(status) : 'Pending inspection';
}

export function blockchainBadge(asset: AssetSummary): string {
  if (asset.latestInspectionHash) {
    return 'Ledger anchored';
  }
  return 'Awaiting first attestation';
}

export function hashPreview(hash: string | null | undefined): string {
  if (!hash) {
    return 'No inspection hash yet';
  }
  return `${hash.slice(0, 12)}...${hash.slice(-8)}`;
}

export function eventHeadline(event: AssetEvent | undefined): string {
  if (!event) {
    return 'Ledger update';
  }

  switch (event.eventType) {
    case 'REGISTERED':
      return `Registered for ${event.newOwner ?? 'initial owner'}`;
    case 'OWNERSHIP_TRANSFERRED':
      return `Ownership transferred to ${event.newOwner ?? 'new owner'}`;
    case 'INSPECTION_RECORDED':
      return 'Inspection report hashed on-chain';
    default:
      return titleCase(event.eventType.replace(/_/g, ' '));
  }
}

export function eventSubline(event: AssetEvent | undefined): string {
  if (!event) {
    return 'A ledger mutation was recorded for this asset.';
  }

  if (event.eventType === 'OWNERSHIP_TRANSFERRED') {
    return `${event.previousOwner ?? 'Unknown owner'} to ${event.newOwner ?? 'current owner'}`;
  }

  if (event.eventType === 'INSPECTION_RECORDED') {
    return hashPreview(event.reportHash);
  }

  return 'Immutable transaction committed to Fabric.';
}

export function readApiError(error: unknown, fallback: string): string {
  if (typeof error === 'object' && error !== null && 'error' in error) {
    const payload = (error as { error?: ApiErrorResponse }).error;
    if (payload?.message) {
      return payload.message;
    }
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return fallback;
}
