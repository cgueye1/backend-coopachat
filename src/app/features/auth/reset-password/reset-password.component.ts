import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthLayoutComponent } from '../auth-layout/auth-layout.component';
import { AuthService } from '../../../shared/services/auth.service';
import { getUserFacingHttpErrorMessage } from '../../../shared/utils/http-error-message';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AuthLayoutComponent, RouterLink],
  templateUrl: './reset-password.component.html'
})
export class ResetPasswordComponent implements OnInit {
  resetPasswordForm!: FormGroup;
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  constructor(
    private formBuilder: FormBuilder,
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    this.initializeForm();
  }

  private initializeForm(): void {
    this.resetPasswordForm = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  onSubmit(): void {
    if (this.resetPasswordForm.valid) {
      this.isLoading = true;
      this.errorMessage = '';
      this.successMessage = '';

      const email = this.resetPasswordForm.get('email')?.value?.trim();

      this.authService.forgotPassword(email).subscribe({
        next: (message) => {
          this.isLoading = false;
          this.successMessage =
            message ||
            'Si cette adresse est enregistrée, un lien de réinitialisation vient de vous être envoyé. Consultez votre boîte mail.';
        },
        error: (error) => {
          this.isLoading = false;
          this.errorMessage = getUserFacingHttpErrorMessage(
            error,
            'Impossible d\'envoyer la demande pour le moment. Réessayez plus tard.'
          );
        }
      });
    } else {
      this.markFormGroupTouched();
    }
  }

  private markFormGroupTouched(): void {
    Object.keys(this.resetPasswordForm.controls).forEach(key => {
      const control = this.resetPasswordForm.get(key);
      control?.markAsTouched();
    });
  }

}
