import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

type Role = 'log' | 'com' | 'admin';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="bg-white border-b border-gray-200 px-6 py-4 shadow-sm">
      <div class="flex items-center justify-end">
        <div class="flex items-center space-x-4">
          <input placeholder="rechercher" type="text" class="w-[320px] h-[38px] border-[1px] rounded-[6px]" />
          <div class="flex items-center text-sm text-gray-500">
            <svg class="mr-2" width="25" height="24" viewBox="0 0 25 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M11.1259 21C11.3014 21.304 11.5539 21.5565 11.8579 21.732C12.1619 21.9075 12.5068 21.9999 12.8579 21.9999C13.2089 21.9999 13.5538 21.9075 13.8578 21.732C14.1618 21.5565 14.4143 21.304 14.5899 21" stroke="#9CA3AF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M4.11991 15.326C3.98928 15.4692 3.90307 15.6472 3.87177 15.8385C3.84047 16.0298 3.86543 16.226 3.94362 16.4034C4.02181 16.5807 4.14985 16.7316 4.31217 16.8375C4.47449 16.9434 4.66409 16.9999 4.85791 17H20.8579C21.0517 17.0001 21.2414 16.9438 21.4038 16.8381C21.5662 16.7324 21.6944 16.5817 21.7728 16.4045C21.8512 16.2273 21.8764 16.0311 21.8454 15.8398C21.8143 15.6485 21.7283 15.4703 21.5979 15.327C20.2679 13.956 18.8579 12.499 18.8579 8C18.8579 6.4087 18.2258 4.88258 17.1006 3.75736C15.9753 2.63214 14.4492 2 12.8579 2C11.2666 2 9.74049 2.63214 8.61527 3.75736C7.49005 4.88258 6.85791 6.4087 6.85791 8C6.85791 12.499 5.44691 13.956 4.11991 15.326Z" stroke="#9CA3AF" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>

            <!-- affiche le rôle lisible -->
            {{ roleLabel }}
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .fa, .fas {
      font-family: "Inter";
      font-weight: 900;
    }

    .fa-bell::before { content: "🔔"; }
    .fa-plus::before { content: "+"; }
  `]
})
export class HeaderComponent {
  /** rôle reçu depuis le main layout (normalisé : 'log'|'com'|'admin') */
  @Input() role: Role = 'log';

  get roleLabel(): string {
    switch (this.role) {
      case 'log': return 'Resp. Logistique';
      case 'com': return 'Commercial';
      case 'admin': return 'Administrateur';
      default: return 'Resp. Logistique';
    }
  }
}
