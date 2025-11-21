import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';

interface Metric {
  title: string;
  value: string;
  icon: string;
  
}

interface Livraison {
  id: string;
  client: {
    name: string;
    id: string;
    initials: string;
    color: string;
  };
  transporteur: string;
  commande: string;
  fournisseur: string;
  chauffeur: {
    name: string;
    vehicle: string;
  } | null;
  date: string;
  statut: 'Planifié' | 'Confirmé' | 'Annulé' | 'À confirmer' | 'Livrée';
}

@Component({
  selector: 'app-livraisons',
  standalone: true,
  imports: [CommonModule, FormsModule, MainLayoutComponent],
  templateUrl: './livraisons.component.html',
  styles: []
})
export class LivraisonsComponent {
  searchText: string = '';
  currentPage: number = 1;
  totalPages: number = 6;

  metricsData: Metric[] = [
    { title: 'Planifiées', value: '06', icon: 'box-blue' },
    { title: 'À confirmer', value: '05', icon: 'warning-yellow',  },
    { title: 'Confirmées', value: '05', icon: 'check-blue' },
    { title: 'Livrées', value: '05', icon: 'check-green'},
    { title: 'Annulées', value: '01', icon: 'box-red'}
  ];

  livraisons: Livraison[] = [
    {
      id: '1',
      client: { name: 'Aminata Ndiaye', id: 'US-2025-05', initials: 'AN', color: 'bg-gray-100 text-gray-600' },
      transporteur: 'DHL',
      commande: 'Salarié',
      fournisseur: 'Sahel Agro',
      chauffeur: { name: 'I. Diallo', vehicle: 'Sprinter-1' },
      date: '05/10/2025',
      statut: 'Planifié'
    },
    {
      id: '2',
      client: { name: 'Moussa Sarr', id: 'US-2025-04', initials: 'MS', color: 'bg-gray-100 text-gray-600' },
      transporteur: 'Chrono SN',
      commande: 'Commercial',
      fournisseur: 'Nordik Import',
      chauffeur: { name: 'A. Faye', vehicle: 'Kia-Box' },
      date: '05/10/2025',
      statut: 'Confirmé'
    },
    {
      id: '3',
      client: { name: 'Fatou Diop', id: 'US-2025-03', initials: 'FD', color: 'bg-gray-100 text-gray-600' },
      transporteur: 'Chrono SN',
      commande: 'Livreur',
      fournisseur: 'Nordik Import',
      chauffeur: { name: 'S. Sow', vehicle: 'Sprinter-0' },
      date: '05/10/2025',
      statut: 'Planifié'
    },
    {
      id: '4',
      client: { name: 'Ibrahima Ba', id: 'US-2025-02', initials: 'IB', color: 'bg-gray-100 text-gray-600' },
      transporteur: 'Colis SN',
      commande: 'Salarié',
      fournisseur: 'Sénégalaise Fourn.',
      chauffeur: { name: 'M. Ndiaye', vehicle: 'Dacia D' },
      date: '05/10/2025',
      statut: 'Annulé'
    },
    {
      id: '5',
      client: { name: 'Lamine Sy', id: 'US-2025-01', initials: 'LS', color: 'bg-gray-100 text-gray-600' },
      transporteur: 'DHL',
      commande: 'Administrateur',
      fournisseur: 'Sénégalaise Fourn.',
      chauffeur: null,
      date: '05/10/2025',
      statut: 'À confirmer'
    }
  ];

  getStatusClass(status: string): string {
    switch (status) {
      case 'Planifié': return 'bg-[#0A97480F] text-[#0A9748]';
      case 'Confirmé': return 'bg-[#4F46E50F] text-[#4F46E5]';
      case 'Annulé': return 'bg-[#FF09090F] text-[#FF0909]';
      case 'À confirmer': return 'bg-[#EAB3080F] text-[#EAB308]';
      case 'Livrée': return 'bg-emerald-50 text-emerald-600';
      default: return 'bg-gray-50 text-gray-600';
    }
  }

  getStatusDotClass(status: string): string {
    switch (status) {
      case 'Planifié': return 'bg-[#0A9748]';
      case 'Confirmé': return 'bg-[#4F46E5]';
      case 'Annulé': return 'bg-[#FF0909]';
      case 'À confirmer': return 'bg-[#EAB308]';
      case 'Livrée': return 'bg-emerald-500';
      default: return 'bg-gray-500';
    }
  }
}
