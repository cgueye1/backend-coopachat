import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ModalComponent } from '../../ui/modal/modal.component';
import { FormFieldComponent } from '../../ui/form-field/form-field.component';

export interface EmployeeFormData {
  prenom: string;
  nom: string;
  email: string;
  telephone: string;
}

/** Utilisé par les écrans qui listent les entreprises (filtres), pas par la modale salarié. */
export interface Company {
  id: string;
  name: string;
}

@Component({
  selector: 'app-employee-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ModalComponent, FormFieldComponent],
  templateUrl: './employee-modal.component.html'
})
export class EmployeeModalComponent implements OnInit, OnChanges {
  @Input() isOpen = false;
  @Input() isSubmitting = false;
  @Input() title = "Inscrire un nouveau salarié";
  @Input() initialData: EmployeeFormData | null = null;
  @Output() close = new EventEmitter<void>();
  @Output() submit = new EventEmitter<EmployeeFormData>();

  employeeForm!: FormGroup;

  constructor(private fb: FormBuilder) {}

  ngOnInit() {
    this.initForm();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['initialData'] && this.employeeForm) {
      if (this.initialData) {
        this.employeeForm.patchValue(this.initialData);
      } else {
        this.employeeForm.reset();
      }
    }
  }

  private initForm() {
    this.employeeForm = this.fb.group({
      prenom: ['', [Validators.required, Validators.minLength(2)]],
      nom: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      telephone: ['', [Validators.required, Validators.pattern(/^[+]?[0-9\s\-\(\)]+$/)]]
    });
  }

  getFieldError(fieldName: string): string {
    const field = this.employeeForm.get(fieldName);
    if (field?.invalid && field?.touched) {
      if (field.errors?.['required']) {
        return 'Ce champ est requis';
      }
      if (field.errors?.['minlength']) {
        return `Minimum ${field.errors?.['minlength'].requiredLength} caractères`;
      }
      if (field.errors?.['email']) {
        return 'Format d\'email invalide';
      }
      if (field.errors?.['pattern'] && fieldName === 'telephone') {
        return 'Format de téléphone invalide';
      }
    }
    return '';
  }

  onClose() {
    this.employeeForm.reset();
    this.close.emit();
  }

  onSubmit() {
    if (this.employeeForm.valid) {
      this.submit.emit(this.employeeForm.value as EmployeeFormData);
    } else {
      Object.keys(this.employeeForm.controls).forEach((key) => {
        this.employeeForm.get(key)?.markAsTouched();
      });
    }
  }
}
