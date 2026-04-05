import { AddInspectionReportRequest, CreateAssetRequest } from '../models/luxury-goods.models';

export interface ShowcaseEntry {
  asset: CreateAssetRequest;
  inspection: AddInspectionReportRequest;
}

export const SHOWCASE_ENTRIES: ShowcaseEntry[] = [
  {
    asset: {
      assetId: 'LG-ROLEX-001',
      type: 'watch',
      brand: 'Rolex',
      owner: 'Maison Aurelia'
    },
    inspection: {
      inspector: 'Geneva Watch Registry',
      status: 'PASSED',
      location: 'Geneva',
      notes: 'Serial engraving, clasp geometry, and movement finish all align with the archive profile.',
      inspectedAt: '2026-04-05T09:00:00Z',
      metadata: {
        serialMatch: 'true',
        condition: 'museum-grade',
        movement: 'verified'
      }
    }
  },
  {
    asset: {
      assetId: 'LG-HERMES-002',
      type: 'handbag',
      brand: 'Hermes',
      owner: 'Atelier Meridian'
    },
    inspection: {
      inspector: 'Paris Leather Atelier',
      status: 'PASSED',
      location: 'Paris',
      notes: 'Stamp depth, hand-stitch cadence, and leather grain confirmed against sourcing records.',
      inspectedAt: '2026-04-05T10:30:00Z',
      metadata: {
        leather: 'togo',
        hardware: 'palladium',
        seal: 'confirmed'
      }
    }
  },
  {
    asset: {
      assetId: 'LG-CARTIER-003',
      type: 'jewelry',
      brand: 'Cartier',
      owner: 'Vault Nouvelle'
    },
    inspection: {
      inspector: 'Monaco Gem Bureau',
      status: 'PASSED',
      location: 'Monaco',
      notes: 'Stone layout, hallmarks, and archived certificate references reconciled without deviation.',
      inspectedAt: '2026-04-05T12:15:00Z',
      metadata: {
        stones: 'natural',
        hallmark: 'verified',
        certificate: 'matched'
      }
    }
  }
];
