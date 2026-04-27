import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin, finalize } from 'rxjs';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import Swal from 'sweetalert2';
import { PAGE_SIZE_OPTIONS } from '../../../shared/constants/pagination';
import {
  AdminService,
  UserListItemDTO,
  UserListResponseDTO,
  UserStatsDTO,
  UserDetailsDTO
} from '../../../shared/services/admin.service';
import { UserDisplay } from '../../../shared/models/user-display.model';
import { mapUserDetailsToDisplay } from '../../../shared/models/user-display.mapper';
import { UserProfileDetailModalComponent } from '../../../shared/components/user-profile-detail-modal/user-profile-detail-modal.component';
import { HttpErrorResponse } from '@angular/common/http';

interface MetricCard {
  title: string;
  value: string;
  icon: string;
}

/** Map libellé rôle → enum backend pour le filtre. */
const ROLE_LABEL_TO_ENUM: Record<string, string> = {
  'Administrateur': 'ADMINISTRATOR',
  'Commercial': 'COMMERCIAL',
  'Livreur': 'DELIVERY_DRIVER',
  'Responsable Logistique': 'LOGISTICS_MANAGER'
};

/** Salariés gérés par le commercial, pas listés / filtrés côté admin. */
const ROLES_FOR_FILTER = ['Toutes les rôles', 'Administrateur', 'Commercial', 'Livreur', 'Responsable Logistique'];

function formatDate(v: string | { day?: number; month?: number; year?: number } | unknown): string {
  if (typeof v === 'string') {
    const d = new Date(v);
    if (!isNaN(d.getTime())) {
      return d.toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit', year: 'numeric' });
    }
    return v;
  }
  return '';
}

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [MainLayoutComponent, HeaderComponent, CommonModule, FormsModule, UserProfileDetailModalComponent],
  templateUrl: './users.component.html',
  styles: ``
})
export class UsersComponent implements OnInit, OnDestroy {
  searchText = '';
  selectedRole = 'Toutes les rôles';
  selectedStatut = 'Tous les statuts';
  showRoleDropdown = false;
  showStatutDropdown = false;
  currentPage = 1;
  pageSize = 10;
  pageSizeOptions = PAGE_SIZE_OPTIONS;
  totalPages = 1;
  totalElements = 0;
  showUserModal = false;
  selectedUser: UserDisplay | null = null;
  users: UserDisplay[] = [];
  metricsData: MetricCard[] = [];
  loading = false;
  loadingExport = false;
  toggleLoadingId: number | null = null;
  private searchDebounceTimer: ReturnType<typeof setTimeout> | null = null;
  private refreshInterval: ReturnType<typeof setInterval> | null = null;
  private readonly REFRESH_INTERVAL_MS = 30000; //on raffraichît la page chaque 30s

  constructor(
    private router: Router,
    private adminService: AdminService,
    @Inject(PLATFORM_ID) private platformId: object
  ) {}

  onSearchInput() {
    if (this.searchDebounceTimer != null) clearTimeout(this.searchDebounceTimer);
    this.searchDebounceTimer = setTimeout(() => {
      this.searchDebounceTimer = null;
      this.applyFilters();
    }, 300);
  }

  ngOnInit() {
    this.loadInitialDataInParallel();
    // Auto-refresh : recharge la liste et les stats toutes les 30 secondes (côté navigateur uniquement)
    if (isPlatformBrowser(this.platformId)) {
      this.refreshInterval = setInterval(() => {
        this.loadUsers();
        this.loadStats();
      }, this.REFRESH_INTERVAL_MS);
    }
  }

  ngOnDestroy() {
    // Nettoyage de l'intervalle pour éviter les fuites mémoire, l'intervalle est stoppé dès que l'admin quitte la page pour éviter les appels inutiles 
    if (this.refreshInterval !== null) {
      clearInterval(this.refreshInterval);
      this.refreshInterval = null;
    }
  }

  /** Stats + liste utilisateurs (graphiques sur le tableau de bord admin). */
  private loadInitialDataInParallel() {
    this.loading = true;
    const roleParam = this.selectedRole === 'Toutes les rôles' ? undefined : ROLE_LABEL_TO_ENUM[this.selectedRole];
    const statusParam = this.selectedStatut === 'Tous les statuts' ? undefined : this.selectedStatut === 'Actif';

    forkJoin({
      stats: this.adminService.getUsersStats(),
      users: this.adminService.getUsers({
        page: this.currentPage - 1,
        size: this.pageSize,
        search: this.searchText.trim() || undefined,
        role: roleParam,
        status: statusParam
      })
    }).subscribe({
      next: ({ stats, users }) => {
        this.metricsData = [
          { title: 'Utilisateurs', value: String(stats.totalUsers).padStart(2, '0'), icon: '/icones/utilisateurs.svg' },
          { title: 'Actifs', value: String(stats.activeUsers).padStart(2, '0'), icon: '/icones/GreenUser.svg' },
          { title: 'Inactifs', value: String(stats.inactiveUsers).padStart(2, '0'), icon: '/icones/OrangeUser.svg' }
        ];
        this.users = (users.content || []).map(item => this.mapListItemToDisplay(item));
        this.totalPages = users.totalPages || 1;
        this.totalElements = users.totalElements ?? 0;

        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  private mapListItemToDisplay(item: UserListItemDTO): UserDisplay {
    const name = `${item.firstName || ''} ${item.lastName || ''}`.trim() || item.email;
    const initials = name.split(/\s+/).map(s => s[0]).join('').toUpperCase().slice(0, 2) || '?';
    return {
      id: item.id,
      ref: item.reference,
      name,
      initials,
      email: item.email,
      role: item.roleLabel,
      createdAt: typeof item.createdAt === 'string' ? item.createdAt : formatDate(item.createdAt),
      status: item.isActive ? 'Actif' : 'Inactif',
      profilePhotoUrl: this.adminService.getProfilePhotoUrl(item.profilePhotoUrl) ?? undefined
    };
  }

  loadStats() {
    this.adminService.getUsersStats().subscribe({
      next: (s: UserStatsDTO) => {
    this.metricsData = [
          { title: 'Utilisateurs', value: String(s.totalUsers).padStart(2, '0'), icon: '/icones/utilisateurs.svg' },
          { title: 'Actifs', value: String(s.activeUsers).padStart(2, '0'), icon: '/icones/GreenUser.svg' },
          { title: 'Inactifs', value: String(s.inactiveUsers).padStart(2, '0'), icon: '/icones/OrangeUser.svg' }
        ];
      },
      error: (err) => console.error('Erreur stats utilisateurs:', err)
    });
  }

  loadUsers() {
    this.loading = true;
    const roleParam = this.selectedRole === 'Toutes les rôles' ? undefined : ROLE_LABEL_TO_ENUM[this.selectedRole];
    const statusParam = this.selectedStatut === 'Tous les statuts' ? undefined : this.selectedStatut === 'Actif';
    this.adminService.getUsers({
      page: this.currentPage - 1,
      size: this.pageSize,
      search: this.searchText.trim() || undefined,
      role: roleParam,
      status: statusParam
    }).subscribe({
      next: (res: UserListResponseDTO) => {
        this.users = (res.content || []).map(item => this.mapListItemToDisplay(item));
        this.totalPages = res.totalPages || 1;
        this.totalElements = res.totalElements ?? 0;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  applyFilters() {
    this.currentPage = 1;
    this.loadUsers();
  }

  onPageSizeChange(size: number) {
    this.pageSize = size;
    this.currentPage = 1;
    this.loadUsers();
  }

  get uniqueRoles(): string[] {
    return ROLES_FOR_FILTER;
  }

  get uniqueStatuts(): string[] {
    return ['Tous les statuts', 'Actif', 'Inactif'];
  }

  toggleRoleDropdown() {
    this.showRoleDropdown = !this.showRoleDropdown;
    this.showStatutDropdown = false;
  }

  toggleStatutDropdown() {
    this.showStatutDropdown = !this.showStatutDropdown;
    this.showRoleDropdown = false;
  }

  selectRole(role: string) {
    this.selectedRole = role;
    this.showRoleDropdown = false;
    this.applyFilters();
  }

  selectStatut(statut: string) {
    this.selectedStatut = statut;
    this.showStatutDropdown = false;
    this.applyFilters();
  }

  exportUsers(): void {
    if (!isPlatformBrowser(this.platformId) || this.loadingExport) return;
    this.loadingExport = true;
    const roleParam = this.selectedRole === 'Toutes les rôles' ? undefined : ROLE_LABEL_TO_ENUM[this.selectedRole];
    const statusParam = this.selectedStatut === 'Tous les statuts' ? undefined : this.selectedStatut === 'Actif';
    this.adminService
      .exportUsers(this.searchText.trim() || undefined, roleParam, statusParam)
      .pipe(finalize(() => { this.loadingExport = false; }))
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `utilisateurs_${new Date().toISOString().slice(0, 10)}.xlsx`;
          a.click();
          window.URL.revokeObjectURL(url);
        },
        error: () => {
          Swal.fire({
            title: 'Export impossible',
            text: 'Une erreur est survenue lors du téléchargement.',
            icon: 'error'
          });
        }
      });
  }

  getStatusClass(status: string): string {
    return status === 'Actif'
      ? 'bg-[#0A97480F] text-[#0A9748]'
      : 'bg-red-50 text-[#FF0909]';
  }

  getStatusDotClass(status: string): string {
    return status === 'Actif' ? 'bg-[#0A9748]' : 'bg-[#FF0909]';
  }

  nouveauUtilisateur() {
    this.router.navigate(['/admin/users/add']);
  }

  viewUser(user: UserDisplay) {
    this.adminService.getUserById(user.id).subscribe({
      next: (d: UserDetailsDTO) => {
        this.selectedUser = mapUserDetailsToDisplay(d, (p) => this.adminService.getProfilePhotoUrl(p));
        this.showUserModal = true;
      },
      error: () => {}
    });
  }

  closeUserModal() {
    this.showUserModal = false;
    this.selectedUser = null;
  }

  modifierUser() {
    if (this.selectedUser) {
      this.router.navigate(['/admin/users/edit', this.selectedUser.id]);
      this.closeUserModal();
    }
  }

  annulerUser() {
      this.closeUserModal();
  }

  onUserModalToggleStatus(): void {
    if (this.selectedUser) {
      this.toggleUserStatus(this.selectedUser);
    }
  }

  editUser(user: UserDisplay) {
    this.router.navigate(['/admin/users/edit', user.id]);
  }

  toggleUserStatus(user: UserDisplay) {
    const isActive = user.status !== 'Actif';
    const title = isActive ? 'Activer cet utilisateur ?' : 'Désactiver cet utilisateur ?';
    const text = isActive
      ? 'L\'utilisateur pourra à nouveau se connecter et utiliser l\'application.'
      : 'Il ne pourra plus se connecter ni utiliser l\'application.';
    Swal.fire({
      title,
      text,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: isActive ? '#22C55E' : '#EF4444',
      cancelButtonColor: '#6B7280',
      confirmButtonText: 'Oui',
      cancelButtonText: 'Annuler'
    }).then((result) => {
      if (!result.isConfirmed) return;
      this.toggleLoadingId = user.id;
      this.adminService.updateUserStatus(user.id, { isActive }).subscribe({
        next: () => {
          user.status = isActive ? 'Actif' : 'Inactif';
          this.loadStats();
          this.loadUsers();
          this.toggleLoadingId = null;
        },
        error: (err: unknown) => {
          this.toggleLoadingId = null;
          const msg = this.extractApiErrorMessage(err)
            || "Impossible d'effectuer cette action pour le moment.";
          Swal.fire({
            title: 'Activation impossible',
            text: msg,
            icon: 'error'
          });
        }
      });
    });
  }

  /** Extrait un message backend (texte) depuis une erreur HTTP Angular. */
  private extractApiErrorMessage(err: unknown): string | null {
    // Angular HttpClient: err est souvent HttpErrorResponse
    const httpErr = err as HttpErrorResponse | undefined;
    const raw = (httpErr && typeof (httpErr as any).error !== 'undefined') ? (httpErr as any).error : null;

    // Cas 1: backend renvoie du texte (responseType text) => raw est string
    if (typeof raw === 'string') {
      const t = raw.trim();
      if (!t) return null;

      // Parfois l'API renvoie un JSON stringifié (ex: {"message":"...","status":500,...})
      // → on tente de parser pour ne garder que "message".
      if (t.startsWith('{') || t.startsWith('[')) {
        try {
          const parsed = JSON.parse(t);
          if (parsed && typeof parsed === 'object') {
            const anyParsed = parsed as any;
            const m = (typeof anyParsed.message === 'string' ? anyParsed.message : '')
              || (typeof anyParsed.error === 'string' ? anyParsed.error : '')
              || '';
            const tm = String(m).trim();
            if (tm) return tm;
          }
        } catch {
          // ignore JSON parse errors → fallback to raw string below
        }
      }

      return t;
    }

    // Cas 2: backend renvoie JSON {message: "..."} ou {error: "..."}
    if (raw && typeof raw === 'object') {
      const anyRaw = raw as any;
      const m = (typeof anyRaw.message === 'string' ? anyRaw.message : '')
        || (typeof anyRaw.error === 'string' ? anyRaw.error : '')
        || '';
      const t = String(m).trim();
      return t ? t : null;
    }

    // Cas 3: fallback sur statusText
    const statusText = (httpErr && typeof httpErr.statusText === 'string') ? httpErr.statusText.trim() : '';
    return statusText ? statusText : null;
  }

  previousPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadUsers();
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadUsers();
    }
  }
}
