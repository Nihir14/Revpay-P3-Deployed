import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BusinessProfileService } from '../business-profile.service';
import { LoanService } from '../loan.service';
import { BusinessAnalyticsService, BusinessSummaryDTO } from '../analytics.service';
import { AuthService } from '../../auth/auth.service';

@Component({
  standalone: true,
  selector: 'app-business-dashboard',
  imports: [CommonModule],
  templateUrl: './business-dashboard.component.html',
  styleUrl: './business-dashboard.component.css'
})
export class BusinessDashboardComponent implements OnInit {
  isVerified = false;
  profileId = 0;

  outstandingDebt = 0;
  pendingEmis = 0;
  latestLoanStatus = '';
  summary: BusinessSummaryDTO | null = null;
  userId: number | null = null;

  constructor(
    private profileService: BusinessProfileService,
    private loanService: LoanService,
    private analyticsService: BusinessAnalyticsService,
    private authService: AuthService
  ) { }

  ngOnInit() {
    this.userId = this.authService.getUserId();

    this.profileService.getMyProfile().subscribe({
      next: (res) => {
        this.isVerified = res.data.isVerified;
        this.profileId = res.data.profileId;
        if (this.isVerified) {
          this.loadMetrics();
        }
      },
      error: (err) => console.error('Failed to load profile', err)
    });
  }

  loadMetrics() {
    // Load Loan metrics
    this.loanService.getAnalytics().subscribe({
      next: (res) => {
        if (res.data) {
          this.outstandingDebt = res.data.totalOutstanding || 0;
          this.pendingEmis = res.data.totalPending || 0;
        }
      },
      error: (err) => console.error('Failed to load loan analytics', err)
    });

    // Load Latest Loan for status
    this.loanService.getMyLoans().subscribe({
      next: (res) => {
        if (res.data && Array.isArray(res.data) && res.data.length > 0) {
          // Sort by loanId DESC to get the latest application at the top
          const sortedLoans = [...res.data].sort((a, b) => b.loanId - a.loanId);
          this.latestLoanStatus = sortedLoans[0].status;
        }
      },
      error: (err) => console.error('Failed to load my loans', err)
    });

    // Load Business Analytics Summary
    if (this.userId) {
      this.analyticsService.getSummary(this.userId).subscribe({
        next: (res) => {
          this.summary = res.data;
        },
        error: (err) => console.error('Failed to load summary', err)
      });
    }
  }
}

