import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { WalletService } from '../wallet.service';
import { AuthService } from '../../auth/auth.service';
import { NotificationService } from '../../core/notification.service';

@Component({
  standalone: true,
  selector: 'app-dashboard',
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class DashboardComponent implements OnInit {
  balance: number = 0;
  transactions: any[] = [];
  cards: any[] = [];

  showAddFunds: boolean = false;
  addFundsForm: FormGroup;
  currentUserId: number | null = null;

  constructor(private fb: FormBuilder, private walletService: WalletService, private authService: AuthService, private notify: NotificationService) {
    this.currentUserId = this.authService.getUserId();
    this.addFundsForm = this.fb.group({
      amount: ['', [Validators.required, Validators.min(1)]],
      cardId: ['', Validators.required],
      description: ['']
    });
  }

  ngOnInit() {
    this.loadData();
  }

  loadData() {
    this.walletService.getBalance().subscribe(res => {
      if (res && res.data) {
        // Correctly handle WalletDto or direct number
        if (typeof res.data === 'object' && 'balance' in res.data) {
          this.balance = (res.data as any).balance;
        } else {
          this.balance = Number(res.data) || 0;
        }
      }
    });

    this.walletService.getTransactions(0, 50).subscribe(res => {
      if (res.data) {
        const txnList = Array.isArray(res.data) ? res.data : (res.data.content || []);
        this.transactions = txnList.slice(0, 10);
      }
    });

    this.walletService.getCards(0, 50).subscribe(res => {
      if (res.data) {
        this.cards = Array.isArray(res.data) ? res.data : (res.data.content || []);
      }
    });
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

    // 3. Fallback to hardcoded list if IDs are missing/same
    const debitTypes = ['LOAN_REPAYMENT', 'INVOICE_PAYMENT', 'PAYMENT', 'SEND', 'TRANSFER'];
    if (debitTypes.includes(txn.type)) {
      return true;
    }

    return false;
  }

  sanitizeDescription(desc: string): string {
    return desc || 'No description';
  }

  abs(val: number): number {
    return Math.abs(val);
  }

  onAddFunds() {
    if (this.addFundsForm.valid) {
      this.walletService.addFunds(this.addFundsForm.value).subscribe({
        next: () => {
          this.notify.success('Funds added successfully!');
          this.addFundsForm.reset({ cardId: '' });
          this.showAddFunds = false;
          this.loadData();
        },
        error: (err) => this.notify.error('Failed to add funds: ' + (err.error?.message || 'Unknown error'))
      });
    }
  }
}
