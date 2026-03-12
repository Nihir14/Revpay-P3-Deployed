import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { WalletService } from '../wallet.service';
import { Transaction } from '../../shared/models/models';
import { AuthService } from '../../auth/auth.service';
import { NotificationService } from '../../core/notification.service';

@Component({
  standalone: true,
  selector: 'app-transactions',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './transactions.html',
  styleUrl: './transactions.css'
})
export class TransactionsComponent implements OnInit {
  transactions: Transaction[] = [];
  filterForm: FormGroup;
  currentPage = 0;
  totalPages = 1;
  pageSize = 20;
  isExporting = false;
  currentUserId: number | null = null;

  constructor(private fb: FormBuilder, private walletService: WalletService, private authService: AuthService, private notify: NotificationService) {
    this.currentUserId = this.authService.getUserId();
    this.filterForm = this.fb.group({
      type: [''],
      status: [''],
      minAmount: [null],
      maxAmount: [null],
      startDate: [''],
      endDate: ['']
    });
  }

  ngOnInit() {
    this.loadTransactions();
  }

  loadTransactions() {
    // If form has any values, use filter method
    const formVals = this.filterForm.value;
    const isFiltering = Object.values(formVals).some(v => v !== '' && v !== null);

    if (isFiltering) {
      // We append page data if using filter using Spring's pageable
      const payload = { ...formVals, page: this.currentPage, size: this.pageSize };

      // Convert local datetime to ISO so Spring accepts it (remove Z and ms if any)
      if (payload.startDate) {
        const d = new Date(payload.startDate);
        // build yyyy-MM-dd'T'HH:mm:ss format local time
        payload.startDate = d.getFullYear() + '-' +
          String(d.getMonth() + 1).padStart(2, '0') + '-' +
          String(d.getDate()).padStart(2, '0') + 'T' +
          String(d.getHours()).padStart(2, '0') + ':' +
          String(d.getMinutes()).padStart(2, '0') + ':00';
      }
      if (payload.endDate) {
        const d = new Date(payload.endDate);
        payload.endDate = d.getFullYear() + '-' +
          String(d.getMonth() + 1).padStart(2, '0') + '-' +
          String(d.getDate()).padStart(2, '0') + 'T' +
          String(d.getHours()).padStart(2, '0') + ':' +
          String(d.getMinutes()).padStart(2, '0') + ':00';
      }

      this.walletService.filterTransactions(payload).subscribe({
        next: (res) => this.handleResponse(res),
        error: (err) => console.error("Failed to filter", err)
      });
    } else {
      this.walletService.getTransactions(this.currentPage, this.pageSize).subscribe({
        next: (res) => this.handleResponse(res),
        error: (err) => console.error("Failed to fetch all", err)
      });
    }
  }

  handleResponse(res: any) {
    if (res && res.data) {
      const txnList = Array.isArray(res.data) ? res.data : (res.data.content || []);
      this.transactions = txnList;
      this.totalPages = res.data.totalPages || 1;
    }
  }

  applyFilter() {
    this.currentPage = 0;
    this.loadTransactions();
  }

  resetFilter() {
    this.filterForm.reset({ type: '', status: '', startDate: '', endDate: '' });
    this.currentPage = 0;
    this.loadTransactions();
  }

  changePage(pageIndex: number) {
    if (pageIndex >= 0 && pageIndex < this.totalPages) {
      this.currentPage = pageIndex;
      this.loadTransactions();
    }
  }

  isOutgoing(txn: any): boolean {
    const userId = Number(this.currentUserId);
    const senderId = txn.senderId ? Number(txn.senderId) : null;
    const receiverId = txn.receiverId ? Number(txn.receiverId) : null;

    // 1. If both exist and are different, current user's role defines it
    if (senderId && receiverId && senderId !== receiverId) {
      return senderId === userId;
    }

    // 2. If it's a known single-party type
    if (txn.type === 'ADD_FUNDS' || txn.type === 'DEPOSIT' || txn.type === 'REFUND') {
      return false;
    }
    if (txn.type === 'WITHDRAWAL' || txn.type === 'WITHDRAW') {
      return true;
    }

    // 3. Fallback logic for REQUEST type
    if (txn.type === 'REQUEST') {
      return senderId === userId;
    }

    // 4. Fallback to hardcoded list
    const debitTypes = ['LOAN_REPAYMENT', 'INVOICE_PAYMENT', 'PAYMENT', 'SEND', 'TRANSFER'];
    if (debitTypes.includes(txn.type)) {
      return true;
    }

    return false;
  }

  abs(val: number): number {
    return Math.abs(val);
  }

  sanitizeDescription(desc: string): string {
    if (!desc) return 'No description provided';
    if (desc === 'Added via: null' || desc === 'Added via: ' || desc === 'Added via: null ') {
      return 'Added Funds';
    }
    return desc;
  }

  exportToPdf() {
    if (!this.currentUserId) return;
    this.isExporting = true;
    this.walletService.exportTransactionPdf(this.currentUserId).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `revpay_transactions_${new Date().getTime()}.pdf`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        this.isExporting = false;
      },
      error: (err) => {
        console.error("Failed to export PDF", err);
        this.notify.error("Failed to export PDF. Please try again later.");
        this.isExporting = false;
      }
    });
  }

  exportToCsv() {
    if (!this.currentUserId) return;
    this.isExporting = true;
    this.walletService.exportTransactionCsv(this.currentUserId).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `revpay_transactions_${new Date().getTime()}.csv`;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
        this.isExporting = false;
      },
      error: (err) => {
        console.error("Failed to export CSV", err);
        this.notify.error("Failed to export CSV. Please try again later.");
        this.isExporting = false;
      }
    });
  }
}
