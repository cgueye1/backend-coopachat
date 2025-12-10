import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'ui-form-field',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  host: {
    class: 'contents'
  },
  templateUrl: './form-field.component.html'
})
export class FormFieldComponent {
  @Input() label = '';
  @Input() type?: 'text' | 'email' | 'tel' | 'date' | 'select' | 'textarea';
  @Input() placeholder = '';
  @Input() required = false;
  @Input() error = '';
  @Input() control!: AbstractControl;
  @Input() rows = 1;
  @Input() options?: { value: string, label: string }[] = [];

  getMinDate(): string {
    const today = new Date();
    const year = today.getFullYear();
    const month = String(today.getMonth() + 1).padStart(2, '0');
    const day = String(today.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}