import { Component, ElementRef, QueryList, ViewChildren, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-otp-verification',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="min-h-screen relative overflow-hidden"
         style="background-image: url('/images/fond_ecran.png');">

      <div class="h-full w-full flex items-center justify-center">
        <!-- Left Section - Form -->
        <div class="w-1/2 h-full p-8 p-12 flex flex-col justify-center">
          <!-- Logo -->
          <div class="mb-8">
            <img 
              src="/images/logo.png" 
              alt="Logo CoopAchat"
              class="h-12 w-auto animate-float"
            >
          </div>
          <!-- Title -->
          <div class="mb-4 flex flex-wrap items-center">
              <h1 class="text-3xl lg:text-4xl font-bold text-gray-800 mr-2">
                  Entrez le code de vérification
              </h1>
              <p class="text-gray-600 text-lg mr-2">Veuillez entrer le code à 6 chiffres envoyé par Email à</p>
              <span>{{ maskedEmail }}</span>
          </div>
          <!-- OTP Form -->
          <form [formGroup]="otpForm" (ngSubmit)="onSubmit()" class="space-y-4">
            <!-- OTP Input Fields -->
            <div>
              <div class="flex justify-center space-x-4 mb-4">
                <input
                  #otpInput
                  type="text"
                  maxlength="1"
                  formControlName="digit1"
                  (input)="onDigitInput($event, 0)"
                  (keydown)="onKeyDown($event, 0)"
                  (paste)="onPaste($event, 0)"
                  (focus)="onFocus($event, 0)"
                  class="w-12 h-12 border border-gray-300 rounded-lg text-center text-xl font-semibold focus:ring-2 focus:ring-orange-500 focus:border-orange-500 outline-none transition-all duration-200 text-gray-700"
                  [class.border-red-500]="isInvalid"
                  [class.border-orange-500]="otpForm.get('digit1')?.value"
                  autocomplete="off"
                />
                <input
                  #otpInput
                  type="text"
                  maxlength="1"
                  formControlName="digit2"
                  (input)="onDigitInput($event, 1)"
                  (keydown)="onKeyDown($event, 1)"
                  (paste)="onPaste($event, 1)"
                  (focus)="onFocus($event, 1)"
                  class="w-12 h-12 border border-gray-300 rounded-lg text-center text-xl font-semibold focus:ring-2 focus:ring-orange-500 focus:border-orange-500 outline-none transition-all duration-200 text-gray-700"
                  [class.border-red-500]="isInvalid"
                  [class.border-orange-500]="otpForm.get('digit2')?.value"
                  autocomplete="off"
                />
                <input
                  #otpInput
                  type="text"
                  maxlength="1"
                  formControlName="digit3"
                  (input)="onDigitInput($event, 2)"
                  (keydown)="onKeyDown($event, 2)"
                  (paste)="onPaste($event, 2)"
                  (focus)="onFocus($event, 2)"
                  class="w-12 h-12 border border-gray-300 rounded-lg text-center text-xl font-semibold focus:ring-2 focus:ring-orange-500 focus:border-orange-500 outline-none transition-all duration-200 text-gray-700"
                  [class.border-red-500]="isInvalid"
                  [class.border-orange-500]="otpForm.get('digit3')?.value"
                  autocomplete="off"
                />
                <input
                  #otpInput
                  type="text"
                  maxlength="1"
                  formControlName="digit4"
                  (input)="onDigitInput($event, 3)"
                  (keydown)="onKeyDown($event, 3)"
                  (paste)="onPaste($event, 3)"
                  (focus)="onFocus($event, 3)"
                  class="w-12 h-12 border border-gray-300 rounded-lg text-center text-xl font-semibold focus:ring-2 focus:ring-orange-500 focus:border-orange-500 outline-none transition-all duration-200 text-gray-700"
                  [class.border-red-500]="isInvalid"
                  [class.border-orange-500]="otpForm.get('digit4')?.value"
                  autocomplete="off"
                />
                <input
                  #otpInput
                  type="text"
                  maxlength="1"
                  formControlName="digit5"
                  (input)="onDigitInput($event, 4)"
                  (keydown)="onKeyDown($event, 4)"
                  (paste)="onPaste($event, 4)"
                  (focus)="onFocus($event, 4)"
                  class="w-12 h-12 border border-gray-300 rounded-lg text-center text-xl font-semibold focus:ring-2 focus:ring-orange-500 focus:border-orange-500 outline-none transition-all duration-200 text-gray-700"
                  [class.border-red-500]="isInvalid"
                  [class.border-orange-500]="otpForm.get('digit5')?.value"
                  autocomplete="off"
                />
                <input
                  #otpInput
                  type="text"
                  maxlength="1"
                  formControlName="digit6"
                  (input)="onDigitInput($event, 5)"
                  (keydown)="onKeyDown($event, 5)"
                  (paste)="onPaste($event, 5)"
                  (focus)="onFocus($event, 5)"
                  class="w-12 h-12 border border-gray-300 rounded-lg text-center text-xl font-semibold focus:ring-2 focus:ring-orange-500 focus:border-orange-500 outline-none transition-all duration-200 text-gray-700"
                  [class.border-red-500]="isInvalid"
                  [class.border-orange-500]="otpForm.get('digit6')?.value"
                  autocomplete="off"
                />
              </div>
            </div>
            <!-- Error Message -->
            <div *ngIf="errorMessage" class="text-red-500 text-sm text-center">
              {{ errorMessage }}
            </div>
            <!-- Resend Code -->
            <div class="text-center">
              <span class="text-gray-600">Vous n'avez pas reçu de code ? </span>
              <button
                type="button"
                (click)="resendCode()"
                [disabled]="isResendDisabled"
                class="text-orange-600 hover:text-orange-500 font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <span *ngIf="!isResendDisabled">Renvoyer</span>
                <span *ngIf="isResendDisabled">Renvoyer dans ({{ resendCountdown }}s)</span>
              </button>
            </div>
            <!-- Verify Button -->
            <button
              type="submit"
              [disabled]="!isCodeComplete || isLoading"
              class="w-full bg-gradient-to-r from-orange-500 to-orange-600 text-white py-3 px-4 rounded-lg hover:from-orange-600 hover:to-orange-700 focus:ring-4 focus:ring-orange-200 font-medium text-lg transition-all duration-200 transform hover:scale-[1.02] disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:scale-100"
            >
              <span *ngIf="!isLoading">Vérifier le code</span>
              <span *ngIf="isLoading" class="flex items-center justify-center">
                <svg class="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Vérification...
              </span>
            </button>

            <!-- Back to Login -->
            <div class="text-center pt-4">
              <button
                type="button"
                (click)="navigateToLogin()"
                class="font-medium transition-colors inline-flex items-center"
              >
                  <svg width="22" height="12" viewBox="0 0 22 12" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M16.9987 6.66671H4.9987C4.62536 6.66671 4.33203 6.37337 4.33203 6.00004C4.33203 5.62671 4.62536 5.33337 4.9987 5.33337H16.9987C17.372 5.33337 17.6654 5.62671 17.6654 6.00004C17.6654 6.37337 17.372 6.66671 16.9987 6.66671Z" fill="#2C2D5B"/>
                    <path d="M8.33236 11.3333C8.24492 11.3344 8.15823 11.3171 8.07786 11.2827C7.99749 11.2482 7.92522 11.1973 7.86569 11.1333L3.19902 6.46663C2.93236 6.19996 2.93236 5.78663 3.19902 5.51996L7.86569 0.866626C8.13236 0.599959 8.54569 0.599959 8.81236 0.866626C9.07902 1.13329 9.07902 1.54663 8.81236 1.81329L4.61236 6.01329L8.81236 10.2133C9.07902 10.48 9.07902 10.8933 8.81236 11.16C8.67902 11.2933 8.50569 11.36 8.34569 11.36L8.33236 11.3333Z" fill="#2C2D5B"/>
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
export class OtpVerificationComponent implements OnInit {
  @ViewChildren('otpInput') otpInputs!: QueryList<ElementRef>;

  otpForm: FormGroup;
  maskedEmail: string = '';
  verificationType: string = 'registration';
  errorMessage: string = '';
  isLoading: boolean = false;
  isInvalid: boolean = false;
  isResendDisabled: boolean = false;
  resendCountdown: number = 0;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.otpForm = this.fb.group({
      digit1: ['', [Validators.required, Validators.pattern(/^\d$/)]],
      digit2: ['', [Validators.required, Validators.pattern(/^\d$/)]],
      digit3: ['', [Validators.required, Validators.pattern(/^\d$/)]],
      digit4: ['', [Validators.required, Validators.pattern(/^\d$/)]],
      digit5: ['', [Validators.required, Validators.pattern(/^\d$/)]],
      digit6: ['', [Validators.required, Validators.pattern(/^\d$/)]]
    });
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      const email = params['email'] || localStorage.getItem('verificationEmail');
      this.verificationType = params['type'] || 'registration';

      if (email) {
        this.maskedEmail = this.maskEmail(email);
      } else {
        this.maskedEmail = 'votre@email.com';
      }
    });
  }

  private maskEmail(email: string): string {
    const [localPart, domain] = email.split('@');
    if (localPart.length <= 2) {
      return `${localPart}***@${domain}`;
    }
    const maskedLocal = localPart.charAt(0) + '*'.repeat(localPart.length - 2) + localPart.charAt(localPart.length - 1);
    return `${maskedLocal}@${domain}`;
  }

  get isCodeComplete(): boolean {
    return this.otpForm.valid &&
      this.otpForm.get('digit1')?.value &&
      this.otpForm.get('digit2')?.value &&
      this.otpForm.get('digit3')?.value &&
      this.otpForm.get('digit4')?.value &&
      this.otpForm.get('digit5')?.value &&
      this.otpForm.get('digit6')?.value;
  }

  onDigitInput(event: any, index: number): void {
    const value = event.target.value;

    // Only allow digits
    if (!/^\d$/.test(value) && value !== '') {
      event.target.value = '';
      const digitControlName = `digit${index + 1}`;
      this.otpForm.get(digitControlName)?.setValue('');
      return;
    }

    this.errorMessage = '';
    this.isInvalid = false;

    // Move to next input if value is entered
    if (value && index < 5) {
      const nextInput = this.otpInputs.toArray()[index + 1];
      if (nextInput) {
        nextInput.nativeElement.focus();
      }
    }
  }

  onKeyDown(event: KeyboardEvent, index: number): void {
    const digitControlName = `digit${index + 1}`;

    // Handle backspace
    if (event.key === 'Backspace') {
      if (!this.otpForm.get(digitControlName)?.value) {
        // Move to previous input if current is empty
        if (index > 0) {
          const prevInput = this.otpInputs.toArray()[index - 1];
          if (prevInput) {
            prevInput.nativeElement.focus();
          }
        }
      } else {
        // Clear current input
        this.otpForm.get(digitControlName)?.setValue('');
      }
    }

    // Handle arrow keys
    if (event.key === 'ArrowLeft' && index > 0) {
      const prevInput = this.otpInputs.toArray()[index - 1];
      if (prevInput) {
        prevInput.nativeElement.focus();
      }
    }

    if (event.key === 'ArrowRight' && index < 5) {
      const nextInput = this.otpInputs.toArray()[index + 1];
      if (nextInput) {
        nextInput.nativeElement.focus();
      }
    }
  }

  onPaste(event: ClipboardEvent, index: number): void {
    event.preventDefault();
    const pastedData = event.clipboardData?.getData('text') || '';

    if (/^\d{6}$/.test(pastedData)) {
      // Valid 6-digit code
      for (let i = 0; i < 6; i++) {
        const digitControlName = `digit${i + 1}`;
        this.otpForm.get(digitControlName)?.setValue(pastedData[i]);

        const input = this.otpInputs.toArray()[i];
        if (input) {
          input.nativeElement.value = pastedData[i];
        }
      }

      // Focus on last input
      const lastInput = this.otpInputs.toArray()[5];
      if (lastInput) {
        lastInput.nativeElement.focus();
      }
    }
  }

  onFocus(event: FocusEvent, index: number): void {
    // Select all text when input is focused
    const input = event.target as HTMLInputElement;
    input.select();
  }

  getOtpCode(): string {
    return (this.otpForm.get('digit1')?.value || '') +
      (this.otpForm.get('digit2')?.value || '') +
      (this.otpForm.get('digit3')?.value || '') +
      (this.otpForm.get('digit4')?.value || '') +
      (this.otpForm.get('digit5')?.value || '') +
      (this.otpForm.get('digit6')?.value || '');
  }

  onSubmit(): void {
    if (this.isCodeComplete && !this.isLoading) {
      this.isLoading = true;
      this.errorMessage = '';

      const otpCode = this.getOtpCode();

      // Simulate API call
      setTimeout(() => {
        this.isLoading = false;

        // Simulate validation (replace with actual API call)
        if (otpCode === '123456') {
          // Success - redirect based on verification type
          if (this.verificationType === 'registration') {
            this.router.navigate(['/create-password']);
          } else {
            this.router.navigate(['/reset-password']);
          }
        } else {
          // Error
          this.errorMessage = 'Code de vérification incorrect. Veuillez réessayer.';
          this.isInvalid = true;

          // Clear inputs and focus first one
          this.otpForm.reset();
          this.otpInputs.first.nativeElement.focus();
        }
      }, 2000);
    }
  }

  resendCode(): void {
    if (!this.isResendDisabled) {
      this.isResendDisabled = true;
      this.resendCountdown = 60;

      // Simulate sending new code
      console.log('Nouveau code envoyé');

      // Start countdown
      const countdownInterval = setInterval(() => {
        this.resendCountdown--;
        if (this.resendCountdown <= 0) {
          this.isResendDisabled = false;
          clearInterval(countdownInterval);
        }
      }, 1000);
    }
  }

  navigateToLogin(): void {
    this.router.navigate(['/login']);
  }
}