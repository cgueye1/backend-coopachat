import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { AuthService } from '../../../shared/services/auth.service';
import { EntrepriseService, CompanyDashboardKpisDTO } from '../../../shared/services/entreprise.service';

@Component({
  selector: 'app-entreprise-dashboard',
  standalone: true,
  imports: [CommonModule, MainLayoutComponent, HeaderComponent],
  templateUrl: './entreprise-dashboard.component.html',
  styles: [`
    .animate-fadeIn {
      animation: fadeIn 0.5s ease-out;
    }
    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(10px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class EntrepriseDashboardComponent implements OnInit {
  role: any = 'company';
  userName = '';
  companyName = '';

  // KPIs
  kpis: any[] = [];
  chartData: any[] = [];
  statusData = { actifs: 0, enAttente: 0, inactifs: 0 };

  constructor(
    private entrepriseService: EntrepriseService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadUserProfile();
    this.loadDashboardData();
  }

  loadUserProfile(): void {
    this.authService.getCurrentUserProfile().subscribe({
      next: (user) => {
        this.userName = `${user.firstName} ${user.lastName}`;
        this.companyName = user.companyName || 'Mon Entreprise';
      },
      error: (err) => console.error('Erreur profil', err)
    });
  }

  // Top salariés
  topSalaries: any[] = [];

  loadDashboardData(): void {
    this.entrepriseService.getDashboardKpis().subscribe({
      next: (data: CompanyDashboardKpisDTO) => {
        this.kpis = [
          { label: 'Total salariés',     value: data.totalEmployees,    sub: 'Effectif total inscrit',      subColor: 'text-gray-400',   border: 'border-l-[#2B3674]', icon: 'icones/users.svg' },
          { label: 'Salariés actifs',    value: data.activeEmployeesRatio, sub: 'Comptes activés',             subColor: 'text-green-600',  border: 'border-l-green-500', icon: 'icones/actif.svg' },
          { label: 'Salariés inactifs',  value: data.inactiveEmployees, sub: 'En attente d\'activation',    subColor: 'text-[#FF6B00]',  border: 'border-l-[#FF6B00]', icon: 'icones/exclamUser.svg' },
          { label: 'Commandes ce mois',  value: data.ordersThisMonth,   sub: 'Activité de l\'entreprise',    subColor: 'text-blue-500',   border: 'border-l-blue-400', icon: 'icones/commandes.svg' },
        ];
        
        this.statusData = {
          actifs: data.activeEmployees,
          enAttente: data.inactiveEmployees,
          inactifs: 0
        };

        if (data.evolutionCommandes) {
          this.chartData = data.evolutionCommandes;
        }

        if (data.topEmployees) {
          this.topSalaries = data.topEmployees.map(emp => ({
            initials: this.getInitials(emp.firstName, emp.lastName),
            name: `${emp.firstName} ${emp.lastName}`,
            ref: emp.employeeCode,
            commandes: emp.nbCommandes,
            activite: emp.activite,
            statut: emp.status
          }));
        }
      },
      error: (err) => {
        console.error('Erreur lors du chargement des KPIs', err);
      }
    });
  }

  getInitials(firstName: string, lastName: string): string {
    return (firstName?.charAt(0) || '') + (lastName?.charAt(0) || '');
  }

  // Helpers UI
  getStatutClass(statut: string): string {
    switch (statut) {
      case 'Actif': return 'bg-green-100 text-green-700';
      case 'En attente': return 'bg-orange-100 text-orange-700';
      case 'Inactif': return 'bg-gray-100 text-gray-700';
      default: return 'bg-blue-100 text-blue-700';
    }
  }

  getStatutIcon(statut: string): string {
    return statut === 'Actif' ? 'icones/check.svg' : 'icones/clock.svg';
  }

  getActiviteColor(percent: number): string {
    if (percent >= 80) return 'bg-green-500';
    if (percent >= 50) return 'bg-blue-500';
    return 'bg-orange-500';
  }
}
