import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../shared/constants';
import { ApiResponse } from '../shared/models/models';
import { AuthService } from '../auth/auth.service';

export interface InvoiceDTO {
    id: number;
    businessId: number;
    customerName: string;
    customerEmail: string;
    totalAmount: number;
    dueDate: string;
    status: string;
}

export interface InvoiceCreateRequest {
    customerName: string;
    customerEmail: string;
    totalAmount: number;
    dueDate: string;
}

@Injectable({
    providedIn: 'root'
})
export class InvoiceService {
    private apiUrl = `${environment.apiUrl}/invoices`;

    constructor(
        private http: HttpClient,
        private authService: AuthService
    ) { }

    createInvoice(request: InvoiceCreateRequest): Observable<ApiResponse<InvoiceDTO>> {
        const userId = this.authService.getUserId();
        return this.http.post<ApiResponse<InvoiceDTO>>(`${this.apiUrl}/${userId}`, request);
    }

    getInvoices(profileId: number, page: number = 0, size: number = 10): Observable<ApiResponse<any>> {
        const userId = this.authService.getUserId();
        return this.http.get<ApiResponse<any>>(`${this.apiUrl}/user/${userId}`);
    }

    sendInvoice(id: number): Observable<ApiResponse<string>> {
        return this.http.post<ApiResponse<string>>(`${this.apiUrl}/${id}/send`, {});
    }

    markPaid(id: number): Observable<ApiResponse<string>> {
        return this.http.patch<ApiResponse<string>>(`${this.apiUrl}/${id}/pay`, {});
    }
}
