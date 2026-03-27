import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ModalComponent } from '../../ui/modal/modal.component';
import { FormFieldComponent } from '../../ui/form-field/form-field.component';

/** Données envoyées au parent pour création (code promo panier uniquement). */
export interface CouponFormData {
  name: string;
  code: string;
  discountType: 'PERCENTAGE' | 'FIXED_AMOUNT';
  value: number;
  startDate: string;
  endDate: string;
}

@Component({
  selector: 'app-coupon-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ModalComponent,
    FormFieldComponent
  ],
  templateUrl: './coupon-modal.component.html'
})
export class CouponModalComponent implements OnInit {
  @Input() isOpen = false;
  @Input() isSubmitting = false;
  @Output() close = new EventEmitter<void>();
  @Output() submitCoupon = new EventEmitter<CouponFormData>();

  couponForm: FormGroup;
  dateRangeError: string | null = null;

  readonly DISCOUNT_TYPE = { PERCENTAGE: 'PERCENTAGE', FIXED_AMOUNT: 'FIXED_AMOUNT' };

  constructor(private fb: FormBuilder) {
    this.couponForm = this.fb.group({
      name: ['', Validators.required],
      code: ['', Validators.required],
      discountType: [this.DISCOUNT_TYPE.PERCENTAGE, Validators.required],
      value: [null as number | null, [Validators.required, Validators.min(0.01)]],
      dateDebut: ['', Validators.required],
      dateFin: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.couponForm.get('dateDebut')?.valueChanges.subscribe(() => this.validateDateRange());
    this.couponForm.get('dateFin')?.valueChanges.subscribe(() => this.validateDateRange());
  }

  get isFixedAmount(): boolean {
    return this.couponForm.get('discountType')?.value === this.DISCOUNT_TYPE.FIXED_AMOUNT;
  }

  validateDateRange(): void {
    const start = this.couponForm.get('dateDebut')?.value;
    const end = this.couponForm.get('dateFin')?.value;
    if (start && end && new Date(start) > new Date(end)) {
      this.dateRangeError = 'La date de début doit être antérieure à la date de fin.';
    } else {
      this.dateRangeError = null;
    }
  }

  getFieldError(fieldName: string): string {
    const field = this.couponForm.get(fieldName);
    if (!field?.invalid || !field?.touched) return '';
    if (field.errors?.['required']) return 'Ce champ est requis';
    if (field.errors?.['min']) return 'La valeur doit être positive';
    return '';
  }

  onClose(): void {
    this.couponForm.reset({
      name: '',
      code: '',
      discountType: this.DISCOUNT_TYPE.PERCENTAGE,
      value: null,
      dateDebut: '',
      dateFin: ''
    });
    this.couponForm.get('code')?.markAsPristine();
    this.dateRangeError = null;
    this.close.emit();
  }

  onSubmit(): void {
    if (this.couponForm.invalid || this.dateRangeError) {
      this.couponForm.markAllAsTouched();
      return;
    }
    const start = this.couponForm.get('dateDebut')?.value;
    const end = this.couponForm.get('dateFin')?.value;
    const payload: CouponFormData = {
      name: this.couponForm.get('name')?.value?.trim() ?? '',
      code: this.couponForm.get('code')?.value?.trim().toUpperCase() ?? '',
      discountType: this.couponForm.get('discountType')?.value,
      value: Number(this.couponForm.get('value')?.value),
      startDate: start ? new Date(start).toISOString().slice(0, 10) + 'T00:00:00.000Z' : '',
      endDate: end ? new Date(end).toISOString().slice(0, 10) + 'T23:59:59.999Z' : ''
    };
    this.submitCoupon.emit(payload);
  }
}
