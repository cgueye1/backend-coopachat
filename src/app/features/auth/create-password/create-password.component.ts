import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthLayoutComponent } from '../auth-layout/auth-layout.component';
import { AuthService } from '../../../shared/services/auth.service';
import { getUserFacingHttpErrorMessage } from '../../../shared/utils/http-error-message';
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
    private authService: AuthService,
    @Inject(PLATFORM_ID) private platformId: object
  ) {
    this.passwordForm = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(8), this.passwordValidator]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });
  }

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');//Token de réinitialisation 
    this.resetToken = token?.trim() ? token.trim() : null;
    this.isResetEmailFlow = !!this.resetToken; //"Avons-nous un token ?" (Oui/Non). Si c'est Oui, on sait qu'on va devoir valider ce token.
    if (isPlatformBrowser(this.platformId)) {
      // Récupère l'email : soit via l'URL (Activation), soit via le stockage local (Inscription classique)      
        this.route.snapshot.queryParamMap.get('email') || 
        localStorage.getItem('verificationEmail') ||//Email stocké après inscription
        sessionStorage.getItem('email') ||//Email stocké après inscription
        null;
    } else {
      this.registrationEmail = null;
    }

    this.syncPageErrorState();
  }

  /** Message d’erreur uniquement si ni token reset ni email d’inscription  */
  private syncPageErrorState(): void {
    if (!this.isResetEmailFlow && !this.registrationEmail) {
      this.pageError =
        'Lien invalide ou session expirée. Repassez par « Activer mon compte » ou utilisez le lien reçu par e-mail.';
    } else {
      this.pageError = '';
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

      // --- Flux « mot de passe oublié » ou « Activation directe via lien » ---
      if (this.resetToken) {
        // Tente de récupérer l'email spécifiquement dans l'URL pour différencier les deux flux
        const emailFromUrl = this.route.snapshot.queryParamMap.get('email');
        
        if (emailFromUrl) {
          // --- CAS 1 : ACTIVATION (Token + Email présents) ---
          // 1. On vérifie d'abord la validité du code d'activation
          this.authService.verifyActivationCode(emailFromUrl, this.resetToken).subscribe({
            next: () => {
              // 2. Si le code est valide, on définit le mot de passe et on active le compte
              this.authService.setPassword(emailFromUrl, newPassword, confirmPassword).subscribe({
                next: () => {
                  this.isSubmitting = false;
                  this.showSuccessMessage();
                },
                error: (error) => this.handleSubmitError(error)
              });
            },
            error: (error) => this.handleSubmitError(error)
          });
          return;
        }

        // --- CAS 2 : RÉINITIALISATION (Token seul présent) ---
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
        this.pageError = 'Email introuvable. Repassez par « Activer mon compte » ou la vérification du code.';
        return;
      }

      //Dans ce cas on appelle la méthode setPassword du service authService
      this.authService.setPassword(email, newPassword, confirmPassword).subscribe({
        next: () => {
          this.isSubmitting = false;
          if (isPlatformBrowser(this.platformId)) {
            localStorage.removeItem('verificationEmail');
          }
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
    const backendMessage = getUserFacingHttpErrorMessage(
      error,
      'Impossible d\'enregistrer le mot de passe. Vérifiez les critères ou réessayez.'
    );
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
