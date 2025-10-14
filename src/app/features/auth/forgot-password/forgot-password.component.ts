import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './forgot-password.component.html'
})
export class ForgotPasswordComponent implements OnInit {
  forgotPasswordForm!: FormGroup;
  contactMethod: 'email' | 'phone' = 'email';
  isLoading = false;

  constructor(
    private formBuilder: FormBuilder,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.initializeForm();
  }

  private initializeForm(): void {
    this.forgotPasswordForm = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern(/^(\+221|00221)?[7][0-9]{8}$/)]],
    });

    // Écouter les changements de méthode de contact
    this.updateValidators();
  }

  setContactMethod(method: 'email' | 'phone'): void {
    this.contactMethod = method;
    this.updateValidators();
  }

  private updateValidators(): void {
    const emailControl = this.forgotPasswordForm.get('email');
    const phoneControl = this.forgotPasswordForm.get('phone');

    if (this.contactMethod === 'email') {
      // Activer les validateurs pour email, désactiver pour téléphone
      emailControl?.setValidators([Validators.required, Validators.email]);
      phoneControl?.clearValidators();
      phoneControl?.setValue('');
    } else {
      // Activer les validateurs pour téléphone, désactiver pour email
      phoneControl?.setValidators([
        Validators.required, 
        Validators.pattern(/^(\+221|00221)?[7][0-9]{8}$/)
      ]);
      emailControl?.clearValidators();
      emailControl?.setValue('');
    }

    // Mettre à jour le statut de validation
    emailControl?.updateValueAndValidity();
    phoneControl?.updateValueAndValidity();
  }

  onSubmit(): void {
    if (this.forgotPasswordForm.valid) {
      this.isLoading = true;

      const formData = {
        contactMethod: this.contactMethod,
        [this.contactMethod]: this.forgotPasswordForm.get(this.contactMethod)?.value
      };

      console.log('Données du formulaire:', formData);

      // Simulation d'appel API
      this.sendVerificationCode(formData);
    } else {
      this.markFormGroupTouched();
    }
  }

  private sendVerificationCode(data: any): void {
    // Simuler un appel API
    setTimeout(() => {
      this.isLoading = false;
      console.log('Code de vérification envoyé:', data);
      
      // Ici vous pouvez :
      // 1. Appeler votre service de réinitialisation
      // 2. Naviguer vers la page de vérification du code
      // 3. Afficher un message de succès
      
      // Exemple : navigation vers la page de vérification du code
      this.router.navigate(['/otp-verification'], { 
        queryParams: { 
          method: this.contactMethod,
          contact: data[this.contactMethod]
        }
      });

      alert(`Code de vérification envoyé par ${this.contactMethod === 'email' ? 'email' : 'SMS'} !`);
    }, 2000);
  }

  private markFormGroupTouched(): void {
    Object.keys(this.forgotPasswordForm.controls).forEach(key => {
      const control = this.forgotPasswordForm.get(key);
      control?.markAsTouched();
    });
  }

  navigateToLogin(): void {
    this.router.navigate(['/login']);
  }

  // Getters pour faciliter l'accès aux contrôles dans le template
  get emailControl() {
    return this.forgotPasswordForm.get('email');
  }

  get phoneControl() {
    return this.forgotPasswordForm.get('phone');
  }

  // Méthodes utilitaires pour la validation
  isFieldInvalid(fieldName: string): boolean {
    const field = this.forgotPasswordForm.get(fieldName);
    return !!(field && field.invalid && field.touched);
  }

  getFieldError(fieldName: string): string {
    const field = this.forgotPasswordForm.get(fieldName);
    if (field?.errors && field.touched) {
      if (field.errors['required']) {
        return fieldName === 'email' 
          ? 'L\'adresse email est requise' 
          : 'Le numéro de téléphone est requis';
      }
      if (field.errors['email']) {
        return 'Format d\'email invalide';
      }
      if (field.errors['pattern']) {
        return 'Format de téléphone invalide';
      }
    }
    return '';
  }
}