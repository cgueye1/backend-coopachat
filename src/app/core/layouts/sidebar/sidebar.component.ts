import { Component, Input, OnChanges, SimpleChanges, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, NavigationEnd, RouterModule } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../../shared/services/auth.service';

type Role = 'log' | 'com' | 'admin' | 'commercial';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrls: [],
})
export class SidebarComponent implements OnChanges, OnInit {
  @Input() role: Role = 'log';
  @Input() isOpen: boolean = false;
  @Output() closeSidebar = new EventEmitter<void>();

  userMenuOpen = false;

  // Informations utilisateur affichées dans le profil
  displayName = 'Utilisateur';
  displayRoleLabel = 'Commercial';

  constructor(
    private router: Router,
    private authService: AuthService
  ) { }

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

    // Charger les infos utilisateur stockées
    this.loadUserFromStorage();
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

  // Toggle mobile menu
  toggleMobile() {
    this.closeSidebar.emit();
  }

  // Toggle user menu
  toggleUserMenu() {
    this.userMenuOpen = !this.userMenuOpen;
  }

  // Close all menus
  closeMenus() {
    this.closeSidebar.emit();
    this.userMenuOpen = false;
  }

  // Actions du menu utilisateur
  goToProfile() {
    console.log('Redirection vers Mon compte');
    // Implémentez ici la navigation vers la page de profil
    // Par exemple : this.router.navigate(['/profile']);
    this.closeMenus();
  }

  logout() {
    const token =
      sessionStorage.getItem('token') ||
      localStorage.getItem('token') ||
      '';

    if (token) {
      this.authService.logout(token).subscribe({
        next: () => this.finishLogout(),
        error: () => this.finishLogout()
      });
    } else {
      this.finishLogout();
    }
  }

  // Charger le nom/prénom et le rôle depuis le storage
  private loadUserFromStorage(): void {
    const firstName =
      sessionStorage.getItem('firstName') ||
      localStorage.getItem('firstName') ||
      '';
    const lastName =
      sessionStorage.getItem('lastName') ||
      localStorage.getItem('lastName') ||
      '';
    const email =
      sessionStorage.getItem('email') ||
      localStorage.getItem('email') ||
      '';
    const role =
      sessionStorage.getItem('role') ||
      localStorage.getItem('role') ||
      '';

    const fullName = `${firstName} ${lastName}`.trim();
    const emailName = email ? email.split('@')[0] : '';
    this.displayName = fullName || emailName || 'Utilisateur';
    this.displayRoleLabel = role || 'Commercial';
  }

  // Nettoyer la session et rediriger
  private finishLogout(): void {
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('role');
    sessionStorage.removeItem('firstName');
    sessionStorage.removeItem('lastName');
    sessionStorage.removeItem('email');
    sessionStorage.removeItem('otpEmail');
    sessionStorage.removeItem('verificationEmail');

    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('firstName');
    localStorage.removeItem('lastName');
    localStorage.removeItem('email');

    this.closeMenus();
    this.router.navigate(['/login']);
  }
}