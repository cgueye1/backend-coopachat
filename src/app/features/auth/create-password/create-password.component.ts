import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  selector: 'app-create-password',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="min-h-screen relative overflow-hidden"
         style="background-image: url('/images/fond_ecran.png');">
      
      <div class="h-full w-full flex items-center justify-center">          
        <!-- Left Section - Form -->
        <div class="w-1/2 h-full p-8 p-12 flex flex-col justify-center">
          <!-- Logo -->
          <div class="mb-4">
            <img 
              src="/images/logo.png" 
              alt="Logo CoopAchat"
              class="h-12 w-auto animate-float"
            >
          </div>
          
          <!-- Title -->
          <div class="mb-8">
            <h1 class="text-3xl lg:text-4xl font-bold text-gray-800 mb-4">
              Création mot de passe
            </h1>
            <p class="text-gray-600 text-lg">
              Choisissez un mot de passe sécurisé pour protéger votre compte.
            </p>
          </div>
          
          <!-- Form -->
          <form [formGroup]="passwordForm" (ngSubmit)="onSubmit()" class="space-y-6">
            <!-- New Password Field -->
            <div>
              <label class="block text-gray-700 text-sm font-medium mb-2">
                Nouveau mot de passe
              </label>
              <div class="relative">
                <input 
                  [type]="showPassword ? 'text' : 'password'"
                  formControlName="newPassword"
                  placeholder="*******"
                  class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-orange-500 focus:border-orange-500 outline-none transition-all duration-200 text-gray-700 pr-12"
                  [class.border-red-500]="passwordForm.get('newPassword')?.invalid && passwordForm.get('newPassword')?.touched"
                >
                <button 
                  type="button"
                  (click)="togglePassword()"
                  class="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
                >
                  <svg *ngIf="!showPassword" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                  </svg>
                  <svg *ngIf="showPassword" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L3 3m6.878 6.878L21 21"></path>
                  </svg>
                </button>
              </div>
            </div>

            <!-- Confirm Password Field -->
            <div>
              <label class="block text-gray-700 text-sm font-medium mb-2">
                Confirmation mot de passe
              </label>
              <div class="relative">
                <input 
                  [type]="showConfirmPassword ? 'text' : 'password'"
                  formControlName="confirmPassword"
                  placeholder="*******"
                  class="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-orange-500 focus:border-orange-500 outline-none transition-all duration-200 text-gray-700 pr-12"
                  [class.border-red-500]="passwordForm.get('confirmPassword')?.invalid && passwordForm.get('confirmPassword')?.touched"
                >
                <button 
                  type="button"
                  (click)="toggleConfirmPassword()"
                  class="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
                >
                  <svg *ngIf="!showConfirmPassword" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path>
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"></path>
                  </svg>
                  <svg *ngIf="showConfirmPassword" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.878 9.878L3 3m6.878 6.878L21 21"></path>
                  </svg>
                </button>
              </div>
            </div>

            <!-- Password Requirements -->
            <div class="bg-gray-50 p-4 rounded-lg">
              <p class="text-sm font-medium text-gray-700 mb-3">Votre mot de passe doit contenir :</p>
              <div class="space-y-2">
                <div class="flex items-center space-x-2">
                  <div class="w-4 h-4 rounded-full flex items-center justify-center"
                       [class]="hasMinLength ? 'bg-green-500' : ''">
                    <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
<path d="M7.9215 10.869L6.0495 8.997C5.979 8.927 5.89275 8.8895 5.79075 8.8845C5.68875 8.8795 5.598 8.917 5.5185 8.997C5.439 9.077 5.399 9.1655 5.3985 9.2625C5.398 9.3595 5.438 9.448 5.5185 9.528L7.497 11.5065C7.618 11.628 7.75925 11.6888 7.92075 11.6888C8.08225 11.6888 8.22375 11.628 8.34525 11.5065L12.453 7.3995C12.523 7.329 12.5605 7.24275 12.5655 7.14075C12.5705 7.03875 12.533 6.948 12.453 6.8685C12.373 6.789 12.2845 6.749 12.1875 6.7485C12.0905 6.748 12.002 6.788 11.922 6.8685L7.9215 10.869ZM9.00225 15.75C8.06925 15.75 7.19175 15.573 6.36975 15.219C5.54825 14.8645 4.8335 14.3835 4.2255 13.776C3.6175 13.1685 3.13625 12.4545 2.78175 11.634C2.42725 10.8135 2.25 9.93625 2.25 9.00225C2.25 8.06825 2.42725 7.19075 2.78175 6.36975C3.13575 5.54825 3.616 4.8335 4.2225 4.2255C4.829 3.6175 5.54325 3.13625 6.36525 2.78175C7.18725 2.42725 8.06475 2.25 8.99775 2.25C9.93075 2.25 10.8083 2.42725 11.6302 2.78175C12.4517 3.13575 13.1665 3.61625 13.7745 4.22325C14.3825 4.83025 14.8638 5.5445 15.2183 6.366C15.5728 7.1875 15.75 8.06475 15.75 8.99775C15.75 9.93075 15.573 10.8083 15.219 11.6302C14.865 12.4522 14.384 13.167 13.776 13.7745C13.168 14.382 12.454 14.8633 11.634 15.2183C10.814 15.5732 9.93675 15.7505 9.00225 15.75Z" fill="#647480"/>
</svg>

                  </div>
                  <span class="text-sm" [class]="hasMinLength ? '' : 'text-gray-600'">
                    Au moins 8 caractères
                  </span>
                </div>
                <div class="flex items-center space-x-2">
                  <div class="w-4 h-4 rounded-full flex items-center justify-center"
                       [class]="hasUppercase ? 'bg-green-500' : ''">
                    <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
<path d="M7.9215 10.869L6.0495 8.997C5.979 8.927 5.89275 8.8895 5.79075 8.8845C5.68875 8.8795 5.598 8.917 5.5185 8.997C5.439 9.077 5.399 9.1655 5.3985 9.2625C5.398 9.3595 5.438 9.448 5.5185 9.528L7.497 11.5065C7.618 11.628 7.75925 11.6888 7.92075 11.6888C8.08225 11.6888 8.22375 11.628 8.34525 11.5065L12.453 7.3995C12.523 7.329 12.5605 7.24275 12.5655 7.14075C12.5705 7.03875 12.533 6.948 12.453 6.8685C12.373 6.789 12.2845 6.749 12.1875 6.7485C12.0905 6.748 12.002 6.788 11.922 6.8685L7.9215 10.869ZM9.00225 15.75C8.06925 15.75 7.19175 15.573 6.36975 15.219C5.54825 14.8645 4.8335 14.3835 4.2255 13.776C3.6175 13.1685 3.13625 12.4545 2.78175 11.634C2.42725 10.8135 2.25 9.93625 2.25 9.00225C2.25 8.06825 2.42725 7.19075 2.78175 6.36975C3.13575 5.54825 3.616 4.8335 4.2225 4.2255C4.829 3.6175 5.54325 3.13625 6.36525 2.78175C7.18725 2.42725 8.06475 2.25 8.99775 2.25C9.93075 2.25 10.8083 2.42725 11.6302 2.78175C12.4517 3.13575 13.1665 3.61625 13.7745 4.22325C14.3825 4.83025 14.8638 5.5445 15.2183 6.366C15.5728 7.1875 15.75 8.06475 15.75 8.99775C15.75 9.93075 15.573 10.8083 15.219 11.6302C14.865 12.4522 14.384 13.167 13.776 13.7745C13.168 14.382 12.454 14.8633 11.634 15.2183C10.814 15.5732 9.93675 15.7505 9.00225 15.75Z" fill="#647480"/>
</svg>

                  </div>
                  <span class="text-sm" [class]="hasUppercase ? '' : 'text-gray-600'">
                    Au moins une majuscule
                  </span>
                </div>
              </div>
            </div>

            <!-- Error Messages -->
            <div *ngIf="passwordForm.errors?.['passwordMismatch'] && passwordForm.get('confirmPassword')?.touched" 
                 class="text-red-500 text-sm">
              Les mots de passe ne correspondent pas
            </div>

            <!-- Submit Button -->
            <button 
              type="submit"
              [disabled]="passwordForm.invalid"
              class="w-full bg-gradient-to-r from-orange-500 to-orange-600 text-white py-3 px-4 rounded-lg hover:from-orange-600 hover:to-orange-700 focus:ring-4 focus:ring-orange-200 font-medium text-lg transition-all duration-200 transform hover:scale-[1.02] disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
            >
              Créer le mot de passe
            </button>

            <!-- Back to Login -->
            <div class="text-center pt-4">
              <button 
                type="button"
                (click)="goBackToLogin()"
                class="flex items-center justify-center mx-auto text-gray-600 hover:text-gray-800 font-medium transition-colors duration-200"
              >
                <svg class="w-5 h-5 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"></path>
                </svg>
                Retour à la connexion
              </button>
            </div>
          </form>
        </div>
        
        <!-- Right Section - Image -->
        <div class="w-1/2 h-full relative flex items-center justify-center">
          <img 
            src="/images/image.png"
            alt="Femmes faisant du shopping"
            class="w-full h-full object-cover"
          >
        </div>
      </div>
    </div>
  `,
  styles: [`
    .animate-float {
      animation: float  ease-in-out infinite;
    }
    
    @keyframes float {
      0%, 100% { transform: translateY(0px); }
      50% { transform: translateY(-20px); }
    }
  `]
})
export class CreatePasswordComponent {
  passwordForm: FormGroup;
  showPassword = false;
  showConfirmPassword = false;

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

  // Validator to check if passwords match
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

  get hasUppercase(): boolean {
    const password = this.passwordForm.get('newPassword')?.value || '';
    return /[A-Z]/.test(password);
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  onSubmit(): void {
    if (this.passwordForm.valid) {
      console.log('Password created successfully:', this.passwordForm.value);
      // Ici vous pouvez ajouter la logique pour sauvegarder le mot de passe
      // Rediriger vers la page de connexion ou tableau de bord
      this.router.navigate(['/login']);
    } else {
      console.log('Form is invalid');
      // Marquer tous les champs comme touchés pour afficher les erreurs
      Object.keys(this.passwordForm.controls).forEach(key => {
        this.passwordForm.get(key)?.markAsTouched();
      });
    }
  }

  goBackToLogin(): void {
    this.router.navigate(['/login']);
  }
}