import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'ui-modal',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div *ngIf="isOpen" class="fixed inset-0 z-50 overflow-y-auto">
      <div class="flex min-h-screen items-center justify-center px-4 py-6">
        <!-- Overlay -->
        <div 
          class="fixed inset-0 bg-black bg-opacity-50 transition-opacity"
          (click)="onClose()"
        ></div>
        
        <!-- Modal -->
        <div class="relative w-full max-w-2xl transform rounded-lg bg-white shadow-xl transition-all">
          <!-- Header -->
          <div class="flex items-center justify-between border-b border-gray-200 px-6 py-4">
            <h2 class="text-lg font-semibold text-gray-900">{{ title }}</h2>
            <button 
              (click)="onClose()"
              class="rounded-md p-2 text-gray-400 hover:bg-gray-100 hover:text-gray-600"
            >
              <svg class="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
          
          <!-- Content -->
          <div class="px-6 py-6">
            <ng-content></ng-content>
          </div>
        </div>
      </div>
    </div>
  `
})
export class ModalComponent {
  @Input() isOpen = false;
  @Input() title = '';
  @Output() close = new EventEmitter<void>();

  onClose() {
    this.close.emit();
  }
}
