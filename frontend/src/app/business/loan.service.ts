import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../shared/constants';
import { ApiResponse } from '../shared/models/models';
import { AuthService } from '../auth/auth.service';

export interface LoanDTO {
    loanId: number;
    userId: number;
    amount: number;
    interestRate: number;
    tenureMonths: number;
    emiAmount: number;
    remainingAmount: number;
    purpose: string;
    status: string;
    startDate: string;
    endDate: string;
    emiSchedule?: any[];
    showEmi?: boolean;
}

export interface LoanApplyDTO {
    amount: number;
    tenureMonths: number;
    purpose: string;
    loanType: string;
    idempotencyKey: string;
    currency?: string;
}

export interface InstallmentDTO {
    installmentId: number;
    loanId: number;
    installmentNumber: number;
    amount: number;
    dueDate: string;
    status: string;
}

export interface LoanAnalyticsDTO {
    totalOutstanding: number;
    totalPaid: number;
    totalPending: number;
}

@Injectable({
    providedIn: 'root'
})
export class LoanService {
    private apiUrl = `${environment.apiUrl}/loans`;

    constructor(
        private http: HttpClient,
        private authService: AuthService
    ) { }

    applyForLoan(request: LoanApplyDTO): Observable<ApiResponse<any>> {
        const userId = this.authService.getUserId();
        return this.http.post<ApiResponse<any>>(`${this.apiUrl}/apply/${userId}`, request);
    }

    getMyLoans(page: number = 0, size: number = 10): Observable<ApiResponse<any>> {
        const userId = this.authService.getUserId();
        return this.http.get<ApiResponse<any>>(`${this.apiUrl}/user/${userId}`);
    }

    getEmiSchedule(loanId: number, page: number = 0, size: number = 10): Observable<ApiResponse<any>> {
        return this.http.get<ApiResponse<any>>(`${this.apiUrl}/emi/${loanId}?page=${page}&size=${size}`);
    }

    getAnalytics(): Observable<ApiResponse<LoanAnalyticsDTO>> {
        const userId = this.authService.getUserId();
        // Since backend doesn't have an analytics endpoint yet in LoanController, 
        // we might need to implement it or use a default response for now.
        // But for fix, let's assume it should have been /analytics/{userId} or similar
        return this.http.get<ApiResponse<LoanAnalyticsDTO>>(`${this.apiUrl}/analytics/${userId}`);
    }

    repayLoan(loanId: number, amount: number, transactionPin: string, isFullForeclosure: boolean = false): Observable<ApiResponse<string>> {
        if (isFullForeclosure) {
            return this.http.post<ApiResponse<string>>(`${this.apiUrl}/preclose/${loanId}`, {});
        }

        const payload = {
            loanId,
            amount,
            transactionPin,
            isFullForeclosure,
            idempotencyKey: Date.now().toString()
        };
        return this.http.post<ApiResponse<string>>(`${this.apiUrl}/repay`, payload);
    }
}
