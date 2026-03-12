import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AdminService } from '../admin.service';
import { NotificationService } from '../../core/notification.service';

@Component({
    standalone: true,
    selector: 'app-admin-businesses',
    imports: [CommonModule],
    templateUrl: './admin-businesses.component.html',
    styleUrls: ['./admin-businesses.component.css']
})
export class AdminBusinessesComponent implements OnInit {
    businesses: any[] = [];
    loading = false;
    error = '';

    // Pagination
    currentPage = 0;
    pageSize = 10;
    totalPages = 0;
    totalElements = 0;

    constructor(private adminService: AdminService, private notify: NotificationService) { }

    ngOnInit(): void {
        this.loadBusinesses();
    }

    loadBusinesses(page: number = 0) {
        this.loading = true;
        this.error = '';
        this.currentPage = page;

        this.adminService.getAllBusinesses(this.currentPage, this.pageSize).subscribe({
            next: (res) => {
                if (res.success && res.data) {
                    this.businesses = res.data.content;
                    this.totalPages = res.data.totalPages;
                    this.totalElements = res.data.totalElements;
                } else {
                    this.error = res.message || 'Failed to load businesses';
                }
                this.loading = false;
            },
            error: (err) => {
                this.error = err.error?.message || 'Error communicating with server';
                this.loading = false;
            }
        });
    }

    verifyBusiness(business: any) {
        this.notify.confirm(`Approve business profile for ${business.businessName}?`).then((result: any) => {
            if (result.isConfirmed) {
                business.processing = true;
                this.adminService.verifyBusiness(business.profileId).subscribe({
                    next: (res) => {
                        business.processing = false;
                        if (res.success) {
                            business.verified = true;
                            this.notify.success('Business verified successfully.');
                        } else {
                            this.notify.error('Verification failed: ' + res.message);
                        }
                    },
                    error: (err) => {
                        business.processing = false;
                        this.notify.error(err.error?.message || 'Server error');
                    }
                });
            }
        });
    }

    suspendBusiness(business: any) {
        this.notify.confirm(`Suspend business profile for ${business.businessName}?`, 'Confirm Suspension').then((result: any) => {
            if (result.isConfirmed) {
                business.processing = true;
                this.adminService.suspendBusiness(business.profileId).subscribe({
                    next: (res) => {
                        business.processing = false;
                        if (res.success) {
                            business.verified = false;
                            this.notify.success('Business suspended successfully.');
                        } else {
                            this.notify.error('Suspended failed: ' + res.message);
                        }
                    },
                    error: (err) => {
                        business.processing = false;
                        this.notify.error(err.error?.message || 'Server error');
                    }
                });
            }
        });
    }

    nextPage() {
        if (this.currentPage < this.totalPages - 1) {
            this.loadBusinesses(this.currentPage + 1);
        }
    }

    prevPage() {
        if (this.currentPage > 0) {
            this.loadBusinesses(this.currentPage - 1);
        }
    }
}
