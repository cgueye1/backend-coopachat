import { Component, ElementRef, QueryList, ViewChildren, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthLayoutComponent } from '../auth-layout/auth-layout.component';

@Component({
  selector: 'app-otp-verification',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AuthLayoutComponent],
  templateUrl: './otp-verification.component.html',
  styleUrls: ['./otp-verification.component.css']
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