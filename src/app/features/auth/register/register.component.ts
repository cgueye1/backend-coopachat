import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';

// TODO: Importer le service d'inscription
// import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  template: `
    <div class="min-h-screen relative overflow-hidden"
         style="background-image: url('/images/fond_ecran.png');">

      <div class="h-full w-full flex items-center justify-center ">          
        <!-- Left Section - Form -->
        <div class="w-1/2 h-full p-12 p-12 flex flex-col justify-center sm:px-16">
          <!-- Logo -->
          <div class="mb-2">
            <img 
              src="/images/logo.png" 
              alt="Logo CoopAchat"
              class="h-12 w-auto animate-float"
            >
          </div>

          <!-- Title -->
          <div class="mb-8">
            <h1 class="text-3xl lg:text-4xl font-bold text-gray-800 mb-4">
              Créer un compte
            </h1>
            <p class="text-gray-600 text-lg">
              Inscrivez-vous pour accéder à votre espace personnel de gestion logistique
            </p>
          </div>

          <!-- TODO: Afficher les messages d'erreur de l'API -->
          <!-- <div *ngIf="errorMessage" class="mb-4 p-4 bg-red-50 border border-red-200 rounded-lg">
            <p class="text-red-600 text-sm">{{ errorMessage }}</p>
          </div> -->

          <!-- TODO: Afficher un message de succès -->
          <!-- <div *ngIf="successMessage" class="mb-4 p-4 bg-green-50 border border-green-200 rounded-lg">
            <p class="text-green-600 text-sm">{{ successMessage }}</p>
          </div> -->

          <!-- TODO: Afficher un loader pendant la requête API -->
          <!-- <div *ngIf="isLoading" class="mb-4 text-center">
            <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-orange-600"></div>
            <p class="text-gray-600 mt-2">Création de votre compte...</p>
          </div> -->

          <!-- User Type Selection -->
          <div class="mb-8 ">
            <p class="text-gray-700 text-sm font-medium mb-4">Vous êtes ?</p>
            <div class="grid grid-cols-2 gap-4">
              <label class="flex items-center p-4 border-2 rounded-lg cursor-pointer transition-all duration-200 border-gray-200 hover:border-orange-300"
                     [class.border-orange-500]="registerForm.get('userType')?.value === 'commercial'"
                     [class.bg-orange-50]="registerForm.get('userType')?.value === 'commercial'"
                     (click)="selectUserType('commercial')">
                <input 
                  type="radio" 
                  value="commercial"
                  formControlName="userType"
                  class="w-5 h-5 text-orange-600 bg-white border-2 border-gray-300 focus:ring-0 cursor-pointer"
                  [checked]="registerForm.get('userType')?.value === 'commercial'"
                >
                <span class="ml-3 text-gray-700 font-medium">Commercial</span>
              </label>
              
              <label class="flex items-center p-4 border-2 rounded-lg cursor-pointer transition-all duration-200 border-gray-200 hover:border-orange-300"
                     [class.border-orange-500]="registerForm.get('userType')?.value === 'logistique'"
                     [class.bg-orange-50]="registerForm.get('userType')?.value === 'logistique'"
                     (click)="selectUserType('logistique')">
                <input 
                  type="radio" 
                  value="logistique"
                  formControlName="userType"
                  class="w-5 h-5 text-orange-600 bg-white border-2 border-gray-300 focus:ring-0 cursor-pointer"
                  [checked]="registerForm.get('userType')?.value === 'logistique'"
                >
                <span class="ml-3 text-gray-700 font-medium">Logistique</span>
              </label>
            </div>
            <!-- TODO: Afficher erreur de validation pour userType -->
            <!-- <div *ngIf="registerForm.get('userType')?.invalid && registerForm.get('userType')?.touched" class="mt-2">
              <p class="text-red-500 text-xs">Veuillez sélectionner un type d'utilisateur</p>
            </div> -->
          </div>

          <!-- Form -->
          <form [formGroup]="registerForm" (ngSubmit)="onSubmit()" class="space-y-6">
            <!-- Prénom et Nom -->
            <div class="grid grid-cols-2 gap-6">
              <div>
                <label class="block text-gray-700 text-sm font-medium mb-3">
                  Prénom <span class="text-red-500">*</span>
                </label>
                <input 
                  type="text" 
                  formControlName="firstName"
                  placeholder="Ex: Lamine"
                  class="w-full px-4 py-4 bg-gray-50 border border-gray-200 rounded-lg focus:ring-2 focus:ring-orange-500 focus:border-orange-500 focus:bg-white outline-none transition-all duration-200 text-gray-700 placeholder-gray-400"
                >
                <!-- TODO: Afficher erreurs de validation -->
                <!-- <div *ngIf="registerForm.get('firstName')?.invalid && registerForm.get('firstName')?.touched" class="mt-1">
                  <p class="text-red-500 text-xs" *ngIf="registerForm.get('firstName')?.errors?.['required']">Le prénom est requis</p>
                  <p class="text-red-500 text-xs" *ngIf="registerForm.get('firstName')?.errors?.['minlength']">Le prénom doit contenir au moins 2 caractères</p>
                </div> -->
              </div>
              
              <div>
                <label class="block text-gray-700 text-sm font-medium mb-3">
                  Nom <span class="text-red-500">*</span>
                </label>
                <input 
                  type="text" 
                  formControlName="lastName"
                  placeholder="Ex: Ndiaye"
                  class="w-full px-4 py-4 bg-gray-50 border border-gray-200 rounded-lg focus:ring-2 focus:ring-orange-500 focus:border-orange-500 focus:bg-white outline-none transition-all duration-200 text-gray-700 placeholder-gray-400"
                >
                <!-- TODO: Afficher erreurs de validation -->
                <!-- <div *ngIf="registerForm.get('lastName')?.invalid && registerForm.get('lastName')?.touched" class="mt-1">
                  <p class="text-red-500 text-xs" *ngIf="registerForm.get('lastName')?.errors?.['required']">Le nom est requis</p>
                  <p class="text-red-500 text-xs" *ngIf="registerForm.get('lastName')?.errors?.['minlength']">Le nom doit contenir au moins 2 caractères</p>
                </div> -->
              </div>
            </div>

            <!-- Email et Téléphone -->
            <div class="grid grid-cols-2 gap-6">
              <div>
                <label class="block text-gray-700 text-sm font-medium mb-3">
                  Adresse e-mail professionnelle <span class="text-red-500">*</span>
                </label>
                <div class="relative">
                  <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                    <svg width="20" height="16" viewBox="0 0 20 16" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M2 1H18C18.55 1 19 1.45 19 2V14C19 14.55 18.55 15 18 15H2C1.45 15 1 14.55 1 14V2C1 1.45 1.45 1 2 1Z" stroke="#ACB5BB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M1 2.5L10 8L19 2.5" stroke="#ACB5BB" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                  </div>
                  <input 
                    type="email" 
                    formControlName="email"
                    placeholder="Ex: nom@entreprise.com"
                    class="w-full pl-12 pr-4 py-4 bg-gray-50 border border-gray-200 rounded-lg focus:ring-2 focus:ring-orange-500 focus:border-orange-500 focus:bg-white outline-none transition-all duration-200 text-gray-700 placeholder-gray-400"
                  >
                </div>
                <!-- TODO: Afficher erreurs de validation -->
                <!-- <div *ngIf="registerForm.get('email')?.invalid && registerForm.get('email')?.touched" class="mt-1">
                  <p class="text-red-500 text-xs" *ngIf="registerForm.get('email')?.errors?.['required']">L'email est requis</p>
                  <p class="text-red-500 text-xs" *ngIf="registerForm.get('email')?.errors?.['email']">Format d'email invalide</p>
                </div> -->
              </div>
              
              <div>
                <label class="block text-gray-700 text-sm font-medium mb-3">
                  Numéro de téléphone <span class="text-red-500">*</span>
                </label>
                <div class="relative">
                  <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                    <svg width="25" height="24" viewBox="0 0 25 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                      <path d="M20.45 21C18.3667 21 16.3083 20.546 14.275 19.638C12.2417 18.73 10.3917 17.4423 8.725 15.775C7.05833 14.1077 5.771 12.2577 4.863 10.225C3.955 8.19233 3.50067 6.134 3.5 4.05C3.5 3.75 3.6 3.5 3.8 3.3C4 3.1 4.25 3 4.55 3H8.6C8.83333 3 9.04167 3.07933 9.225 3.238C9.40833 3.39667 9.51667 3.584 9.55 3.8L10.2 7.3C10.2333 7.56667 10.225 7.79167 10.175 7.975C10.125 8.15833 10.0333 8.31667 9.9 8.45L7.475 10.9C7.80833 11.5167 8.204 12.1123 8.662 12.687C9.12 13.2617 9.62433 13.816 10.175 14.35C10.6917 14.8667 11.2333 15.346 11.8 15.788C12.3667 16.23 12.9667 16.634 13.6 17L15.95 14.65C16.1 14.5 16.296 14.3877 16.538 14.313C16.78 14.2383 17.0173 14.2173 17.25 14.25L20.7 14.95C20.9333 15.0167 21.125 15.1377 21.275 15.313C21.425 15.4883 21.5 15.684 21.5 15.9V19.95C21.5 20.25 21.4 20.5 21.2 20.7C21 20.9 20.75 21 20.45 21ZM6.525 9L8.175 7.35L7.75 5H5.525C5.60833 5.68333 5.725 6.35833 5.875 7.025C6.025 7.69167 6.24167 8.35 6.525 9ZM15.475 17.95C16.125 18.2333 16.7877 18.4583 17.463 18.625C18.1383 18.7917 18.8173 18.9 19.5 18.95V16.75L17.15 16.275L15.475 17.95Z" fill="#ACB5BB"/>
                    </svg>
                  </div>
                  <input 
                    type="tel" 
                    formControlName="phone"
                    placeholder="Ex: 77 123 45 67"
                    class="w-full pl-12 pr-4 py-4 bg-gray-50 border border-gray-200 rounded-lg focus:ring-2 focus:ring-orange-500 focus:border-orange-500 focus:bg-white outline-none transition-all duration-200 text-gray-700 placeholder-gray-400"
                  >
                </div>
                <!-- TODO: Afficher erreurs de validation -->
                <!-- <div *ngIf="registerForm.get('phone')?.invalid && registerForm.get('phone')?.touched" class="mt-1">
                  <p class="text-red-500 text-xs" *ngIf="registerForm.get('phone')?.errors?.['required']">Le téléphone est requis</p>
                  <p class="text-red-500 text-xs" *ngIf="registerForm.get('phone')?.errors?.['pattern']">Format de téléphone invalide</p>
                </div> -->
              </div>
            </div>

            <!-- Entreprise -->
            <div>
              <label class="block text-gray-700 text-sm font-medium mb-3">
                Entreprise / Entité <span class="text-red-500">*</span>
              </label>
              <div class="relative">
                <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                  <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M9.83371 0.5C10.2542 0.499867 10.6592 0.658672 10.9675 0.944581C11.2758 1.23049 11.4647 1.62237 11.4962 2.04167L11.5004 2.16667V5.5H14.0004C14.4209 5.49987 14.8259 5.65867 15.1342 5.94458C15.4425 6.23049 15.6313 6.62237 15.6629 7.04167L15.667 7.16667V13.8333H16.5004C16.7128 13.8336 16.9171 13.9149 17.0715 14.0607C17.226 14.2065 17.3189 14.4058 17.3314 14.6178C17.3438 14.8299 17.2748 15.0386 17.1385 15.2015C17.0022 15.3644 16.8088 15.4691 16.5979 15.4942L16.5004 15.5H1.50038C1.28798 15.4998 1.08369 15.4184 0.929239 15.2726C0.774792 15.1268 0.68185 14.9275 0.669402 14.7155C0.656955 14.5035 0.725941 14.2947 0.862266 14.1318C0.998591 13.9689 1.19197 13.8643 1.40288 13.8392L1.50038 13.8333H2.33371V2.16667C2.33358 1.74619 2.49238 1.34119 2.77829 1.03288C3.0642 0.724559 3.45608 0.535703 3.87538 0.504167L4.00038 0.5H9.83371ZM14.0004 7.16667H11.5004V13.8333H14.0004V7.16667ZM9.83371 2.16667H4.00038V13.8333H9.83371V2.16667ZM8.16705 10.5V12.1667H5.66705V10.5H8.16705ZM8.16705 7.16667V8.83333H5.66705V7.16667H8.16705ZM8.16705 3.83333V5.5H5.66705V3.83333H8.16705Z" fill="#9C9AA5"/>
                  </svg>
                </div>
                <input 
                  type="text" 
                  formControlName="company"
                  placeholder="Ex: Entreprise SA"
                  class="w-full pl-12 pr-4 py-4 bg-gray-50 border border-gray-200 rounded-lg focus:ring-2 focus:ring-orange-500 focus:border-orange-500 focus:bg-white outline-none transition-all duration-200 text-gray-700 placeholder-gray-400"
                >
              </div>
              <!-- TODO: Afficher erreurs de validation -->
              <!-- <div *ngIf="registerForm.get('company')?.invalid && registerForm.get('company')?.touched" class="mt-1">
                <p class="text-red-500 text-xs" *ngIf="registerForm.get('company')?.errors?.['required']">L'entreprise est requise</p>
                <p class="text-red-500 text-xs" *ngIf="registerForm.get('company')?.errors?.['minlength']">Le nom de l'entreprise doit contenir au moins 2 caractères</p>
              </div> -->
            </div>

            <button 
              type="submit"
              [disabled]="!registerForm.valid"
              class="w-full bg-gradient-to-r from-orange-500 to-orange-600 text-white py-3 px-4 rounded-lg hover:from-orange-600 hover:to-orange-700 focus:ring-4 focus:ring-orange-200 font-medium text-lg transition-all duration-200 transform hover:scale-[1.02] disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
            >
              Créer un compte
            </button>

            <div class="text-center pt-4">
              <span class="text-gray-600">Vous avez déjà un compte ? </span>
              <a href="/login" class="text-orange-600 hover:text-orange-500 font-medium">Connectez-vous</a>
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
export class RegisterComponent {
  registerForm: FormGroup;
  
  // TODO: Ajouter ces propriétés pour gérer l'état de l'API
  // isLoading = false;
  // errorMessage = '';
  // successMessage = '';

  constructor(
    private fb: FormBuilder, 
    private router: Router
    // TODO: Injecter le service d'authentification
    // private authService: AuthService
  ) {
    this.registerForm = this.fb.group({
      userType: ['', [Validators.required]],
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern(/^[+]?[0-9\s\-\(\)]{8,15}$/)]],
      company: ['', [Validators.required, Validators.minLength(2)]]
    });
  }

  selectUserType(type: string): void {
    this.registerForm.patchValue({ userType: type });
  }

  onSubmit(): void {
    if (this.registerForm.valid) {
      console.log('Form submitted:', this.registerForm.value);
      
      // TODO: ============ INTÉGRATION API - DÉBUT ============
      // this.isLoading = true;
      // this.errorMessage = '';
      // this.successMessage = '';
      
      // const registrationData = {
      //   userType: this.registerForm.value.userType,
      //   firstName: this.registerForm.value.firstName,
      //   lastName: this.registerForm.value.lastName,
      //   email: this.registerForm.value.email,
      //   phone: this.registerForm.value.phone,
      //   company: this.registerForm.value.company
      // };
      
      // this.authService.register(registrationData).subscribe({
      //   next: (response) => {
      //     // Succès de l'inscription
      //     this.isLoading = false;
      //     console.log('Inscription réussie:', response);
      //     
      //     this.successMessage = 'Inscription réussie ! Redirection vers la vérification...';
      //     
      //     // Stocker l'email pour la page OTP
      //     const email = this.registerForm.get('email')?.value;
      //     localStorage.setItem('verificationEmail', email);
      //     
      //     // Si l'API retourne un userId ou token temporaire, le stocker aussi
      //     // localStorage.setItem('tempUserId', response.userId);
      //     
      //     // Rediriger vers la page OTP après un court délai
      //     setTimeout(() => {
      //       this.router.navigate(['/otp-verification'], {
      //         queryParams: { 
      //           email: email,
      //           type: 'registration'
      //         }
      //       });
      //     }, 1500);
      //   },
      //   error: (error) => {
      //     // Gestion des erreurs
      //     this.isLoading = false;
      //     console.error('Erreur d\'inscription:', error);
      //     
      //     // Afficher un message d'erreur approprié
      //     if (error.status === 409) {
      //       this.errorMessage = 'Cet email est déjà utilisé';
      //     } else if (error.status === 400) {
      //       this.errorMessage = error.error?.message || 'Données invalides';
      //     } else if (error.status === 0) {
      //       this.errorMessage = 'Impossible de se connecter au serveur';
      //     } else {
      //       this.errorMessage = error.error?.message || 'Une erreur est survenue lors de l\'inscription';
      //     }
      //   }
      // });
      // TODO: ============ INTÉGRATION API - FIN ============
      
      // Code de simulation actuel (à supprimer après intégration API)
      setTimeout(() => {
        const email = this.registerForm.get('email')?.value;
        localStorage.setItem('verificationEmail', email);
        
        this.router.navigate(['/otp-verification'], {
          queryParams: { 
            email: email,
            type: 'registration'
          }
        });
      }, 2000);
      
    } else {
      console.log('Form is invalid');
      // Marquer tous les champs comme touchés pour afficher les erreurs
      Object.keys(this.registerForm.controls).forEach(key => {
        const control = this.registerForm.get(key);
        control?.markAsTouched();
      });
    }
  }

  // TODO: Méthode pour vérifier si l'email existe déjà (optionnel)
  // checkEmailExists(): void {
  //   const email = this.registerForm.get('email')?.value;
  //   if (email && this.registerForm.get('email')?.valid) {
  //     this.authService.checkEmailExists(email).subscribe({
  //       next: (exists) => {
  //         if (exists) {
  //           this.registerForm.get('email')?.setErrors({ emailTaken: true });
  //           this.errorMessage = 'Cet email est déjà utilisé';
  //         }
  //       },
  //       error: (error) => {
  //         console.error('Erreur lors de la vérification de l\'email:', error);
  //       }
  //     });
  //   }
  // }

  // TODO: Méthode pour resend OTP si nécessaire
  // resendOTP(): void {
  //   const email = localStorage.getItem('verificationEmail');
  //   if (email) {
  //     this.authService.resendOTP(email).subscribe({
  //       next: (response) => {
  //         console.log('OTP renvoyé avec succès');
  //         this.successMessage = 'Un nouveau code a été envoyé à votre email';
  //       },
  //       error: (error) => {
  //         console.error('Erreur lors du renvoi du OTP:', error);
  //         this.errorMessage = 'Impossible de renvoyer le code';
  //       }
  //     });
  //   }
  // }
}