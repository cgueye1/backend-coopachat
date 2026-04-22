import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';

@Component({
  selector: 'app-entreprise-layout',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './entreprise-layout.component.html',
  styleUrls: []
})
export class EntrepriseLayoutComponent {
  companyName = 'SENICO DAKAR';
  companyInitials = 'SD';
  contactName = 'Fatou Traoré';

  menuItems = [
    { label: 'Tableau de bord', icon: 'dashboard', link: '/entreprise/dashboard' },
    { label: 'Mes salariés',    icon: 'people',    link: '/entreprise/salaries' },
    { label: 'Mes commandes',   icon: 'orders',    link: '/entreprise/commandes' },
    { label: 'Mon profil',      icon: 'profile',   link: '/entreprise/profil' },
  ];

  constructor(private router: Router) {}

  isActive(link: string): boolean {
    return this.router.url === link || this.router.url.startsWith(link + '/');
  }

  logout(): void {
    sessionStorage.clear();
    localStorage.clear();
    this.router.navigate(['/login']);
  }
}
