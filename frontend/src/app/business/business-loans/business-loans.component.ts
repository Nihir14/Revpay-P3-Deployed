import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { LoanService, LoanDTO, LoanAnalyticsDTO } from '../loan.service';
import { BusinessProfileService } from '../business-profile.service';
import { NotificationService } from '../../core/notification.service';

@Component({
  standalone: true,
  selector: 'app-business-loans',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './business-loans.component.html',
  styleUrl: './business-loans.component.css'
})
export class BusinessLoansComponent implements OnInit {
  isVerified = false;
  showApplyForm = false;
  loanForm: FormGroup;

  analytics: LoanAnalyticsDTO | null = null;
  loans: LoanDTO[] = [];

  constructor(
    private loanService: LoanService,
    private profileService: BusinessProfileService,
    private fb: FormBuilder,
    private notify: NotificationService
  ) {
    this.loanForm = this.fb.group({
      amount: ['', [Validators.required, Validators.min(1000)]],
      tenureMonths: ['', [Validators.required, Validators.min(3), Validators.max(60)]],
      purpose: ['', Validators.required],
      loanType: ['WORKING_CAPITAL', Validators.required]
    });
  }

  ngOnInit() {
    this.profileService.getMyProfile().subscribe({
      next: (res) => {
        this.isVerified = res.data.isVerified;
        if (this.isVerified) {
          this.loadDashboardData();
        }
      },
      error: (err) => console.error(err)
    });
  }

  loadDashboardData() {
    // Load Analytics
    this.loanService.getAnalytics().subscribe({
      next: (res) => {
        if (res && res.data) {
          this.analytics = res.data;
        }
      },
      error: (err) => {
        console.warn("Analytics not available yet", err);
        // Default values to prevent UI crash
        this.analytics = { totalOutstanding: 0, totalPaid: 0, totalPending: 0 };
      }
    });

    // Load Loans
    this.loanService.getMyLoans(0, 50).subscribe({
      next: (res) => {
        // Backend returns List<LoanResponseDto>, not a Page object
        this.loans = res.data || [];
        // Fetch EMI schedules for active/approved loans
        this.loans.forEach(loan => {
          if (['APPROVED', 'ACTIVE'].includes(loan.status)) {
            this.loanService.getEmiSchedule(loan.loanId).subscribe({
              next: (emiRes) => {
                loan.emiSchedule = emiRes.data || [];
              },
              error: () => loan.emiSchedule = []
            });
          }
        });
      },
      error: (err) => console.error("Failed to load loans", err)
    });
  }

  applyForLoan() {
    if (this.loanForm.valid) {
      this.notify.confirm('Are you sure you want to apply for this loan?').then((result: any) => {
        if (result.isConfirmed) {
          const payload = { ...this.loanForm.value, idempotencyKey: Date.now().toString() };
          this.loanService.applyForLoan(payload).subscribe({
            next: () => {
              this.showApplyForm = false;
              this.loanForm.reset();
              this.loadDashboardData();
              this.notify.success("Loan application submitted successfully and is pending admin approval!");
            },
            error: (err) => this.notify.error("Failed to apply for loan: " + (err.error?.message || "Error"))
          });
        }
      });
    }
  }

  repayEmi(loanId: number, emiAmount: number) {
    this.notify.confirm(`Pay the scheduled Monthly EMI of ₹${emiAmount.toFixed(2)} from your wallet balance?`, 'Confirm EMI Payment').then((resConfirm: any) => {
      if (resConfirm.isConfirmed) {
        this.notify.promptPin("Enter your transaction PIN to authorize Monthly EMI payment:").then((resPin: any) => {
          if (resPin.isConfirmed && resPin.value) {
            this.loanService.repayLoan(loanId, emiAmount, resPin.value, false).subscribe({
              next: () => {
                this.loadDashboardData();
                this.notify.success("Monthly EMI paid successfully!");
              },
              error: (err) => this.notify.error(err.error?.message || "Insufficient balance or error")
            });
          }
        });
      }
    });
  }

  precloseLoan(loanId: number, remainingAmount: number) {
    this.notify.confirm(`Are you sure you want to preclose this loan? This will deduct the remaining balance of ₹${remainingAmount.toFixed(2)} from your wallet.`, 'Loan Preclosure').then((resConfirm: any) => {
      if (resConfirm.isConfirmed) {
        this.notify.promptPin("Authorize Loan Preclosure with your transaction PIN:").then((resPin: any) => {
          if (resPin.isConfirmed && resPin.value) {
            this.loanService.repayLoan(loanId, remainingAmount, resPin.value, true).subscribe({
              next: () => {
                this.loadDashboardData();
                this.notify.success("Loan preclosed successfully!");
              },
              error: (err) => this.notify.error(err.error?.message || "Insufficient balance or error")
            });
          }
        });
      }
    });
  }
}
