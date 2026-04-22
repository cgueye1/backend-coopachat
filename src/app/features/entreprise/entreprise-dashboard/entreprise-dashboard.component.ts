import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EntrepriseLayoutComponent } from '../entreprise-layout/entreprise-layout.component';

@Component({
  selector: 'app-entreprise-dashboard',
  standalone: true,
  imports: [CommonModule, EntrepriseLayoutComponent],
  templateUrl: './entreprise-dashboard.component.html'
})
export class EntrepriseDashboardComponent {
  companyName = 'SENICO DAKAR';
  selectedPeriod = 'Ce mois — Avril 2026';

  // KPIs (données mock — à brancher sur l'API plus tard)
  kpis = [
    { label: 'Taux d\'adoption',   value: '41%',  sub: '+8% vs mois dernier',         subColor: 'text-green-600',  border: 'border-l-[#FF6B00]' },
    { label: 'Salariés actifs',    value: '9/22', sub: '13 en attente d\'activation', subColor: 'text-[#FF6B00]',  border: 'border-l-green-500' },
    { label: 'Commandes ce mois',  value: '34',   sub: 'Moy. 3,8 cmd/salarié actif', subColor: 'text-gray-400',   border: 'border-l-[#2B3674]' },
    { label: 'Salariés inactifs',  value: '13',   sub: 'Pas encore commandé',         subColor: 'text-red-500',    border: 'border-l-red-400' },
  ];

  // Top salariés (mock)
  topSalaries = [
    { initials: 'MS', name: 'Mariama Sow',   ref: 'SAL-2026-34', commandes: 8, activite: 100, statut: 'Actif' },
    { initials: 'AT', name: 'Aminata Tall',  ref: 'SAL-2026-32', commandes: 6, activite: 75,  statut: 'Actif' },
    { initials: 'CD', name: 'Coumba Diagne', ref: 'SAL-2026-31', commandes: 5, activite: 62,  statut: 'Actif' },
    { initials: 'FL', name: 'Fatima Lawson', ref: 'SAL-679A7603', commandes: 0, activite: 0,  statut: 'En attente' },
    { initials: 'KK', name: 'Khadija Ka',    ref: 'SAL-2026-03', commandes: 0, activite: 0,  statut: 'En attente' },
  ];

  getStatutClass(statut: string): string {
    return statut === 'Actif'
      ? 'bg-green-100 text-green-700'
      : 'bg-orange-100 text-orange-600';
  }
}
