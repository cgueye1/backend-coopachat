import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeaderComponent } from '../header/header.component';
import { SidebarComponent } from '../sidebar/sidebar.component';

type Role = 'log' | 'com' | 'admin' | 'commercial';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, HeaderComponent, SidebarComponent],
  templateUrl: './main-layout.component.html',
  styles: [`
    :host {
      display: block;
      min-height: 100vh;
    }
  `]
})
export class MainLayoutComponent {
  @Input() role?: Role;

  /** Normalisation du rôle */
  get normalizedRole(): 'log' | 'com' | 'admin' {
    if (!this.role) return 'log';
    if (this.role === 'commercial') return 'com';
    if (this.role === 'com' || this.role === 'log' || this.role === 'admin') return this.role;
    return 'log';
  }
}
