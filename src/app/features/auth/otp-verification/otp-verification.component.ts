import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthLayoutComponent } from '../auth-layout/auth-layout.component';
import { NgOtpInputModule, NgOtpInputConfig } from 'ng-otp-input';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-otp-verification',
  standalone: true,
  imports: [CommonModule, AuthLayoutComponent, NgOtpInputModule],
  templateUrl: './otp-verification.component.html',
  styleUrls: ['./otp-verification.component.css']
})
export class OtpVerificationComponent implements OnInit {
  @ViewChild('ngOtpInput') ngOtpInputRef: any;

  maskedEmail: string = '';
  verificationType: string = 'registration';
  errorMessage: string = '';
  isLoading: boolean = false;
  isInvalid: boolean = false;
  isResendDisabled: boolean = false;
  resendCountdown: number = 0;
  otpValue: string = '';

  otpConfig: NgOtpInputConfig = {
    length: 6,
    isPasswordInput: false,
    disableAutoFocus: false,
    placeholder: '',
    allowNumbersOnly: true,
    inputStyles: {
      'width': '48px',
      'height': '48px',
      'border': '1px solid #D1D5DB',
      'border-radius': '16px',
      'text-align': 'center',
      'font-size': '1.25rem',
      'font-weight': '600',
      'color': '#374151',
      'outline': 'none',
      'transition': 'all 0.2s'
    },
    containerStyles: {
      'display': 'flex',
      'gap': '16px'
    }
  };

  constructor(
    private router: Router,
    private route: ActivatedRoute
  ) { }

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
    return this.otpValue.length === 6;
  }

  onOtpChange(otp: string): void {
    this.otpValue = otp;
    this.errorMessage = '';
    this.isInvalid = false;
  }

  onSubmit(): void {
    if (this.isCodeComplete && !this.isLoading) {
      this.isLoading = true;
      this.errorMessage = '';

      // Simulate API call
      setTimeout(() => {
        this.isLoading = false;

        // Simulate validation (replace with actual API call)
        if (this.otpValue === '123456') {
          // Success - show message then redirect
          this.showSuccessMessage();
        } else {
          // Error
          this.errorMessage = 'Code de vérification incorrect. Veuillez réessayer.';
          this.isInvalid = true;

          // Clear OTP input
          if (this.ngOtpInputRef) {
            this.ngOtpInputRef.setValue('');
          }
          this.otpValue = '';
        }
      }, 2000);
    }
  }

  showSuccessMessage(): void {
    Swal.fire({
      title: 'Vérification réussie',
      html: '<p style="color: #231F20; font-size: 18px; margin: 0;">Votre adresse email a été confirmée avec succès</p>',
      iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
      showConfirmButton: false,
      timer: 1500,
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-xl p-6',
        title: 'text-2xl font-semibold text-[#231F20]',
        icon: 'border-none'
      },
      backdrop: `rgba(0,0,0,0.2)`,
      width: '580px',
      showClass: {
        popup: 'animate__animated animate__fadeIn animate__faster'
      },
      hideClass: {
        popup: 'animate__animated animate__fadeOut animate__faster'
      }
    }).then(() => {
      // Redirect based on verification type
      if (this.verificationType === 'registration') {
        this.router.navigate(['/create-password']);
      } else {
        this.router.navigate(['/reset-password']);
      }
    });
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