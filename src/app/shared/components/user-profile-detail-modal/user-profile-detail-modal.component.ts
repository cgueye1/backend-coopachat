import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserDisplay } from '../../models/user-display.model';

@Component({
  selector: 'app-user-profile-detail-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-profile-detail-modal.component.html',
})
export class UserProfileDetailModalComponent {
  @Input() visible = false;
  @Input() user: UserDisplay | null = null;
  /** admin : bascule statut + Modifier ; self : compte connecté, sans activation/désactivation */
  @Input() variant: 'admin' | 'self' = 'self';
  /** En mode self, afficher « Modifier » (ex. administrateur → /admin/users/edit) */
  @Input() allowSelfEdit = false;

  @Output() closed = new EventEmitter<void>();
  @Output() modify = new EventEmitter<void>();
  @Output() toggleStatus = new EventEmitter<void>();

  onBackdropClick(): void {
    this.closed.emit();
  }

  stopPropagation(event: Event): void {
    event.stopPropagation();
  }

  onModify(): void {
    this.modify.emit();
  }

  onAnnuler(): void {
    this.closed.emit();
  }

  onToggleStatus(): void {
    this.toggleStatus.emit();
  }
}
