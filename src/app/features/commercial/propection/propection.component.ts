import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { CompanyModalComponent, CompanyFormData } from '../../../shared/components/company-modal/company-modal.component';
import Swal from 'sweetalert2';

interface Prospect {
  id: string;
  entreprise: string;
  secteur: string;
  localisation: string;
  contact: {
    nom: string;
    telephone: string;
  };
  statut: 'Actif' | 'Inactif';
  date: string;
  initials: string;
}

interface MetricCard {
  title: string;
  value: string;
  icon: string;
}

@Component({
  selector: 'app-prospection',
  standalone: true,
  imports: [CommonModule, FormsModule, MainLayoutComponent, CompanyModalComponent, HeaderComponent],
  templateUrl: `./propection.component.html`,
  styles: [`
    /* Custom styles for better visual consistency */
    .table-row:hover {
      background-color: #f9fafb;
    }
    
    /* Focus styles for accessibility */
    input:focus, select:focus, button:focus {
      outline: 2px solid transparent;
      outline-offset: 2px;
    }
    
    /* Smooth transitions */
    * {
      transition-property: background-color, border-color, color, fill, stroke;
      transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
      transition-duration: 200ms;
    }

    /* Scrollbar styles (applied to elements with overflow-x-auto) */
    .overflow-x-auto {
      scrollbar-width: thin;
      scrollbar-color: #cbd5e1 #f8fafc;
      position: relative;
    }
    
    .overflow-x-auto::-webkit-scrollbar {
      height: 8px;
    }
    
    .overflow-x-auto::-webkit-scrollbar-track {
      background: #f8fafc;
      border-radius: 4px;
    }
    
    .overflow-x-auto::-webkit-scrollbar-thumb {
      background: #cbd5e1;
      border-radius: 4px;
    }
    
    .overflow-x-auto::-webkit-scrollbar-thumb:hover {
      background: #94a3b8;
    }

    /* Responsive tweaks: ensure grids collapse nicely on small screens */
    @media (max-width: 1024px) {
      .grid-cols-12 { grid-template-columns: repeat(12, minmax(0, 1fr)); }
    }

    /* Improve touch target sizes on small screens */
    @media (max-width: 640px) {
      button, .px-4, .py-2 {
        min-height: 40px;
      }
    }

    /* Ensure cards and table cells wrap text properly */
    .whitespace-nowrap { white-space: nowrap; }
  `]
})
export class ProspectionComponent {
  searchTerm = '';
  selectedStatus = '';
  selectedSector = '';
  currentPage = 1;
  itemsPerPage = 5;
  isCompanyModalOpen = false;
  showStatusDropdown = false;
  showSectorDropdown = false;
  showProspectModal = false;
  selectedProspect: Prospect | null = null;

  uniqueStatuses = ['Tous les statuts', 'Actif', 'Inactif'];
  uniqueSectors = ['Tous les secteurs', 'Technologie', 'Santé', 'Éducation'];

  metricsData: MetricCard[] = [
    {
      title: 'Total des salariés',
      value: '5',
      icon: '/icones/utilisateurs.svg'
    },
    {
      title: 'Salariés actifs',
      value: '3',
      icon: '/icones/GreenUser.svg'
    },
    {
      title: "En attente d'activation",
      value: '1',
      icon: '/icones/exclamUser.svg'
    }
  ];

  prospects: Prospect[] = [
    { id: 'EA', entreprise: 'Entreprise ABC', secteur: 'Technologie', localisation: 'Dakar, Point E', contact: { nom: 'Moussa Ndiaye', telephone: '77 123 45 67' }, statut: 'Actif', date: '05/10/2025', initials: 'EA' },
    { id: 'SX', entreprise: 'Société XYZ', secteur: 'Santé', localisation: 'Dakar, Point E', contact: { nom: 'Moussa Ndiaye', telephone: '77 123 45 67' }, statut: 'Actif', date: '05/10/2025', initials: 'SX' },
    { id: 'GE', entreprise: 'Groupe 456', secteur: 'Éducation', localisation: 'Dakar, Point E', contact: { nom: 'Moussa Ndiaye', telephone: '77 123 45 67' }, statut: 'Actif', date: '05/10/2025', initials: 'GE' },
    { id: 'TI', entreprise: 'Tech Innovate', secteur: 'Technologie', localisation: 'Dakar, Point E', contact: { nom: 'Moussa Ndiaye', telephone: '77 123 45 67' }, statut: 'Actif', date: '05/10/2025', initials: 'TI' },
    { id: 'MP', entreprise: 'Médical Plus', secteur: 'Santé', localisation: 'Dakar, Point E', contact: { nom: 'Moussa Ndiaye', telephone: '77 123 45 67' }, statut: 'Inactif', date: '05/10/2025', initials: 'MP' }
  ];

  filteredProspects: Prospect[] = [...this.prospects];

  // totalResults is dynamic now
  get totalResults(): number {
    return this.filteredProspects.length;
  }

  get totalPages(): number {
    return Math.max(1, Math.ceil(this.totalResults / this.itemsPerPage));
  }

  // Retourne seulement les prospects de la page courante
  getCurrentPageProspects(): Prospect[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    return this.filteredProspects.slice(startIndex, endIndex);
  }

  trackByProspect(index: number, prospect: Prospect): string {
    return prospect.id;
  }

  toggleStatusDropdown(): void {
    this.showStatusDropdown = !this.showStatusDropdown;
    this.showSectorDropdown = false;
  }

  toggleSectorDropdown(): void {
    this.showSectorDropdown = !this.showSectorDropdown;
    this.showStatusDropdown = false;
  }

  selectStatus(status: string): void {
    this.selectedStatus = status === 'Tous les statuts' ? '' : status;
    this.showStatusDropdown = false;
    this.filterProspects();
  }

  selectSector(sector: string): void {
    this.selectedSector = sector === 'Tous les secteurs' ? '' : sector;
    this.showSectorDropdown = false;
    this.filterProspects();
  }

  onSearch(): void {
    this.filterProspects();
  }

  onStatusFilter(): void {
    this.filterProspects();
  }

  onSectorFilter(): void {
    this.filterProspects();
  }

  filterProspects(): void {
    let filtered = [...this.prospects];

    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase().trim();
      filtered = filtered.filter(prospect =>
        prospect.entreprise.toLowerCase().includes(term) ||
        prospect.contact.nom.toLowerCase().includes(term) ||
        prospect.localisation.toLowerCase().includes(term)
      );
    }

    if (this.selectedStatus) {
      filtered = filtered.filter(prospect => prospect.statut === this.selectedStatus);
    }

    if (this.selectedSector) {
      filtered = filtered.filter(prospect => prospect.secteur === this.selectedSector);
    }

    this.filteredProspects = filtered;
    this.currentPage = 1; // Reset à la première page après filtrage
  }

  editProspect(id: string): void {
    console.log('Éditer prospect:', id);
    // Implémentation de l'édition
  }

  viewDetails(prospect: Prospect): void {
    this.selectedProspect = prospect;
    this.showProspectModal = true;
  }

  closeProspectModal(): void {
    this.showProspectModal = false;
    this.selectedProspect = null;
  }

  toggleProspectStatus(prospect: Prospect): void {
    prospect.statut = prospect.statut === 'Actif' ? 'Inactif' : 'Actif';
  }

  modifierProspect(): void {
    if (this.selectedProspect) {
      console.log('Modifier prospect:', this.selectedProspect);
      this.closeProspectModal();
    }
  }

  annulerProspect(): void {
    if (this.selectedProspect) {
      console.log('Annuler prospect:', this.selectedProspect);
      this.closeProspectModal();
    }
  }

  getStatusClass(status: string): string {
    return status === 'Actif'
      ? 'bg-[#0A97480F] text-[#0A9748]'
      : 'bg-red-50 text-[#FF0909]';
  }

  getStatusDotClass(status: string): string {
    return status === 'Actif' ? 'bg-[#0A9748]' : 'bg-[#FF0909]';
  }

  getDisplayStart(): number {
    if (this.totalResults === 0) return 0;
    return (this.currentPage - 1) * this.itemsPerPage + 1;
  }

  getDisplayEnd(): number {
    return Math.min(this.currentPage * this.itemsPerPage, this.totalResults);
  }

  getPageNumbers(): number[] {
    const pages = [];
    const maxVisiblePages = 5;
    let startPage = Math.max(1, this.currentPage - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(this.totalPages, startPage + maxVisiblePages - 1);

    if (endPage - startPage + 1 < maxVisiblePages) {
      startPage = Math.max(1, endPage - maxVisiblePages + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }
    return pages;
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
    }
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  onCompanySubmit(data: CompanyFormData): void {
    // Créer un nouveau prospect
    const initials = data.nom.split(' ').map(word => word[0]).join('').substring(0, 2).toUpperCase();
    const newProspect: Prospect = {
      id: initials,
      entreprise: data.nom,
      secteur: data.secteur || 'Non défini',
      localisation: data.localisation,
      contact: {
        nom: data.contact,
        telephone: data.telephone
      },
      statut: data.statut === 'actif' ? 'Actif' : 'Inactif',
      date: new Date().toLocaleDateString('fr-FR'),
      initials: initials
    };

    // Ajouter au début du tableau
    this.prospects.unshift(newProspect);
    this.filterProspects();
    this.isCompanyModalOpen = false;

    // Afficher le message de succès
    Swal.fire({
      iconHtml: '<img src="/icones/message success.svg" style="width: 95px; height: 95px; margin: 0 auto;" />',
      title: 'Entreprise créée avec succès',
      showConfirmButton: false,
      timer: 1500,
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-3xl p-6',
        title: 'text-xl font-medium text-gray-900',
        icon: 'border-none'
      },
      backdrop: `rgba(0,0,0,0.2)`,
      width: '580px',
      showClass: {
        popup: 'animate__animated animate__fadeIn animate__faster'
      },
      hideClass: {
        popup: 'animate__animated animate__fadeOut animate__faster'
      }
    });
  }
}
