import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { WalletService } from '../wallet.service';
import { Transaction } from '../../shared/models/models';
import { NotificationService } from '../../core/notification.service';

@Component({
  standalone: true,
  selector: 'app-requests',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './requests.html',
  styleUrl: './requests.css'
})
export class RequestsComponent implements OnInit {
  incomingRequests: any[] = [];
  outgoingRequests: any[] = [];
  pendingInvoices: any[] = [];
  requestForm: FormGroup;

  constructor(private fb: FormBuilder, private walletService: WalletService, private notify: NotificationService) {
    this.requestForm = this.fb.group({
      targetEmail: ['', [Validators.required, Validators.email]],
      amount: ['', [Validators.required, Validators.min(1)]]
    });
  }

  ngOnInit() {
    this.loadIncoming();
    this.loadOutgoing();
    this.loadInvoices();
  }

  loadInvoices() {
    this.walletService.getPendingInvoices().subscribe(res => {
      if (res.data) {
        this.pendingInvoices = res.data;
      }
    });
  }

  loadIncoming() {
    this.walletService.getIncomingRequests().subscribe(res => {
      if (res.data) {
        this.incomingRequests = Array.isArray(res.data) ? res.data : (res.data.content || []);
      }
    });
  }

  loadOutgoing() {
    this.walletService.getOutgoingRequests().subscribe(res => {
      if (res.data) {
        this.outgoingRequests = Array.isArray(res.data) ? res.data : (res.data.content || []);
      }
    });
  }

  onRequestMoney() {
    if (this.requestForm.valid) {
      this.walletService.requestMoney(this.requestForm.value).subscribe({
        next: () => {
          this.notify.success('Money request sent!');
          this.requestForm.reset();
          this.loadOutgoing();
        },
        error: (err) => this.notify.error('Failed to send request: ' + (err.error?.message || 'Unknown error'))
      });
    }
  }

  acceptRequest(txnId: number) {
    this.notify.promptPin('Confirm accepting this money request').then((res: any) => {
      if (res.isConfirmed && res.value) {
        this.walletService.acceptRequest(txnId, res.value).subscribe({
          next: () => {
            this.notify.success('Request accepted and paid successfully!');
            this.loadIncoming();
          },
          error: (err) => this.notify.error('Failed to accept: ' + (err.error?.message || 'Unknown error'))
        });
      }
    });
  }

  declineRequest(txnId: number) {
    this.notify.confirm('Are you sure you want to decline this request?').then((res: any) => {
      if (res.isConfirmed) {
        this.walletService.declineRequest(txnId).subscribe({
          next: () => {
            this.notify.info('Request declined.');
            this.loadIncoming();
          },
          error: (err) => this.notify.error('Failed to decline: ' + (err.error?.message || 'Unknown error'))
        });
      }
    });
  }

  payInvoice(invoice: any) {
    this.notify.promptPin(`Confirm payment of ₹${invoice.totalAmount} to ${invoice.businessName}`).then((res: any) => {
      if (res.isConfirmed && res.value) {
        this.walletService.payInvoice(invoice.id, res.value).subscribe({
          next: () => {
            this.notify.success('Invoice paid successfully!');
            this.loadInvoices();
          },
          error: (err) => this.notify.error('Failed to pay invoice: ' + (err.error?.message || 'Unknown error'))
        });
      }
    });
  }
}
