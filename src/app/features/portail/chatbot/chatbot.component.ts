import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-chatbot',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './chatbot.component.html',
  styles: [`
    :host {
      display: block;
      position: fixed;
      bottom: 4rem;
      right: 4rem;
      z-index: 50;
    }
  `]
})
export class ChatbotComponent {
  @Input() isOpen = false;

  messages = [
    { from: 'bot', text: ' Bonjour et bienvenue chez Coop Achat 😋' },
    { from: 'bot', text: 'Petite question pour mieux vous servir : êtes-vous un patient, médecin ou un Centre de santé?\n\nÇa nous aidera à personnaliser notre assistance.' }
  ];

  toggle() {
    this.isOpen = !this.isOpen;
  }
}
