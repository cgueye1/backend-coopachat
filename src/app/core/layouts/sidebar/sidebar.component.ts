import { Component, Input, OnChanges, SimpleChanges, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';

type Role = 'log' | 'com' | 'admin' | 'commercial';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './sidebar.component.html',
  styleUrls: [],
})
export class SidebarComponent implements OnChanges, OnInit {
  @Input() role: Role = 'log';

  mobileOpen = false;
  userMenuOpen = false;
  userMenuMobileOpen = false;

  constructor(private router: Router) { }

  private readonly menuItems = [
    { label: 'Tableau de bord', icon: 'dashboard', link: '/com/dashboard', active: false },
    { label: 'Prospections', icon: 'prospection', link: '/com/propection', active: false },
    { label: 'Gestion salariés', icon: 'salaries', link: '/com/salaries', active: false },
    { label: 'Statistiques', icon: 'statistiques', link: '/com/statistiques', active: false },
    { label: 'Promotions', icon: 'promotion', link: '/com/promotions', active: false },

    { label: 'Tableau de bord', icon: 'dashboard', link: '/log/dashboardlog', active: false },
    { label: 'Commandes Fournisseurs', icon: 'fournisseurs', link: '/log/fournisseurs', active: false },
    { label: 'Gestion des Commandes', icon: 'commandes', link: '/log/commandes', active: false },
    { label: 'Gestion des Stocks', icon: 'stocks', link: '/log/stocks', active: false },
    { label: 'Planification Livraisons', icon: 'livraisons', link: '/log/livraisons', active: false },
    { label: 'Gestion des Retours', icon: 'retours', link: '/log/retours', active: false },


    { label: 'Tableau de bord', icon: 'dashboard', link: '/admin/dashboardadmin', active: false },
    { label: 'Utilisateurs', icon: 'users', link: '/admin/users', active: false },
    { label: 'Catalogue', icon: 'catalogue', link: '/admin/catalogue', active: false },

  ];

  filteredMenuItems = this.menuItems.slice();
  roleLabel = 'Logistique';

  ngOnInit(): void {
    // Mettre à jour l'état actif au chargement initial
    this.updateActiveState();

    // Écouter les changements de route
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        this.updateActiveState();
      });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['role']) {
      this.applyRoleFilter();
      this.updateActiveState();
    }
  }

  private updateActiveState(): void {
    const currentUrl = this.router.url;
    this.filteredMenuItems.forEach(item => {
      item.active = currentUrl === item.link || currentUrl.startsWith(item.link + '/');
    });
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
    this.updateActiveState();
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