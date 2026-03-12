import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { InvoiceService, InvoiceDTO } from '../invoice.service';
import { BusinessProfileService } from '../business-profile.service';
import { NotificationService } from '../../core/notification.service';

@Component({
  standalone: true,
  selector: 'app-business-invoices',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './business-invoices.component.html',
  styleUrl: './business-invoices.component.css'
})
export class BusinessInvoicesComponent implements OnInit {
  isVerified = false;
  profileId: number = 0;

  showCreateForm = false;
  invoiceForm: FormGroup;

  invoices: InvoiceDTO[] = [];
  currentPage = 0;
  totalPages = 0;

  constructor(
    private invoiceService: InvoiceService,
    private profileService: BusinessProfileService,
    private fb: FormBuilder,
    private notify: NotificationService
  ) {
    this.invoiceForm = this.fb.group({
      customerName: ['', Validators.required],
      customerEmail: ['', [Validators.required, Validators.email]],
      totalAmount: ['', [Validators.required, Validators.min(0.01)]],
      dueDate: ['', Validators.required]
    });
  }

  ngOnInit() {
    this.profileService.getMyProfile().subscribe({
      next: (res) => {
        this.isVerified = res.data.isVerified;
        this.profileId = res.data.profileId;
        if (this.isVerified) {
          this.loadInvoices(0);
        }
      },
      error: (err) => console.error("Failed to load business profile", err)
    });
  }

  loadInvoices(page: number) {
    this.invoiceService.getInvoices(this.profileId, page, 10).subscribe({
      next: (res) => {
        // Backend returns List<InvoiceDto>, not a Page object
        this.invoices = res.data || [];
        // Optional: calculate totalPages if needed, but for now just show all
        this.totalPages = 1;
        this.currentPage = 0;
      },
      error: (err) => console.error("Failed to load invoices", err)
    });
  }

  createInvoice() {
    if (this.invoiceForm.valid) {
      const formVals = this.invoiceForm.value;
      const payload = {
        customerName: formVals.customerName,
        customerEmail: formVals.customerEmail,
        totalAmount: formVals.totalAmount,
        dueDate: formVals.dueDate,
        items: [
          {
            description: "General Service",
            quantity: 1,
            unitPrice: formVals.totalAmount,
            total: formVals.totalAmount
          }
        ]
      };
      this.invoiceService.createInvoice(payload).subscribe({
        next: (res) => {
          this.showCreateForm = false;
          this.invoiceForm.reset();
          this.loadInvoices(this.currentPage);
          this.notify.success("Invoice created successfully!");
        },
        error: (err) => {
          console.error(err);
          this.notify.error("Failed to create invoice: " + (err.error?.message || "Unknown error"));
        }
      });
    }
  }

  sendInvoice(id: number) {
    this.notify.confirm("Are you sure you want to send this invoice to the customer?").then((res: any) => {
      if (res.isConfirmed) {
        this.invoiceService.sendInvoice(id).subscribe({
          next: () => {
            this.loadInvoices(this.currentPage);
            this.notify.success("Invoice sent successfully!");
          },
          error: (err) => this.notify.error("Failed to send invoice.")
        });
      }
    });
  }

  markPaid(id: number) {
    this.notify.confirm("Mark this invoice as manually paid?").then((res: any) => {
      if (res.isConfirmed) {
        this.invoiceService.markPaid(id).subscribe({
          next: () => {
            this.loadInvoices(this.currentPage);
            this.notify.success("Invoice marked as paid!");
          },
          error: (err) => this.notify.error("Failed to mark invoice as paid.")
        });
      }
    });
  }
}
