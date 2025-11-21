import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';

@Component({
  selector: 'app-admin-page',
  standalone: true,
  imports: [CommonModule, MainLayoutComponent],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class AdminPageComponent {
  // Définir le rôle comme 'admin' pour utiliser le sidebar admin
  role: 'admin' = 'admin';
  
  constructor() {
    // Initialisations si nécessaire
  }

  // Méthodes du composant peuvent être ajoutées ici
  onNewAction(): void {
    console.log('Nouvelle action déclenchée');
  }

  onSettingsClick(): void {
    console.log('Paramètres cliqués');
  }
}