import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'ui-form-field',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="space-y-1">
      <label class="block text-sm font-medium text-gray-700">
        {{ label }}
        <span *ngIf="required" class="text-red-500">*</span>
      </label>
      <ng-content></ng-content>
      <div *ngIf="error" class="text-sm text-red-600">{{ error }}</div>
    </div>
  `
})
export class FormFieldComponent {
  @Input() label = '';
  @Input() required = false;
  @Input() error = '';
}