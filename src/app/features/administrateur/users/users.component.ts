import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';

interface MetricCard {
  title: string;
  value: string;
  icon: string;
}

interface User {
  id: string;
  name: string;
  initials: string;
  email: string;
  role: string;
  createdAt: string;
  status: 'Actif' | 'Inactif';
  phone?: string;
}

@Component({
  selector: 'app-users',
  standalone: true,
  imports: [MainLayoutComponent, HeaderComponent, CommonModule, FormsModule],
  templateUrl: './users.component.html',
  styles: ``
})
export class UsersComponent {
  searchText = '';
  selectedRole = 'Toutes les rôles';
  selectedStatut = 'Tous les statuts';
  showRoleDropdown = false;
  showStatutDropdown = false;
  currentPage = 1;
  totalPages = 6;
  showUserModal = false;
  selectedUser: User | null = null;

  metricsData: MetricCard[] = [
    {
      title: 'Utilisateurs',
      value: '06',
      icon: '/icones/utilisateurs.svg'
    },
    {
      title: 'Actifs',
      value: '05',
      icon: '/icones/GreenUser.svg'
    },
    {
      title: 'Inactifs',
      value: '01',
      icon: '/icones/OrangeUser.svg'
    }
  ];

  users: User[] = [
    {
      id: 'US-2025-05',
      name: 'Aminata Ndiaye',
      initials: 'AN',
      email: 'aminata@exemple.sn',
      role: 'Salarié',
      createdAt: '05/10/2025',
      status: 'Actif',
      phone: '70 645 87 92'
    },
    {
      id: 'US-2025-04',
      name: 'Moussa Sarr',
      initials: 'MS',
      email: 'moussa@exemple.sn',
      role: 'Commercial',
      createdAt: '05/10/2025',
      status: 'Actif',
      phone: '77 123 45 67'
    },
    {
      id: 'US-2025-03',
      name: 'Fatou Diop',
      initials: 'FD',
      email: 'fatou@exemple.sn',
      role: 'Livreur',
      createdAt: '05/10/2025',
      status: 'Actif',
      phone: '76 987 65 43'
    },
    {
      id: 'US-2025-02',
      name: 'Ibrahima Ba',
      initials: 'IB',
      email: 'ibrahima@exemple.sn',
      role: 'Salarié',
      createdAt: '05/10/2025',
      status: 'Actif',
      phone: '78 456 78 90'
    },
    {
      id: 'US-2025-01',
      name: 'Lamine Sy',
      initials: 'LS',
      email: 'lamine@exemple.sn',
      role: 'Administrateur',
      createdAt: '05/10/2025',
      status: 'Inactif',
      phone: '77 234 56 78'
    }
  ];

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
      ? 'bg-green-50 text-green-700'
      : 'bg-red-50 text-red-700';
  }

  getStatusDotClass(status: string): string {
    return status === 'Actif' ? 'bg-green-500' : 'bg-red-500';
  }

  nouveauUtilisateur() {
    console.log('Nouveau utilisateur');
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
