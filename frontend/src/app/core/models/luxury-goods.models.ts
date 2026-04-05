export interface AssetSummary {
  assetId: string;
  type: string;
  brand: string;
  owner: string;
  latestInspectionHash: string | null;
  createdAt: string;
  updatedAt: string;
  inspectionCount: number;
  latestInspectionStatus: string | null;
  latestInspectionAt: string | null;
}

export interface AssetEvent {
  eventType: string;
  timestamp: string;
  previousOwner: string | null;
  newOwner: string | null;
  reportHash: string | null;
}

export interface AssetLedgerState {
  assetId: string;
  type: string;
  brand: string;
  owner: string;
  createdAt: string;
  updatedAt: string;
  eventHistory: AssetEvent[];
}

export interface AssetHistoryEntry {
  transactionId: string;
  timestamp: string;
  deleted: boolean;
  asset: AssetLedgerState | null;
}

export interface AssetHistoryResponse {
  assetId: string;
  history: AssetHistoryEntry[];
}

export interface InspectionReport {
  id: string;
  assetId: string;
  reportHash: string;
  inspector: string;
  status: string;
  location: string;
  notes: string;
  inspectedAt: string;
  metadata: Record<string, string>;
  storedAt: string;
  ledgerSynced: boolean;
  ledgerSyncedAt: string | null;
}

export interface CreateAssetRequest {
  assetId: string;
  type: string;
  brand: string;
  owner: string;
}

export interface TransferOwnershipRequest {
  newOwner: string;
}

export interface AddInspectionReportRequest {
  inspector: string;
  status: string;
  location: string;
  notes: string;
  inspectedAt: string;
  metadata: Record<string, string>;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
