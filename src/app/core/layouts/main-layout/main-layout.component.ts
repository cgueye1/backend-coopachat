import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HeaderComponent } from '../header/header.component';
import { SidebarComponent } from '../sidebar/sidebar.component';

type Role = 'log' | 'com' | 'admin' | 'commercial';

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [CommonModule, HeaderComponent, SidebarComponent],
  template: `
    <div class="flex flex-col min-h-screen bg-[#F5F5F3]">
      <!-- Sidebar : on passe le role à la sidebar -->
      <app-sidebar class="lg:fixed lg:top-0 lg:left-0 lg:h-full" [role]="normalizedRole"></app-sidebar>

      <!-- Main Content Area -->
      <div class="flex-1 flex flex-col lg:ml-64 transition-all duration-300">
        <!-- Header : visible uniquement pour le rôle commercial -->
        <app-header 
          *ngIf="normalizedRole === 'com'"
          class="sticky top-0 z-10"
          [role]="normalizedRole">
        </app-header>

        <!-- Page Content -->
        <main class="flex-1 overflow-y-auto">
          <div class="p-4 sm:p-6">
            <ng-content></ng-content>
          </div>
        </main>
      </div>
    </div>
  `,
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
