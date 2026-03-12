import { Component, OnInit } from '@angular/core';
import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { AdminService } from '../admin.service';

@Component({
    standalone: true,
    selector: 'app-admin-ledger',
    imports: [CommonModule],
    providers: [CurrencyPipe, DatePipe],
    templateUrl: './admin-ledger.component.html',
    styleUrls: ['./admin-ledger.component.css']
})
export class AdminLedgerComponent implements OnInit {
    transactions: any[] = [];
    loading = false;
    error = '';

    // Pagination
    currentPage = 0;
    pageSize = 15;
    totalPages = 0;
    totalElements = 0;

    userMap: Map<number, any> = new Map();

    constructor(private adminService: AdminService) { }

    ngOnInit(): void {
        this.loadUsersAndTransactions();
    }

    loadUsersAndTransactions() {
        this.loading = true;
        // Fetch users first for name mapping
        this.adminService.getAllUsers(0, 1000).subscribe({
            next: (res: any) => {
                if (res.success && res.data) {
                    res.data.content.forEach((u: any) => {
                        this.userMap.set(u.userId, u);
                    });
                }
                this.loadTransactions();
            },
            error: () => this.loadTransactions() // proceed even if user fetch fails
        });
    }

    loadTransactions(page: number = 0) {
        this.loading = true;
        this.error = '';
        this.currentPage = page;

        this.adminService.getAllTransactions(this.currentPage, this.pageSize).subscribe({
            next: (res: any) => {
                if (res.success && res.data) {
                    this.transactions = res.data.content.map((tx: any) => {
                        // Map internal IDs to names using the user map
                        if (tx.senderId && this.userMap.has(tx.senderId)) {
                            const sender = this.userMap.get(tx.senderId);
                            tx.senderName = sender.fullName;
                            tx.senderEmail = sender.email;
                        } else if (tx.senderId === 1) {
                            tx.senderName = 'System Admin';
                            tx.senderEmail = 'admin@revpay.com';
                        }

                        if (tx.receiverId && this.userMap.has(tx.receiverId)) {
                            const receiver = this.userMap.get(tx.receiverId);
                            tx.receiverName = receiver.fullName;
                            tx.receiverEmail = receiver.email;
                        } else if (tx.receiverId === 1) {
                            tx.receiverName = 'System Admin';
                            tx.receiverEmail = 'admin@revpay.com';
                        }

                        return tx;
                    });
                    this.totalPages = res.data.totalPages;
                    this.totalElements = res.data.totalElements;
                } else {
                    this.error = res.message || 'Failed to load transactions';
                }
                this.loading = false;
            },
            error: (err: any) => {
                this.error = err.error?.message || 'Error communicating with server';
                this.loading = false;
            }
        });
    }

    nextPage() {
        if (this.currentPage < this.totalPages - 1) {
            this.loadTransactions(this.currentPage + 1);
        }
    }

    prevPage() {
        if (this.currentPage > 0) {
            this.loadTransactions(this.currentPage - 1);
        }
    }

    getTypeBadgeClass(type: string): string {
        switch (type) {
            case 'TRANSFER': return 'badge bg-primary';
            case 'ADD_FUNDS': return 'badge bg-success';
            case 'WITHDRAWAL': return 'badge bg-warning text-dark';
            case 'PAYMENT': return 'badge bg-info text-dark';
            default: return 'badge bg-secondary';
        }
    }

    getStatusBadgeClass(status: string): string {
        switch (status) {
            case 'COMPLETED': return 'text-success';
            case 'PENDING': return 'text-warning';
            case 'FAILED': return 'text-danger';
            default: return 'text-muted';
        }
    }
}
