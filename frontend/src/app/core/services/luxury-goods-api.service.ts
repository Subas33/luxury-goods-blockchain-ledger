import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AddInspectionReportRequest,
  AssetHistoryResponse,
  AssetLedgerState,
  AssetSummary,
  CreateAssetRequest,
  InspectionReport,
  TransferOwnershipRequest
} from '../models/luxury-goods.models';

@Injectable({ providedIn: 'root' })
export class LuxuryGoodsApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080';

  listAssets(): Observable<AssetSummary[]> {
    return this.http.get<AssetSummary[]>(`${this.baseUrl}/assets`);
  }

  getAsset(assetId: string): Observable<AssetSummary> {
    return this.http.get<AssetSummary>(`${this.baseUrl}/assets/${assetId}`);
  }

  getAssetHistory(assetId: string): Observable<AssetHistoryResponse> {
    return this.http.get<AssetHistoryResponse>(`${this.baseUrl}/assets/${assetId}/history`);
  }

  getInspectionReports(assetId: string): Observable<InspectionReport[]> {
    return this.http.get<InspectionReport[]>(`${this.baseUrl}/assets/${assetId}/inspections`);
  }

  registerAsset(payload: CreateAssetRequest): Observable<AssetLedgerState> {
    return this.http.post<AssetLedgerState>(`${this.baseUrl}/assets`, payload);
  }

  transferOwnership(assetId: string, payload: TransferOwnershipRequest): Observable<AssetLedgerState> {
    return this.http.post<AssetLedgerState>(`${this.baseUrl}/assets/${assetId}/transfer`, payload);
  }

  addInspectionReport(assetId: string, payload: AddInspectionReportRequest): Observable<InspectionReport> {
    return this.http.post<InspectionReport>(`${this.baseUrl}/assets/${assetId}/inspection`, payload);
  }
}
