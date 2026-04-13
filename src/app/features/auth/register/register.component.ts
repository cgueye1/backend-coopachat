import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AuthLayoutComponent } from '../auth-layout/auth-layout.component';
import { AuthService } from '../../../shared/services/auth.service';
import { getUserFacingHttpErrorMessage } from '../../../shared/utils/http-error-message';
import Swal from 'sweetalert2';

/**
 * Activation de compte (Commercial / Responsable logistique) : l’admin crée l’utilisateur,
 * un code est envoyé par e-mail ; cette page ne demande que l’e-mail pour renvoyer un code
 * et enchaîne vers OTP puis création de mot de passe.
 */
@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, AuthLayoutComponent, RouterLink],
  templateUrl: './register.component.html'
})
export class RegisterComponent {
  form: FormGroup;
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private authService: AuthService
  ) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  onSubmit(event?: Event): void {
    if (event) event.preventDefault();
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const email = (this.form.value.email as string).trim().toLowerCase();
    this.isLoading = true;
    this.authService.sendActivationCode(email).subscribe({
      next: () => {
        this.isLoading = false;
        if (typeof localStorage !== 'undefined') {
          localStorage.setItem('verificationEmail', email);
        }
        Swal.fire({
          title: 'Code envoyé',
          text: 'Un code de vérification vient de vous être envoyé par e-mail.',
          iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
          showConfirmButton: false,
          timer: 3500,
          buttonsStyling: false,
          customClass: {
            popup: 'rounded-3xl p-6',
            title: 'text-xl font-medium text-gray-900',
            htmlContainer: 'text-base text-gray-600',
            icon: 'border-none'
          },
          backdrop: 'rgba(0,0,0,0.2)',
          width: '580px'
        }).then(() => {
          void this.router.navigate(['/otp-verification'], {
            queryParams: { email, flow: 'activation' }
          });
        });
      },
      error: (error) => {
        this.isLoading = false;
        const message = getUserFacingHttpErrorMessage(
          error,
          'Impossible d’envoyer le code. Vérifiez l’adresse ou réessayez plus tard.'
        );
        void Swal.fire({ icon: 'error', title: 'Erreur', text: message });
      }
    });
  }
}
