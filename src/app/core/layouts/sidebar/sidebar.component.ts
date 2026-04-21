import { Component, Input, OnChanges, SimpleChanges, OnInit, Output, EventEmitter, PLATFORM_ID, Inject } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router, NavigationEnd, RouterModule } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../../shared/services/auth.service';
import { AdminService } from '../../../shared/services/admin.service';
import { environment } from '../../../../environments/environment';
import { MyAccountModalService } from '../../../shared/services/my-account-modal.service';
import { mapUserDetailsToDisplay } from '../../../shared/models/user-display.mapper';
import Swal from 'sweetalert2';
import { HttpErrorResponse } from '@angular/common/http';

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
  /** Sous-menu Offres (Codes promo / Promotions) ouvert ou non. */
  offresExpanded = false;
  /** Sous-menu Configuration ouvert ou non. */
  configExpanded = false;

  myAccountLoading = false;

  // Informations utilisateur affichées dans le profil
  displayName = 'Utilisateur';
  displayRoleLabel = 'Commercial';
  /** URL complète de la photo de profil (pour la sidebar), ou null si pas de photo. */
  displayProfilePhotoUrl: string | null = null;

  constructor(
    private router: Router,
    private authService: AuthService,
    private adminService: AdminService,
    private myAccountModalService: MyAccountModalService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) { }

  /** Mon compte : afficher « Modifier » pour admin (admin/users/edit) et pour RL / commercial (/profile/edit). */
  get allowSelfAccountEdit(): boolean {
    const r = this.normalizeRole(this.role);
    return r === 'admin' || r === 'log' || r === 'com';
  }

  /** Item sans enfants : lien direct. Item avec enfants : sous-menu (Offres). */
  private readonly menuItems: Array<{
    label: string;
    icon: string;
    link?: string;
    active?: boolean;
    children?: Array<{ label: string; link: string; active?: boolean }>;
  }> = [
    { label: 'Tableau de bord', icon: 'dashboard', link: '/com/dashboard', active: false },
    { label: 'Prospections', icon: 'prospection', link: '/com/prospections', active: false },
    { label: 'Entreprises', icon: 'entreprises', link: '/com/entreprises', active: false },
    {
      label: 'Offres',
      icon: 'promotion',
      active: false,
      children: [
        { label: 'Codes promo', link: '/com/promotions', active: false },
        { label: 'Promotions', link: '/com/promotions-produits', active: false },
      ],
    },

    { label: 'Tableau de bord', icon: 'dashboard', link: '/log/dashboardlog', active: false },
    { label: 'Commandes Fournisseurs', icon: 'fournisseurs', link: '/log/fournisseurs', active: false },
    { label: 'Gestion des Commandes', icon: 'commandes', link: '/log/commandes', active: false },
    { label: 'Gestion des Stocks', icon: 'stocks', link: '/log/stocks', active: false },
    { label: 'Planification Livraisons', icon: 'livraisons', link: '/log/livraisons', active: false },
    { label: 'Gestion des Retours', icon: 'retours', link: '/log/retours', active: false },


    { label: 'Tableau de bord', icon: 'dashboard', link: '/admin/dashboardadmin', active: false },
    { label: 'Utilisateurs', icon: 'users', link: '/admin/users', active: false },
    { label: 'Catalogue', icon: 'catalogue', link: '/admin/catalogue', active: false },
    { label: 'Catégories', icon: 'categories', link: '/admin/categories', active: false },
    {
      label: 'Configuration',
      icon: 'settings',
      active: false,
      children: [
        { label: 'Options livraison', link: '/admin/config/delivery-options', active: false },
        { label: 'Frais', link: '/admin/config/fees', active: false },
        { label: 'Types réclamation', link: '/admin/config/claim-types', active: false },
        { label: 'Motifs incidents (Livreur)', link: '/admin/config/delivery-reasons', active: false },
        { label: 'Motifs incidents (Salarié)', link: '/admin/config/employee-reasons', active: false },
        { label: 'Secteurs d\'activité', link: '/admin/config/activity-sectors', active: false },
      ],
    },
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

    // Charger les infos utilisateur stockées (uniquement en navigateur, pas en SSR)
    if (isPlatformBrowser(this.platformId)) {
      this.loadUserFromStorage();
      this.refreshProfileFromApi();
    }
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
      if (item.children) {
        item.children.forEach(child => {
          child.active = currentUrl === child.link || currentUrl.startsWith(child.link + '/');
        });
        item.active = item.children.some(c => c.active);
        if (item.active) {
          if (item.icon === 'promotion') this.offresExpanded = true;
          if (item.icon === 'settings') this.configExpanded = true;
        }
      } else if (item.link) {
        item.active = currentUrl === item.link || currentUrl.startsWith(item.link + '/');
      }
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
    this.filteredMenuItems = this.menuItems.filter(item =>
      item.link ? item.link.startsWith(prefix) : (item.children?.some(c => c.link.startsWith(prefix)) ?? false)
    );
    this.updateActiveState();
  }

  // Toggle mobile menu
  toggleMobile() {
    this.closeSidebar.emit();
  }

  // Toggle user menu
  toggleUserMenu(event?: Event) {
    event?.stopPropagation();
    this.userMenuOpen = !this.userMenuOpen;
  }

  openMyAccountModal(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.userMenuOpen = false;
    const token = sessionStorage.getItem('token') || localStorage.getItem('token') || '';
    if (!token) return;

    this.myAccountLoading = true;
    this.authService.getCurrentUserProfile().subscribe({
      next: (profile) => {
        const user = mapUserDetailsToDisplay(profile, (p) => this.adminService.getProfilePhotoUrl(p));
        this.myAccountModalService.open(user, this.allowSelfAccountEdit);
        this.myAccountLoading = false;
      },
      error: (err: unknown) => {
        this.myAccountLoading = false;
        const status = err instanceof HttpErrorResponse ? err.status : 0;
        const title =
          status === 403
            ? 'Accès refusé'
            : status === 401
              ? 'Session expirée'
              : 'Impossible de charger le profil';
        const text =
          status === 403
            ? 'Le serveur a refusé l’accès à votre profil (403). Vérifiez que vous utilisez la bonne API (HTTPS) et que le compte a les droits attendus.'
            : status === 401
              ? 'Reconnectez-vous pour afficher votre compte.'
              : 'Une erreur réseau ou serveur est survenue. Réessayez plus tard.';
        Swal.fire({ title, text, icon: 'warning' });
      },
    });
  }

  toggleOffres() {
    this.offresExpanded = !this.offresExpanded;
  }

  toggleConfig() {
    this.configExpanded = !this.configExpanded;
  }

  // Close all menus
  closeMenus() {
    this.closeSidebar.emit();
    this.userMenuOpen = false;
  }

  /** Si l'image de profil ne charge pas (404, CORS, etc.), afficher le placeholder. */
  onProfileImageError(): void {
    this.displayProfilePhotoUrl = null;
  }

  logout() {
    if (!isPlatformBrowser(this.platformId)) {
      this.finishLogout();
      return;
    }
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

  /** Au chargement, si l'utilisateur est connecté, recharge le profil depuis l'API pour afficher la photo et les infos à jour (ex. après rechargement de page). */
  private refreshProfileFromApi(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    const token = sessionStorage.getItem('token') || localStorage.getItem('token') || '';
    if (!token) return;

    this.authService.getCurrentUserProfile().subscribe({
      next: (profile) => {
        const fullName = `${profile.firstName ?? ''} ${profile.lastName ?? ''}`.trim();
        const emailName = profile.email ? profile.email.split('@')[0] : '';
        this.displayName = fullName || emailName || 'Utilisateur';
        this.displayRoleLabel = profile.roleLabel ?? profile.role ?? '';

        const rawUrl = profile.profilePhotoUrl?.trim() ?? '';
        if (rawUrl) {
          const base = (environment as { imageServerUrl?: string }).imageServerUrl?.replace(/\/$/, '') ?? '';
          this.displayProfilePhotoUrl = base ? `${base}/files/${rawUrl}` : `/files/${rawUrl}`;
        } else {
          this.displayProfilePhotoUrl = null;
        }

        // Persister dans les deux storages pour que loadUserFromStorage retrouve les infos au prochain rechargement
        const roleVal = profile.roleLabel ?? profile.role ?? '';
        [sessionStorage, localStorage].forEach(storage => {
          if (profile.firstName != null) storage.setItem('firstName', profile.firstName);
          if (profile.lastName != null) storage.setItem('lastName', profile.lastName);
          if (profile.email != null) storage.setItem('email', profile.email);
          if (roleVal) storage.setItem('role', roleVal);
          if (rawUrl) storage.setItem('profilePhotoUrl', rawUrl);
        });
      },
      error: () => { /* garder les valeurs du storage en cas d'erreur */ }
    });
  }

  // Charger le nom/prénom et le rôle depuis le storage (appelé uniquement en navigateur)
  private loadUserFromStorage(): void {
    if (!isPlatformBrowser(this.platformId)) return;
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

    const profilePhotoUrl =
      sessionStorage.getItem('profilePhotoUrl') ||
      localStorage.getItem('profilePhotoUrl') ||
      '';
    if (profilePhotoUrl.trim()) {
      const base = (environment as { imageServerUrl?: string }).imageServerUrl?.replace(/\/$/, '') ?? '';
      this.displayProfilePhotoUrl = base ? `${base}/files/${profilePhotoUrl}` : `/files/${profilePhotoUrl}`;
    } else {
      this.displayProfilePhotoUrl = null;
    }
  }

  // Nettoyer la session et rediriger
  private finishLogout(): void {
    if (isPlatformBrowser(this.platformId)) {
      sessionStorage.removeItem('token');
      sessionStorage.removeItem('role');
      sessionStorage.removeItem('firstName');
      sessionStorage.removeItem('lastName');
      sessionStorage.removeItem('email');
      sessionStorage.removeItem('otpEmail');
      sessionStorage.removeItem('verificationEmail');
      sessionStorage.removeItem('profilePhotoUrl');

      localStorage.removeItem('token');
      localStorage.removeItem('role');
      localStorage.removeItem('firstName');
      localStorage.removeItem('lastName');
      localStorage.removeItem('email');
      localStorage.removeItem('profilePhotoUrl');
    }

    this.closeMenus();
    this.router.navigate(['/login']);
  }
}