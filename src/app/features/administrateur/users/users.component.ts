import { Component, OnInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { forkJoin, finalize } from 'rxjs';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { NgChartsModule } from 'ng2-charts';
import { ChartConfiguration } from 'chart.js';
import Swal from 'sweetalert2';
import { PAGE_SIZE_OPTIONS } from '../../../shared/constants/pagination';
import {
  AdminService,
  UserListItemDTO,
  UserListResponseDTO,
  UserStatsDTO,
  UserStatsByRoleItemDTO,
  UserStatsByStatusItemDTO,
  UserDetailsDTO
} from '../../../shared/services/admin.service';

interface MetricCard {
  title: string;
  value: string;
  icon: string;
}

/** Modèle d'affichage pour une ligne utilisateur / modal. */
export interface UserDisplay {
  id: number;
  ref: string;
  name: string;
  initials: string;
  email: string;
  role: string;
  createdAt: string;
  status: 'Actif' | 'Inactif';
  phone?: string;
  /** URL complète de la photo de profil (affichage liste / modal). */
  profilePhotoUrl?: string | null;
}

/** Map libellé rôle → enum backend pour le filtre. */
const ROLE_LABEL_TO_ENUM: Record<string, string> = {
  'Administrateur': 'ADMINISTRATOR',
  'Commercial': 'COMMERCIAL',
  'Livreur': 'DELIVERY_DRIVER',
  'Salarié': 'EMPLOYEE',
  'Responsable Logistique': 'LOGISTICS_MANAGER'
};

const ROLES_FOR_FILTER = ['Toutes les rôles', 'Administrateur', 'Commercial', 'Livreur', 'Salarié', 'Responsable Logistique'];

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
  imports: [MainLayoutComponent, HeaderComponent, CommonModule, FormsModule, NgChartsModule],
  templateUrl: './users.component.html',
  styles: ``
})
export class UsersComponent implements OnInit {
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
  loadingCharts = false;
  loadingExport = false;
  toggleLoadingId: number | null = null;
  private searchDebounceTimer: ReturnType<typeof setTimeout> | null = null;

  /** Chart.js / canvas non supportés en SSR — n’afficher les graphiques que dans le navigateur. */
  isBrowser = false;

  constructor(
    private router: Router,
    private adminService: AdminService,
    @Inject(PLATFORM_ID) private platformId: object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  onSearchInput() {
    if (this.searchDebounceTimer != null) clearTimeout(this.searchDebounceTimer);
    this.searchDebounceTimer = setTimeout(() => {
      this.searchDebounceTimer = null;
      this.applyFilters();
    }, 300);
  }

  // Bar Chart - Utilisateurs par rôle (données en % depuis l'API)
  public barChartData: ChartConfiguration<'bar'>['data'] = {
    labels: [] as string[],
    datasets: [
      {
        data: [] as number[],
        backgroundColor: (ctx) => {
          const { chart } = ctx;
          const { ctx: c, chartArea } = chart as any;
          if (!chartArea) return '#FF6B00';
          const gradient = c.createLinearGradient(chartArea.left, 0, chartArea.right, 0);
          gradient.addColorStop(0, '#FF6B00');
          gradient.addColorStop(1, '#FF914D');
          return gradient;
        },
        barThickness: 30,
        hoverBackgroundColor: '#FF914D'
      }
    ]
  };

  public barChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    indexAxis: 'y',
    plugins: {
      legend: {
        display: true,
        position: 'top',
        align: 'start',
        labels: {
          boxWidth: 40,
          boxHeight: 12,
          padding: 15,
          font: { size: 12 },
          generateLabels: () => [{ text: 'Utilisation(%)', fillStyle: '#FF6B00', strokeStyle: '#FF6B00', lineWidth: 0 }]
        }
      },
      tooltip: {
        enabled: true,
        callbacks: {
          label: (ctx) => `${Number(ctx.parsed.x).toFixed(1)}%`
        }
      }
    },
    scales: {
      x: {
        beginAtZero: true,
        max: 100,
        ticks: { stepSize: 10, font: { size: 10 } },
        grid: { display: true, color: '#F2F5F9' }
      },
      y: {
        ticks: { font: { size: 12 } },
        grid: { display: true, color: '#F2F5F9' }
      }
    }
  };

  // Doughnut Chart - Répartition des statuts (données API)
  public doughnutChartData: ChartConfiguration<'doughnut'>['data'] = {
    labels: ['Actifs', 'Inactifs'],
    datasets: [
      {
        data: [0, 0],
        backgroundColor: ['#22C55F', '#FFD3D3'],
        hoverBackgroundColor: ['#22C55E', '#eeb8b8ff'],
        borderWidth: 2,
        hoverBorderColor: '#FFFFFF'
      }
    ]
  };

  public doughnutChartOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '60%',
    plugins: {
      legend: {
        display: true,
        position: 'right',
        labels: {
          usePointStyle: true,
          pointStyle: 'circle',
          boxWidth: 6,
          boxHeight: 6,
          padding: 20,
          font: {
            size: 12
          },
          generateLabels: (chart) => {
            const data = chart.data;
            if (data.labels && data.datasets.length) {
              return data.labels.map((label, i) => ({
                text: label as string,
                fillStyle: (data.datasets[0].backgroundColor as string[])[i],
                strokeStyle: (data.datasets[0].backgroundColor as string[])[i],
                lineWidth: 0,
                hidden: false,
                index: i
              }));
            }
            return [];
          }
        }
      },
      tooltip: {
        enabled: true,
        callbacks: {
          label: (context) => {
            return `${context.label}: ${Number(context.parsed).toFixed(1)}%`;
          }
        }
      }
    }
  };

  ngOnInit() {
    this.loadInitialDataInParallel();
  }

  /** Lance les 4 appels API en parallèle pour un chargement plus rapide. */
  private loadInitialDataInParallel() {
    this.loading = true;
    this.loadingCharts = true;
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
      }),
      byRole: this.adminService.getUsersStatsByRole(),
      byStatus: this.adminService.getUsersStatsByStatus()
    }).subscribe({
      next: ({ stats, users, byRole, byStatus }) => {
        this.metricsData = [
          { title: 'Utilisateurs', value: String(stats.totalUsers).padStart(2, '0'), icon: '/icones/utilisateurs.svg' },
          { title: 'Actifs', value: String(stats.activeUsers).padStart(2, '0'), icon: '/icones/GreenUser.svg' },
          { title: 'Inactifs', value: String(stats.inactiveUsers).padStart(2, '0'), icon: '/icones/OrangeUser.svg' }
        ];
        this.users = (users.content || []).map(item => this.mapListItemToDisplay(item));
        this.totalPages = users.totalPages || 1;
        this.totalElements = users.totalElements ?? 0;

        const labels = (byRole || []).map(r => r.roleLabel ?? '');
        const data = (byRole || []).map(r => Number(r.percentage) ?? 0);
        this.barChartData = {
          ...this.barChartData,
          labels,
          datasets: [{ ...this.barChartData.datasets[0], data }]
        };

        const actifs = (byStatus || []).find(x => /actif/i.test(x.label ?? ''));
        const inactifs = (byStatus || []).find(x => /inactif/i.test(x.label ?? ''));
        const a = actifs != null ? Number(actifs.percentage) : 0;
        const i = inactifs != null ? Number(inactifs.percentage) : 0;
        this.doughnutChartData = {
          ...this.doughnutChartData,
          datasets: [{ ...this.doughnutChartData.datasets[0], data: [a, i] }]
        };

        this.loading = false;
        this.loadingCharts = false;
      },
      error: () => {
        this.loading = false;
        this.loadingCharts = false;
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

  private mapDetailsToDisplay(d: UserDetailsDTO): UserDisplay {
    const name = `${d.firstName || ''} ${d.lastName || ''}`.trim() || d.email;
    const initials = name.split(/\s+/).map(s => s[0]).join('').toUpperCase().slice(0, 2) || '?';
    return {
      id: d.id,
      ref: d.refUser,
      name,
      initials,
      email: d.email,
      role: d.roleLabel,
      createdAt: typeof d.createdAt === 'string' ? d.createdAt : formatDate(d.createdAt),
      status: d.isActive ? 'Actif' : 'Inactif',
      phone: d.phoneNumber ?? undefined,
      profilePhotoUrl: this.adminService.getProfilePhotoUrl(d.profilePhotoUrl) ?? undefined
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

  loadStatsByRole() {
    this.loadingCharts = true;
    this.adminService.getUsersStatsByRole().subscribe({
      next: (list: UserStatsByRoleItemDTO[]) => {
        const labels = (list || []).map(r => r.roleLabel ?? '');
        const data = (list || []).map(r => Number(r.percentage) ?? 0);
        this.barChartData = {
          ...this.barChartData,
          labels,
          datasets: [{
            ...this.barChartData.datasets[0],
            data
          }]
        };
        this.loadingCharts = false;
      },
      error: () => { this.loadingCharts = false; }
    });
  }

  loadStatsByStatus() {
    this.adminService.getUsersStatsByStatus().subscribe({
      next: (list: UserStatsByStatusItemDTO[]) => {
        const actifs = (list || []).find(x => /actif/i.test(x.label ?? ''));
        const inactifs = (list || []).find(x => /inactif/i.test(x.label ?? ''));
        const a = actifs != null ? Number(actifs.percentage) : 0;
        const i = inactifs != null ? Number(inactifs.percentage) : 0;
        this.doughnutChartData = {
          ...this.doughnutChartData,
          datasets: [{
            ...this.doughnutChartData.datasets[0],
            data: [a, i]
          }]
        };
      },
      error: () => {}
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
    if (!this.isBrowser || this.loadingExport) return;
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
        this.selectedUser = this.mapDetailsToDisplay(d);
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
          this.loadStatsByStatus();
          this.loadUsers();
          this.toggleLoadingId = null;
        },
        error: () => { this.toggleLoadingId = null; }
      });
    });
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
