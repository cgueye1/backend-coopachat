import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

type ButtonVariant = 'primary' | 'secondary' | 'danger';

@Component({
  selector: 'ui-button',
  standalone: true,
  imports: [CommonModule],
  template: `
    <button
      [type]="type"
      [disabled]="disabled"
      (click)="onClick()"
      [class]="buttonClasses"
    >
      <ng-content></ng-content>
    </button>
  `
})
export class ButtonComponent {
  @Input() type: 'button' | 'submit' | 'reset' = 'button';
  @Input() variant: ButtonVariant = 'primary';
  @Input() disabled = false;
  @Input() fullWidth = false;
  @Output() click = new EventEmitter<void>();

 // button.component.ts (extrait)
get buttonClasses(): string {
  const baseClasses = 'inline-flex justify-center rounded-md px-4 py-2 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed transition-colors';
  const widthClass = this.fullWidth ? 'w-full' : '';
  
  const variantClasses = {
    primary: 'bg-indigo-800 text-white hover:bg-indigo-900 focus:ring-indigo-500',
    secondary: 'bg-gray-100 text-gray-700 border border-gray-300 hover:bg-gray-200 focus:ring-indigo-500',
    danger: 'bg-red-600 text-white hover:bg-red-700 focus:ring-red-500'
  };

  return `${baseClasses} ${widthClass} ${variantClasses[this.variant]}`;
}

  onClick() {
    if (!this.disabled) {
      this.click.emit();
    }
  }
}