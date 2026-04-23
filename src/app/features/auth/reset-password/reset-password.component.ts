import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthLayoutComponent } from '../auth-layout/auth-layout.component';
import { AuthService } from '../../../shared/services/auth.service';
import { getUserFacingHttpErrorMessage } from '../../../shared/utils/http-error-message';
import Swal from 'sweetalert2';

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
            'Un lien de réinitialisation vient de vous être envoyé. Consultez votre boîte mail.';
        },
        error: (error) => {
          this.isLoading = false;
          this.errorMessage = getUserFacingHttpErrorMessage(
            error,
            'Impossible d\'envoyer la demande pour le moment. Réessayez plus tard.'
          );

          if (this.errorMessage.toLowerCase().includes("pas encore actif")) {
            Swal.fire({
              title: 'Compte inactif',
              text: "Votre compte n'est pas encore actif. Voulez-vous recevoir un nouveau lien d'activation ?",
              icon: 'warning',
              showCancelButton: true,
              confirmButtonText: 'Renvoyer le lien',
              cancelButtonText: 'Annuler',
              buttonsStyling: false,
              customClass: {
                confirmButton: 'bg-gradient-to-r from-[#FF6B00] to-[#FF914D] text-white px-6 py-2 rounded-md hover:from-orange-600 hover:to-orange-700 font-medium text-base ml-2',
                cancelButton: 'bg-gray-200 text-gray-800 px-6 py-2 rounded-md hover:bg-gray-300 font-medium text-base mr-2'
              }
            }).then((result) => {
              if (result.isConfirmed) {
                this.resendActivationLink(email);
              }
            });
          }
        }
      });
    } else {
      this.markFormGroupTouched();
    }
  }

  private resendActivationLink(email: string): void {
    this.isLoading = true;
    this.authService.resendActivation(email).subscribe({
      next: (message) => {
        this.isLoading = false;
        this.errorMessage = ''; // On efface l'erreur car le renvoi a réussi
        Swal.fire({
          title: 'Lien renvoyé !',
          text: message || "Un nouveau lien d'activation a été envoyé à votre adresse email.",
          icon: 'success',
          confirmButtonText: 'OK',
          buttonsStyling: false,
          customClass: {
            confirmButton: 'bg-gradient-to-r from-[#FF6B00] to-[#FF914D] text-white px-6 py-2 rounded-md hover:from-orange-600 hover:to-orange-700 font-medium text-base'
          }
        });
      },
      error: (err) => {
        this.isLoading = false;
        Swal.fire({
          title: 'Erreur',
          text: getUserFacingHttpErrorMessage(err, "Impossible de renvoyer le lien d'activation."),
          icon: 'error',
          confirmButtonText: 'OK',
          buttonsStyling: false,
          customClass: {
            confirmButton: 'bg-gradient-to-r from-[#FF6B00] to-[#FF914D] text-white px-6 py-2 rounded-md hover:from-orange-600 hover:to-orange-700 font-medium text-base'
          }
        });
      }
    });
  }

  private markFormGroupTouched(): void {
    Object.keys(this.resetPasswordForm.controls).forEach(key => {
      const control = this.resetPasswordForm.get(key);
      control?.markAsTouched();
    });
  }

}
