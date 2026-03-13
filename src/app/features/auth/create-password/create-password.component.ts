import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthLayoutComponent } from '../auth-layout/auth-layout.component';
import Swal from 'sweetalert2';

// ============================================================
// CE QUE FAIT CE FICHIER
// ============================================================
// Page "Créer un mot de passe" affichée après la vérification OTP
// (flux inscription Commercial / Logistique).
// L'utilisateur définit son mot de passe (min 8 caractères, 1 majuscule),
// confirme, puis est redirigé vers la page de connexion.
// ============================================================

@Component({
  selector: 'app-create-password',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, AuthLayoutComponent],
  templateUrl: './create-password.component.html'
})
export class CreatePasswordComponent {

  // ============================================================
  // LES VARIABLES DE CE COMPOSANT
  // ============================================================

  passwordForm: FormGroup;

  // true = afficher le mot de passe en clair, false = masquer
  showPassword = false;
  showConfirmPassword = false;

  // ============================================================
  // CONSTRUCTEUR
  // ============================================================
  constructor(private fb: FormBuilder, private router: Router) {
    this.passwordForm = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(8), this.passwordValidator]],
      confirmPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });
  }

  // Custom validator for password requirements
  passwordValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value;
    if (!value) {
      return null;
    }

    const hasMinLength = value.length >= 8;
    const hasUppercase = /[A-Z]/.test(value);

    const passwordValid = hasMinLength && hasUppercase;

    return !passwordValid ? { passwordInvalid: true } : null;
  }

  // Vérifie que newPassword et confirmPassword sont identiques
  passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const password = control.get('newPassword');
    const confirmPassword = control.get('confirmPassword');

    if (!password || !confirmPassword) {
      return null;
    }

    return password.value !== confirmPassword.value ? { passwordMismatch: true } : null;
  }

  // ============================================================
  // PROPRIÉTÉS CALCULÉES (pour l'affichage des critères)
  // ============================================================

  get hasMinLength(): boolean {
    const password = this.passwordForm.get('newPassword')?.value || '';
    return password.length >= 8;
  }

  get hasUppercase(): boolean {
    const password = this.passwordForm.get('newPassword')?.value || '';
    return /[A-Z]/.test(password);
  }

  // ============================================================
  // BASKULE AFFICHAGE MOT DE PASSE
  // ============================================================

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  // ============================================================
  // SOUMISSION DU FORMULAIRE
  // ============================================================
  // Si valide → popup de succès puis redirection login
  // Si invalide → marque les champs touchés pour afficher les erreurs
  onSubmit(): void {
    if (this.passwordForm.valid) {
      console.log('Password created successfully:', this.passwordForm.value);
      this.showSuccessMessage();
    } else {
      console.log('Form is invalid');
      Object.keys(this.passwordForm.controls).forEach(key => {
        this.passwordForm.get(key)?.markAsTouched();
      });
    }
  }

  // ============================================================
  // POPUP DE SUCCÈS
  // ============================================================
  // Affiche un message de confirmation puis redirige vers la connexion
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

  // ============================================================
  // RETOUR À LA CONNEXION
  // ============================================================
  goBackToLogin(): void {
    this.router.navigate(['/login']);
  }
}