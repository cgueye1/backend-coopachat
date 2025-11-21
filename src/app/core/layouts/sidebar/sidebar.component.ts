import { Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';

type Role = 'log' | 'com' | 'admin' | 'commercial';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sidebar.component.html',
  styleUrls: [],
})
export class SidebarComponent implements OnChanges {
  @Input() role: Role = 'log';

  mobileOpen = false;
  userMenuOpen = false;
  userMenuMobileOpen = false;

  private readonly menuItems = [
    { label: 'Tableau de bord', icon: 'icones/dashboard.svg', link: '/com/dashboard', active: false },
    { label: 'Prospections', icon: 'icones/prospection.svg', link: '/com/propection', active: false },
    { label: 'Gestion salariés', icon: 'icones/salaries.svg', link: '/com/salaries', active: false },
    { label: 'Statistiques', icon: 'icones/statistiques.svg', link: '/com/statistiques', active: false },
    { label: 'Promotions', icon: 'icones/promotion.svg', link: '/com/promotions', active: false },
    
    { label: 'Tableau de bord', icon: 'icones/dashboard.svg', link: '/log/dashboardlog', active: false },
    { label: 'Commandes Fournisseurs', icon: 'icones/fourn.svg', link: '/log/fournisseurs', active: false },
    { label: 'Gestion des Stocks', icon: 'icones/stocks.svg', link: '/log/stocks', active: false },
    { label: 'Planification Livraisons', icon: 'icones/livraison.svg', link: '/log/livraisons', active: false },
    { label: 'Gestion des Retours', icon: 'icones/retours.svg', link: '/log/retours', active: false },

    
    { label: 'Tableau de bord', icon: 'icones/dashboard.svg', link: '/admin/dashboardadmin', active: false },
    { label: 'Utilisateurs', icon: 'icones/users.svg', link: '/admin/users', active: false },
    { label: 'Catalogue', icon: 'icones/catalogue.svg', link: '/admin/catalogue', active: false },
    
  ];

  filteredMenuItems = this.menuItems.slice();
  roleLabel = 'Logistique';

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['role']) {
      this.applyRoleFilter();
    }
  }

  private normalizeRole(input: Role): 'log' | 'com' | 'admin' {
    if (!input) return 'log';
    if (input === 'commercial') return 'com';
    if (input === 'com' || input === 'log' || input === 'admin') return input;
    return 'log';
  }

  private applyRoleFilter(): void {
    const normalized = this.normalizeRole(this.role);
    switch (normalized) {
      case 'log': this.roleLabel = 'Logistique'; break;
      case 'com': this.roleLabel = 'Commercial'; break;
      case 'admin': this.roleLabel = 'Administrateur'; break;
      default: this.roleLabel = 'Logistique';
    }

    const prefix = normalized === 'com' ? '/com' : normalized === 'log' ? '/log' : '/admin';
    this.filteredMenuItems = this.menuItems.filter(item => item.link.startsWith(prefix));
  }

  // Méthodes pour mobile
  toggleMobile() {
    this.mobileOpen = !this.mobileOpen;
  }

  closeMobile() {
    this.mobileOpen = false;
  }

  // Méthodes pour menu utilisateur desktop
  toggleUserMenu() {
    this.userMenuOpen = !this.userMenuOpen;
    this.userMenuMobileOpen = false;
  }

  // Méthodes pour menu utilisateur mobile
  toggleUserMenuMobile() {
    this.userMenuMobileOpen = !this.userMenuMobileOpen;
    this.userMenuOpen = false;
  }

  // Fermer tous les menus utilisateur
  closeUserMenus() {
    this.userMenuOpen = false;
    this.userMenuMobileOpen = false;
  }

  // Actions du menu utilisateur
  goToProfile() {
    console.log('Redirection vers Mon compte');
    // Implémentez ici la navigation vers la page de profil
    // Par exemple : this.router.navigate(['/profile']);
    this.closeUserMenus();
  }

  logout() {
    console.log('Déconnexion de l\'utilisateur');
    // Implémentez ici la logique de déconnexion
    // Par exemple : this.authService.logout(); this.router.navigate(['/login']);
    this.closeUserMenus();
  }
}