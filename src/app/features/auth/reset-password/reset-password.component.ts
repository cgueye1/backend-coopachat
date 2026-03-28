import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthLayoutComponent } from '../auth-layout/auth-layout.component';
import { AuthService } from '../../../shared/services/auth.service';

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
    private router: Router,
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
            'Si cette adresse est enregistrée, un lien de réinitialisation vient de vous être envoyé.';
          setTimeout(() => this.router.navigate(['/login']), 3500);
        },
        error: (error) => {
          this.isLoading = false;
          const backendMessage =
            (typeof error?.error === 'string' ? error.error : null) ||
            error?.error?.message ||
            error?.message;
          if (backendMessage) {
            this.errorMessage = backendMessage;
          } else if (error.status === 0) {
            this.errorMessage =
              'Serveur inaccessible. Vérifiez la connexion et l\'URL de l\'API dans environment.';
          } else {
            this.errorMessage = 'Impossible d\'envoyer la demande pour le moment. Réessayez plus tard.';
          }
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
