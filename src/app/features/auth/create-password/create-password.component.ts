import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthLayoutComponent } from '../auth-layout/auth-layout.component';
import { AuthService } from '../../../shared/services/auth.service';
import Swal from 'sweetalert2';

/** Même règle que le backend (SetPassword / ResetPassword). */
const PASSWORD_PATTERN = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;

@Component({
  selector: 'app-create-password',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, AuthLayoutComponent],
  templateUrl: './create-password.component.html'
})
export class CreatePasswordComponent implements OnInit {

  passwordForm: FormGroup;
  showPassword = false;
  showConfirmPassword = false;

  /** Présent dans l'URL après clic sur le lien « mot de passe oublié » (?token=...) */
  private resetToken: string | null = null;
  /** Flux email : réinitialisation par token */
  isResetEmailFlow = false;
  /** Email stocké après inscription, pour POST /set-password */
  private registrationEmail: string | null = null;

  isSubmitting = false;
  pageError = '';

  get canSubmit(): boolean {
    return this.isResetEmailFlow || !!this.registrationEmail;
  }

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute,
    private authService: AuthService
  ) {
    this.passwordForm = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(8), this.passwordValidator]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    this.resetToken = token?.trim() ? token.trim() : null;
    this.isResetEmailFlow = !!this.resetToken;

    this.registrationEmail =
      localStorage.getItem('verificationEmail') ||
      sessionStorage.getItem('email') ||
      null;

    if (!this.isResetEmailFlow && !this.registrationEmail) {
      this.pageError =
        'Lien invalide ou session expirée. Utilisez le lien reçu par email ou repassez par la connexion / l\'inscription.';
    }
  }

  passwordValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value;
    if (!value) {
      return null;
    }
    return PASSWORD_PATTERN.test(value) ? null : { passwordInvalid: true };
  }

  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('newPassword');
    const confirmPassword = control.get('confirmPassword');
    if (!password || !confirmPassword) {
      return null;
    }
    return password.value !== confirmPassword.value ? { passwordMismatch: true } : null;
  }

  get hasMinLength(): boolean {
    const password = this.passwordForm.get('newPassword')?.value || '';
    return password.length >= 8;
  }

  get hasLowercase(): boolean {
    return /[a-z]/.test(this.passwordForm.get('newPassword')?.value || '');
  }

  get hasUppercase(): boolean {
    return /[A-Z]/.test(this.passwordForm.get('newPassword')?.value || '');
  }

  get hasDigit(): boolean {
    return /\d/.test(this.passwordForm.get('newPassword')?.value || '');
  }

  get hasSpecial(): boolean {
    return /[@$!%*?&]/.test(this.passwordForm.get('newPassword')?.value || '');
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  /**
   * Soumission du formulaire : deux flux possibles selon l’origine de l’utilisateur.
   */
  onSubmit(): void {
    // Aucun contexte valide (ni lien email avec token, ni email d’inscription en mémoire) → ne rien faire.
    if (!this.isResetEmailFlow && !this.registrationEmail) {
      return;
    }

    // si le formulaire est valide
    if (this.passwordForm.valid) {
      const newPassword = this.passwordForm.get('newPassword')?.value;
      const confirmPassword = this.passwordForm.get('confirmPassword')?.value;

      // Désactive le bouton et évite les doubles envois pendant l’appel HTTP.
      this.isSubmitting = true;

      // --- Flux « mot de passe oublié » : URL du type /create-password?token=... ---
      // si on a un token et que le formulaire est valide
      if (this.isResetEmailFlow && this.resetToken) {
        // on appelle la méthode resetPassword du service authService
        this.authService.resetPassword(this.resetToken, newPassword, confirmPassword).subscribe({
          next: () => {
            this.isSubmitting = false;
            this.showSuccessMessage();
          },
          error: (error) => this.handleSubmitError(error)
        });
        return;
      }

      // --- Flux inscription : après OTP, l’email est dans localStorage / sessionStorage ---
      const email = this.registrationEmail;
      if (!email) {
        this.isSubmitting = false;
        this.pageError = 'Email introuvable. Recommencez l\'inscription ou utilisez le lien reçu par email.';
        return;
      }

      //Dans ce cas on appelle la méthode setPassword du service authService
      this.authService.setPassword(email, newPassword, confirmPassword).subscribe({
        next: () => {
          this.isSubmitting = false;
          localStorage.removeItem('verificationEmail');
          this.showSuccessMessage();
        },
        error: (error) => this.handleSubmitError(error)
      });
    } else {
      // Formulaire invalide : marquer les champs pour afficher les messages sous les inputs.
      Object.keys(this.passwordForm.controls).forEach(key => {
        this.passwordForm.get(key)?.markAsTouched();
      });
    }
  }

  private handleSubmitError(error: unknown): void {
    this.isSubmitting = false;
    const err = error as { error?: string | { message?: string }; message?: string };
    const backendMessage =
      (typeof err?.error === 'string' ? err.error : null) ||
      (err?.error as { message?: string })?.message ||
      err?.message ||
      'Impossible d\'enregistrer le mot de passe. Vérifiez les critères ou réessayez.';
    Swal.fire({
      title: 'Erreur',
      text: backendMessage,
      icon: 'error',
      confirmButtonText: 'OK'
    });
  }

  showSuccessMessage(): void {
    Swal.fire({
      title: 'Mot de passe défini',
      html: '<p style="color: #231F20; font-size: 18px; font-weight: 400; margin: 0;">Votre mot de passe a été enregistré avec succès. Vous pouvez maintenant vous connecter.</p>',
      iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
      showConfirmButton: true,
      confirmButtonText: 'Cliquez ici pour vous connecter',
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-xl p-6',
        title: 'text-2xl font-semibold text-[#231F20]',
        icon: 'border-none',
        confirmButton: 'w-full bg-gradient-to-r from-[#FF6B00] to-[#FF914D] text-white py-3 px-4 rounded-md hover:from-orange-600 hover:to-orange-700 font-medium text-base transition-all duration-200 transform hover:scale-[1.02] mt-4'
      },
      backdrop: 'rgba(0,0,0,0.2)',
      width: '580px',
      showClass: {
        popup: 'animate__animated animate__fadeIn animate__faster'
      },
      hideClass: {
        popup: 'animate__animated animate__fadeOut animate__faster'
      }
    }).then((result) => {
      if (result.isConfirmed) {
        this.router.navigate(['/login']);
      }
    });
  }

  goBackToLogin(): void {
    this.router.navigate(['/login']);
  }
}
