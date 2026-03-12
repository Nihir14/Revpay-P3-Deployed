import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../shared/constants';
import { ApiResponse, Transaction, PaymentMethodDTO, CardPayload } from '../shared/models/models';
import { AuthService } from '../auth/auth.service';

@Injectable({ providedIn: 'root' })
export class WalletService {
  // Updated URL mapping to match microservices behind API Gateway
  private walletsUrl = `${environment.apiUrl}/wallets`;
  private transactionsUrl = `${environment.apiUrl}/transactions`;
  private invoicesUrl = `${environment.apiUrl}/invoices`;

  constructor(private http: HttpClient, private authService: AuthService) { }

  private get baseWalletUrl(): string {
    const userId = this.authService.getUserId();
    return `${this.walletsUrl}/user/${userId}`;
  }

  private get baseTransactionUrl(): string {
    const userId = this.authService.getUserId();
    return `${this.transactionsUrl}`;
  }

  // Core Money Movement
  getBalance(): Observable<ApiResponse<number>> {
    // Backend WalletController.getWallet matches /api/wallets/user/{userId}
    // and returns WalletDto which contains balance.
    return this.http.get<ApiResponse<number>>(`${this.baseWalletUrl}`);
  }

  addFunds(payload: any): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseWalletUrl}/add-funds`, payload);
  }

  sendMoney(payload: any): Observable<ApiResponse<any>> {
    // Transaction service: /api/transactions/transfer/{senderId}
    const senderId = this.authService.getUserId();
    return this.http.post<ApiResponse<any>>(`${this.baseTransactionUrl}/transfer/${senderId}`, payload);
  }

  getTransactions(page = 0, size = 20): Observable<ApiResponse<any>> {
    // Transaction service: /api/transactions/history/{userId}
    const userId = this.authService.getUserId();
    return this.http.get<ApiResponse<any>>(`${this.baseTransactionUrl}/history/${userId}?page=${page}&size=${size}`);
  }

  exportTransactionPdf(userId: number): Observable<Blob> {
    return this.http.get(`${this.baseTransactionUrl}/export/pdf/${userId}`, { responseType: 'blob' });
  }

  exportTransactionCsv(userId: number): Observable<Blob> {
    return this.http.get(`${this.baseTransactionUrl}/export/csv/${userId}`, { responseType: 'blob' });
  }

  // Cards
  getCards(page = 0, size = 10): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.baseWalletUrl}/cards?page=${page}&size=${size}`);
  }

  addCard(payload: CardPayload): Observable<ApiResponse<PaymentMethodDTO>> {
    return this.http.post<ApiResponse<PaymentMethodDTO>>(`${this.baseWalletUrl}/cards`, payload);
  }

  deleteCard(cardId: number): Observable<ApiResponse<string>> {
    return this.http.delete<ApiResponse<string>>(`${this.baseWalletUrl}/cards/${cardId}`);
  }

  setDefaultCard(cardId: number): Observable<ApiResponse<string>> {
    return this.http.post<ApiResponse<string>>(`${this.baseWalletUrl}/cards/default/${cardId}`, {});
  }

  // Requests - In microservices these might need their own controller or remain in Wallet if strictly internal
  requestMoney(payload: any): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseWalletUrl}/request`, payload);
  }

  getIncomingRequests(page = 0, size = 10): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.baseWalletUrl}/requests/incoming?page=${page}&size=${size}`);
  }

  getOutgoingRequests(page = 0, size = 10): Observable<ApiResponse<any>> {
    return this.http.get<ApiResponse<any>>(`${this.baseWalletUrl}/requests/outgoing?page=${page}&size=${size}`);
  }

  getPendingInvoices(): Observable<ApiResponse<any>> {
    const email = this.authService.getUserEmail();
    return this.http.get<ApiResponse<any>>(`${this.invoicesUrl}/customer/pending?email=${email}`);
  }

  payInvoice(invoiceId: number, pin: string): Observable<ApiResponse<any>> {
    const userId = this.authService.getUserId();
    return this.http.post<ApiResponse<any>>(`${this.invoicesUrl}/pay/${userId}/${invoiceId}?pin=${pin}`, {});
  }

  acceptRequest(txnId: number, pin: string): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseWalletUrl}/request/accept/${txnId}`, { pin });
  }

  declineRequest(txnId: number): Observable<ApiResponse<any>> {
    return this.http.post<ApiResponse<any>>(`${this.baseWalletUrl}/request/decline/${txnId}`, {});
  }

  // Filter
  filterTransactions(paramsObj: any): Observable<ApiResponse<any>> {
    const userId = this.authService.getUserId();
    let params = new HttpParams();
    Object.keys(paramsObj).forEach(key => {
      if (paramsObj[key] !== null && paramsObj[key] !== '') {
        if ((key === 'startDate' || key === 'endDate') && typeof paramsObj[key] === 'string') {
          const dateStr = paramsObj[key].endsWith('Z') ? paramsObj[key].slice(0, -1) : paramsObj[key];
          params = params.append(key, dateStr);
        } else {
          params = params.append(key, paramsObj[key]);
        }
      }
    });
    return this.http.get<ApiResponse<any>>(`${this.baseTransactionUrl}/filter/${userId}`, { params });
  }
}
