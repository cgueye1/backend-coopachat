import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';

export type NotificationStatus = 'success' | 'error' | 'warning' | 'info';
export type ButtonType = 'single' | 'multiple';
export type ButtonAction = 'primary' | 'secondary';

export interface ButtonConfig {
  type: ButtonType;
  primaryText: string;
  secondaryText?: string; // Requis seulement pour le type 'multiple'
}

@Component({
  selector: 'app-notification-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notification-modal.component.html'
})
export class NotificationModalComponent implements OnChanges {
  @Input() isVisible: boolean = false;
  @Input() status: NotificationStatus = 'info';
  @Input() title: string = '';
  @Input() description?: string;
  @Input() buttonConfig?: ButtonConfig;
  @Input() showCloseButton: boolean = true;
  @Input() closeOnOverlayClick: boolean = true;

  @Output() closed = new EventEmitter<void>();
  @Output() buttonClicked = new EventEmitter<ButtonAction>();

  ngOnChanges(changes: SimpleChanges): void {
    // Vous pouvez ajouter de la logique ici si nécessaire
    // par exemple, auto-fermeture après un délai pour les succès
    if (changes['isVisible'] && this.isVisible && this.status === 'success') {
      // Auto-close après 3 secondes pour les notifications de succès
      // setTimeout(() => {
      //   this.onClose();
      // }, 3000);
    }
  }

  onClose(): void {
    this.closed.emit();
  }

  onOverlayClick(): void {
    if (this.closeOnOverlayClick) {
      this.onClose();
    }
  }

  onButtonClick(action: ButtonAction): void {
    this.buttonClicked.emit(action);
  }

  getButtonClasses(buttonType: 'primary' | 'secondary'): string {
    if (buttonType === 'secondary') {
      return 'bg-gray-200 text-gray-700 hover:bg-gray-300';
    }

    // Classes pour le bouton primary selon le status
    const baseClasses = 'text-white ';
    
    switch (this.status) {
      case 'success':
        return baseClasses + 'bg-green-500 hover:bg-green-600';
      case 'error':
        return baseClasses + 'bg-red-500 hover:bg-red-600';
      case 'warning':
        return baseClasses + 'bg-orange-500 hover:bg-orange-600';
      case 'info':
        return baseClasses + 'bg-blue-500 hover:bg-blue-600';
      default:
        return baseClasses + 'bg-[#D4B036] hover:bg-yellow-600';
    }
  }
}

// Service pour gérer les notifications de manière globale (optionnel)
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface NotificationData {
  status: NotificationStatus;
  title: string;
  description?: string;
  buttonConfig?: ButtonConfig;
  showCloseButton?: boolean;
  closeOnOverlayClick?: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private notificationSubject = new BehaviorSubject<NotificationData | null>(null);
  public notification$ = this.notificationSubject.asObservable();

  showNotification(data: NotificationData): void {
    this.notificationSubject.next(data);
  }

  hideNotification(): void {
    this.notificationSubject.next(null);
  }

  // Méthodes de convenance
  showSuccess(title: string, description?: string, buttonConfig?: ButtonConfig): void {
    this.showNotification({
      status: 'success',
      title,
      description,
      buttonConfig
    });
  }

  showError(title: string, description?: string, buttonConfig?: ButtonConfig): void {
    this.showNotification({
      status: 'error',
      title,
      description,
      buttonConfig
    });
  }

  showWarning(title: string, description?: string, buttonConfig?: ButtonConfig): void {
    this.showNotification({
      status: 'warning',
      title,
      description,
      buttonConfig
    });
  }

  showInfo(title: string, description?: string, buttonConfig?: ButtonConfig): void {
    this.showNotification({
      status: 'info',
      title,
      description,
      buttonConfig
    });
  }

  // Notifications spécifiques selon vos images
  showPasswordReset(): void {
    this.showSuccess(
      'Mot de passe réinitialisé avec succès',
      'Votre mot de passe a été modifié. Vous pouvez maintenant vous connecter avec votre nouveau mot de passe.',
      {
        type: 'single',
        primaryText: 'Cliquez ici pour vous connecter'
      }
    );
  }

  showRegistrationSuccess(): void {
    this.showSuccess(
      'Inscription réussie !',
      'Votre compte a été créé avec succès.'
    );
  }

  showDeleteConfirmation(): void {
    this.showWarning(
      'Suppression',
      'Êtes-vous sûr de vouloir supprimer cet employé ?',
      {
        type: 'multiple',
        primaryText: 'Supprimer',
        secondaryText: 'Annuler'
      }
    );
  }
}