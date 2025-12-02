import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { NgChartsModule } from 'ng2-charts';
import { ChartConfiguration } from 'chart.js';
import { UserService, User } from '../../../shared/services/user.service';

interface MetricCard {
  title: string;
  value: string;
  icon: string;
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
  totalPages = 6;
  showUserModal = false;
  selectedUser: User | null = null;

  constructor(private router: Router, private userService: UserService) { }

  // Bar Chart Configuration - Utilisateurs par rôle
  public barChartData: ChartConfiguration<'bar'>['data'] = {
    labels: ['Salariés', 'Commerciaux', 'Livreurs', 'Admin'],
    datasets: [
      {
        data: [45, 35, 25, 20],
        backgroundColor: (ctx) => {
          const { chart } = ctx;
          const { ctx: c, chartArea } = chart as any;
          if (!chartArea) {
            return '#FF6B00';
          }
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
          font: {
            size: 12
          },
          generateLabels: () => [
            {
              text: 'Utilisation(%)',
              fillStyle: '#FF6B00',
              strokeStyle: '#FF6B00',
              lineWidth: 0
            }
          ]
        }
      },
      tooltip: {
        enabled: true
      }
    },
    scales: {
      x: {
        beginAtZero: true,
        max: 50,
        ticks: {
          stepSize: 5,
          font: {
            size: 10
          }
        },
        grid: {
          display: true,
          color: '#F2F5F9'
        }
      },
      y: {
        ticks: {
          font: {
            size: 12
          }
        },
        grid: {
          display: true,
          color: '#F2F5F9'
        }
      }
    }
  };

  // Doughnut Chart Configuration - Répartition des statuts
  public doughnutChartData: ChartConfiguration<'doughnut'>['data'] = {
    labels: ['Actifs', 'Inactifs'],
    datasets: [
      {
        data: [83, 17],
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
            return `${context.label}: ${context.parsed}%`;
          }
        }
      }
    }
  };

  ngOnInit() {
    this.userService.users$.subscribe(users => {
      this.users = users;
      this.updateMetrics();
    });
  }

  updateMetrics() {
    const actifs = this.users.filter(u => u.status === 'Actif').length;
    const inactifs = this.users.filter(u => u.status === 'Inactif').length;

    this.metricsData = [
      {
        title: 'Utilisateurs',
        value: String(this.users.length).padStart(2, '0'),
        icon: '/icones/utilisateurs.svg'
      },
      {
        title: 'Actifs',
        value: String(actifs).padStart(2, '0'),
        icon: '/icones/GreenUser.svg'
      },
      {
        title: 'Inactifs',
        value: String(inactifs).padStart(2, '0'),
        icon: '/icones/OrangeUser.svg'
      }
    ];
  }

  metricsData: MetricCard[] = [];
  users: User[] = [];

  get uniqueRoles(): string[] {
    const roles = new Set(this.users.map(user => user.role));
    return ['Toutes les rôles', ...Array.from(roles)];
  }

  get uniqueStatuts(): string[] {
    return ['Tous les statuts', 'Actif', 'Inactif'];
  }

  get filteredUsers(): User[] {
    return this.users.filter(user => {
      const matchesSearch =
        user.name.toLowerCase().includes(this.searchText.toLowerCase()) ||
        user.email.toLowerCase().includes(this.searchText.toLowerCase()) ||
        user.id.toLowerCase().includes(this.searchText.toLowerCase());

      const matchesRole = this.selectedRole === 'Toutes les rôles' || user.role === this.selectedRole;
      const matchesStatut = this.selectedStatut === 'Tous les statuts' || user.status === this.selectedStatut;

      return matchesSearch && matchesRole && matchesStatut;
    });
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
  }

  selectStatut(statut: string) {
    this.selectedStatut = statut;
    this.showStatutDropdown = false;
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

  viewUser(user: User) {
    this.selectedUser = user;
    this.showUserModal = true;
  }

  closeUserModal() {
    this.showUserModal = false;
    this.selectedUser = null;
  }

  modifierUser() {
    if (this.selectedUser) {
      console.log('Modifier utilisateur:', this.selectedUser);
      this.closeUserModal();
    }
  }

  annulerUser() {
    if (this.selectedUser) {
      console.log('Annuler utilisateur:', this.selectedUser);
      this.closeUserModal();
    }
  }

  editUser(user: User) {
    console.log('Modifier utilisateur:', user);
  }

  toggleUserStatus(user: User) {
    user.status = user.status === 'Actif' ? 'Inactif' : 'Actif';
  }

  previousPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }
}
