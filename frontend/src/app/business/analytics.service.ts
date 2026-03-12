import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../shared/constants';
import { ApiResponse } from '../shared/models/models';

export interface BusinessSummaryDTO {
    totalReceived: number;
    totalSent: number;
    pendingAmount: number;
    totalTransactionCount: number;
    currency: string;
}

@Injectable({
    providedIn: 'root'
})
export class BusinessAnalyticsService {
    private apiUrl = `${environment.apiUrl}/v1/business/analytics`; // Standardized path

    constructor(private http: HttpClient) { }

    getSummary(businessId: number): Observable<ApiResponse<BusinessSummaryDTO>> {
        return this.http.get<ApiResponse<BusinessSummaryDTO>>(`${this.apiUrl}/${businessId}/summary`);
    }
}
