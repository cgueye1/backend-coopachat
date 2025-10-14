import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './reset-password.component.html'
})
export class ResetPasswordComponent implements OnInit {
  resetPasswordForm!: FormGroup;
  showNewPassword = false;
  showConfirmPassword = false;
  isLoading = false;

  // Paramètres de sécurité
  verificationToken: string = '';
  contactMethod: 'email' | 'phone' = 'email';
  contact: string = '';

  // Critères de validation du mot de passe
  passwordCriteria = {
    minLength: false,
    hasUppercase: false
  };

  constructor(
    private formBuilder: FormBuilder,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.initializeComponent();
    this.initializeForm();
  }

  private initializeComponent(): void {
    // Récupérer les paramètres de la route (token, méthode, contact)
    this.route.queryParams.subscribe(params => {
      this.verificationToken = params['token'] || '';
      this.contactMethod = params['method'] || 'email';
      this.contact = params['contact'] || '';

      // Vérifier que le token est présent
      if (!this.verificationToken) {
        console.warn('Token de vérification manquant');
        this.router.navigate(['/forgot-password']);
      }
    });
  }

  private initializeForm(): void {
    this.resetPasswordForm = this.formBuilder.group({
      newPassword: ['', [
        Validators.required,
        Validators.minLength(8),
        this.uppercaseValidator
      ]],
      confirmPassword: ['', [
        Validators.required
      ]]
    }, {
      validators: this.passwordMatchValidator
    });

    // Écouter les changements du nouveau mot de passe pour la validation en temps réel
    this.resetPasswordForm.get('newPassword')?.valueChanges.subscribe(value => {
      this.updatePasswordCriteria(value || '');
    });
  }

  // Validator personnalisé pour vérifier la présence d'une majuscule
  private uppercaseValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value;
    if (!value) return null;
    
    const hasUppercase = /[A-Z]/.test(value);
    return hasUppercase ? null : { pattern: true };
  }

  // Validator personnalisé pour vérifier que les mots de passe correspondent
  private passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
    const newPassword = group.get('newPassword')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;

    if (!newPassword || !confirmPassword) return null;

    return newPassword === confirmPassword ? null : { passwordMismatch: true };
  }

  // Mettre à jour les critères de validation en temps réel
  private updatePasswordCriteria(password: string): void {
    this.passwordCriteria = {
      minLength: password.length >= 8,
      hasUppercase: /[A-Z]/.test(password)
    };
  }

  toggleNewPasswordVisibility(): void {
    this.showNewPassword = !this.showNewPassword;
  }

  toggleConfirmPasswordVisibility(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  onSubmit(): void {
    if (this.resetPasswordForm.valid) {
      this.isLoading = true;

      const resetData = {
        token: this.verificationToken,
        newPassword: this.resetPasswordForm.get('newPassword')?.value,
        contactMethod: this.contactMethod,
        contact: this.contact
      };

      console.log('Données de réinitialisation:', {
        ...resetData,
        newPassword: '***' // Masquer le mot de passe dans les logs
      });

      // Appel API pour réinitialiser le mot de passe
      this.resetPassword(resetData);
    } else {
      this.markFormGroupTouched();
    }
  }

  private resetPassword(data: any): void {
    // Simuler un appel API
    setTimeout(() => {
      this.isLoading = false;

      // Simuler le succès de la réinitialisation
      const isResetSuccessful = true; // Remplacer par votre logique

      if (isResetSuccessful) {
        console.log('Mot de passe réinitialisé avec succès');
        
        // Afficher un message de succès
        this.showSuccessMessage();

        // Rediriger vers la page de connexion après un délai
        setTimeout(() => {
          this.router.navigate(['/login'], {
            queryParams: { resetSuccess: 'true' }
          });
        }, 2000);

      } else {
        this.showErrorMessage('Erreur lors de la réinitialisation. Veuillez réessayer.');
      }
    }, 2000);
  }

  private showSuccessMessage(): void {
    alert('Mot de passe réinitialisé avec succès ! Redirection vers la connexion...');
  }

  private showErrorMessage(message: string): void {
    alert(message);
  }

  private markFormGroupTouched(): void {
    Object.keys(this.resetPasswordForm.controls).forEach(key => {
      const control = this.resetPasswordForm.get(key);
      control?.markAsTouched();
    });
  }

  navigateToLogin(): void {
    this.router.navigate(['/login']);
  }

  // Getters pour faciliter l'accès aux contrôles dans le template
  get newPasswordControl() {
    return this.resetPasswordForm.get('newPassword');
  }

  get confirmPasswordControl() {
    return this.resetPasswordForm.get('confirmPassword');
  }

  // Méthodes utilitaires pour la validation
  isFieldInvalid(fieldName: string): boolean {
    const field = this.resetPasswordForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  getFieldError(fieldName: string): string {
    const field = this.resetPasswordForm.get(fieldName);
    if (field?.errors && field.touched) {
      if (field.errors['required']) {
        return fieldName === 'newPassword' 
          ? 'Le nouveau mot de passe est requis' 
          : 'La confirmation du mot de passe est requise';
      }
      if (field.errors['minlength']) {
        return 'Le mot de passe doit contenir au moins 8 caractères';
      }
      if (field.errors['pattern']) {
        return 'Le mot de passe doit contenir au moins une majuscule';
      }
    }

    // Vérifier l'erreur de correspondance des mots de passe
    if (fieldName === 'confirmPassword' && this.resetPasswordForm.errors?.['passwordMismatch']) {
      return 'Les mots de passe ne correspondent pas';
    }

    return '';
  }

  // Méthode pour vérifier si le formulaire est valide pour l'affichage
  get isFormValid(): boolean {
    return this.resetPasswordForm.valid && 
           this.passwordCriteria.minLength && 
           this.passwordCriteria.hasUppercase;
  }

  // Méthode pour obtenir la force du mot de passe (optionnel)
  getPasswordStrength(): 'weak' | 'medium' | 'strong' {
    const password = this.resetPasswordForm.get('newPassword')?.value || '';
    
    if (password.length < 8) return 'weak';
    if (password.length >= 8 && /[A-Z]/.test(password)) return 'medium';
    if (password.length >= 8 && /[A-Z]/.test(password) && /[0-9]/.test(password) && /[!@#$%^&*]/.test(password)) {
      return 'strong';
    }
      
    return 'medium';
  }
}