import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { AuthService } from '../auth.service';
import { NotificationService } from '../../core/notification.service';

@Component({
  standalone: true,
  selector: 'app-profile',
  imports: [ReactiveFormsModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css'
})
export class ProfileComponent {
  passwordForm: FormGroup;
  pinForm: FormGroup;

  constructor(private fb: FormBuilder, private authService: AuthService, private notify: NotificationService) {
    this.passwordForm = this.fb.group({
      oldPassword: ['', Validators.required],
      newPassword: ['', [Validators.required, Validators.minLength(8)]]
    });

    this.pinForm = this.fb.group({
      oldPin: ['', Validators.required],
      newPin: ['', [Validators.required, Validators.pattern(/^[0-9]{4,6}$/)]]
    });
  }

  onUpdatePassword() {
    if (this.passwordForm.valid) {
      this.authService.updatePassword(this.passwordForm.value).subscribe({
        next: () => {
          this.notify.success('Password updated successfully!');
          this.passwordForm.reset();
        },
        error: (err) => this.notify.error('Failed to update password: ' + (err.error?.message || 'Unknown error'))
      });
    }
  }

  onUpdatePin() {
    if (this.pinForm.valid) {
      this.authService.updatePin(this.pinForm.value).subscribe({
        next: () => {
          this.notify.success('Transaction PIN updated successfully!');
          this.pinForm.reset();
        },
        error: (err) => this.notify.error('Failed to update PIN: ' + (err.error?.message || 'Unknown error'))
      });
    }
  }
}
