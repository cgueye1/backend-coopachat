import { Component, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { LogisticsService, EligibleOrder, EligibleOrderLot, AvailableDriver } from '../../../shared/services/logistics.service';
import Swal from 'sweetalert2';

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
  imports: [MainLayoutComponent, CommonModule, FormsModule],
  templateUrl: './livraisons.component.html',
  styles: []
})
export class LivraisonsComponent {
  constructor(private logistics: LogisticsService) {}
  searchText: string = '';
  currentPage: number = 1;
  totalPages: number = 6;

  metricsData: Metric[] = [
    { title: 'Planifiées', value: '06', icon: 'box-blue' },
    { title: 'À confirmer', value: '05', icon: 'warning-yellow', },
    { title: 'Confirmées', value: '05', icon: 'check-blue' },
    { title: 'Livrées', value: '05', icon: 'check-green' },
    { title: 'Annulées', value: '01', icon: 'box-red' }
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

  selectedFournisseur = 'Tous les fournisseurs';
  selectedTransporteur = 'Tous les transporteurs';
  selectedStatus = 'Tous les statuts';
  showFournisseurDropdown = false;
  showTransporteurDropdown = false;
  showStatusDropdown = false;

  get uniqueFournisseurs(): string[] {
    const fournisseurs = new Set(this.livraisons.map(l => l.fournisseur));
    return ['Tous les fournisseurs', ...Array.from(fournisseurs)];
  }

  get uniqueTransporteurs(): string[] {
    const transporteurs = new Set(this.livraisons.map(l => l.transporteur));
    return ['Tous les transporteurs', ...Array.from(transporteurs)];
  }

  get uniqueStatuses(): string[] {
    const statuses = new Set(this.livraisons.map(l => l.statut));
    return ['Tous les statuts', ...Array.from(statuses)];
  }

  get filteredLivraisons(): Livraison[] {
    return this.livraisons.filter(l => {
      const matchesSearch =
        l.client.name.toLowerCase().includes(this.searchText.toLowerCase()) ||
        l.client.id.toLowerCase().includes(this.searchText.toLowerCase()) ||
        l.fournisseur.toLowerCase().includes(this.searchText.toLowerCase()) ||
        l.transporteur.toLowerCase().includes(this.searchText.toLowerCase());

      const matchesFournisseur = this.selectedFournisseur === 'Tous les fournisseurs' || l.fournisseur === this.selectedFournisseur;
      const matchesTransporteur = this.selectedTransporteur === 'Tous les transporteurs' || l.transporteur === this.selectedTransporteur;
      const matchesStatus = this.selectedStatus === 'Tous les statuts' || l.statut === this.selectedStatus;

      return matchesSearch && matchesFournisseur && matchesTransporteur && matchesStatus;
    });
  }

  toggleFournisseurDropdown() {
    this.showFournisseurDropdown = !this.showFournisseurDropdown;
    this.showTransporteurDropdown = false;
    this.showStatusDropdown = false;
  }

  toggleTransporteurDropdown() {
    this.showTransporteurDropdown = !this.showTransporteurDropdown;
    this.showFournisseurDropdown = false;
    this.showStatusDropdown = false;
  }

  toggleStatusDropdown() {
    this.showStatusDropdown = !this.showStatusDropdown;
    this.showFournisseurDropdown = false;
    this.showTransporteurDropdown = false;
  }

  selectFournisseur(val: string) {
    this.selectedFournisseur = val;
    this.showFournisseurDropdown = false;
  }

  selectTransporteur(val: string) {
    this.selectedTransporteur = val;
    this.showTransporteurDropdown = false;
  }

  selectStatus(val: string) {
    this.selectedStatus = val;
    this.showStatusDropdown = false;
  }

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

  // Modal Logic
  showModal = false;
  showDetailModal = false;
  selectedLivraison: Livraison | null = null;
  showCommandesDropdown = false; // New state for custom dropdown
  showReportModal = false;
  showEditModal = false;
  showCancelModal = false;
  cancelReason: string = '';
  selectedLivraisonToEdit: Livraison | null = null;
  selectedLivraisonToReport: Livraison | null = null;
  selectedLivraisonToCancel: Livraison | null = null;
  newReportDate: string = '';
  reportDateError: string = '';
  editTournee: any = {
    date: '',
    creneau: '',
    zone: '',
    transporteur: '',
    chauffeur: '',
    vehicule: '',
    commandes: [] as string[],
    note: ''
  };
  newTournee: {
    deliveryDate: string;
    driverId: number | null;
    vehiclePlate: string;
    vehicleType: string;
    orderIds: number[];
    notes: string;
    lotSize: number;
  } = {
    deliveryDate: '',
    driverId: null,
    vehiclePlate: '',
    vehicleType: '',
    orderIds: [],
    notes: '',
    lotSize: 5
  };

  errors: any = {
    deliveryDate: '',
    driverId: '',
    vehiclePlate: '',
    vehicleType: '',
    orderIds: ''
  };

  // Données chargées depuis l'API (modal Planifier)
  availableDrivers: AvailableDriver[] = [];
  groupedLots: EligibleOrderLot[] = [];
  loadingDrivers = false;
  loadingLots = false;

  // Pour le modal Modifier (données mock)
  creneaux = ['Matin (08h-12h)', 'Après-midi (14h-18h)', 'Soir (18h-20h)'];
  zones = ['Dakar', 'Thiès', 'Saint-Louis', 'Touba'];
  transporteursList = ['DHL', 'Chrono SN', 'Colis SN', 'Maersk'];
  commandesDisponibles = ['CMD-001', 'CMD-002', 'CMD-003', 'CMD-004'];

  get minDate(): string {
    return new Date().toISOString().split('T')[0];
  }

  openModal() {
    this.showModal = true;
    this.showCommandesDropdown = false;
    this.newTournee = {
      deliveryDate: '',
      driverId: null,
      vehiclePlate: '',
      vehicleType: '',
      orderIds: [],
      notes: '',
      lotSize: 5
    };
    this.groupedLots = [];
    this.errors = {
      deliveryDate: '',
      driverId: '',
      vehiclePlate: '',
      vehicleType: '',
      orderIds: ''
    };
    this.loadAvailableDrivers();
  }

  loadAvailableDrivers() {
    this.loadingDrivers = true;
    this.logistics.getAvailableDrivers().subscribe({
      next: (list) => {
        this.availableDrivers = list || [];
        this.loadingDrivers = false;
      },
      error: () => {
        this.loadingDrivers = false;
        this.availableDrivers = [];
      }
    });
  }

  /** Date au format dd-MM-yyyy pour l'API */
  toApiDate(dateStr: string): string {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    const day = String(d.getDate()).padStart(2, '0');
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const year = d.getFullYear();
    return `${day}-${month}-${year}`;
  }

  loadGroupedLots() {
    const dateStr = this.newTournee.deliveryDate;
    const lotSize = this.newTournee.lotSize;
    if (!dateStr || lotSize < 1) {
      this.groupedLots = [];
      return;
    }
    this.loadingLots = true;
    this.logistics.getGroupedEligibleOrders(this.toApiDate(dateStr), lotSize).subscribe({
      next: (lots) => {
        this.groupedLots = lots || [];
        this.loadingLots = false;
      },
      error: () => {
        this.groupedLots = [];
        this.loadingLots = false;
      }
    });
  }

  /** Toutes les commandes éligibles (tous lots) pour la sélection */
  get allEligibleOrders(): EligibleOrder[] {
    return this.groupedLots.flatMap(lot => lot.orders || []);
  }

  /** Numéros des commandes d'un lot (pour affichage dans le template). */
  getLotOrderNumbers(lot: EligibleOrderLot): string {
    const orders = lot.orders || [];
    return orders.map(or => or.orderNumber).join(', ');
  }

  closeModal() {
    this.showModal = false;
    this.showCommandesDropdown = false;
  }

  toggleCommandesDropdown() {
    this.showCommandesDropdown = !this.showCommandesDropdown;
  }

  toggleOrderSelection(order: EligibleOrder) {
    const id = order.orderId;
    const idx = this.newTournee.orderIds.indexOf(id);
    if (idx > -1) {
      this.newTournee.orderIds.splice(idx, 1);
    } else {
      this.newTournee.orderIds.push(id);
    }
  }

  isOrderSelected(order: EligibleOrder): boolean {
    return this.newTournee.orderIds.includes(order.orderId);
  }

  /** Modal Modifier : bascule sélection d'une commande (mock, par libellé). */
  toggleCommandeSelection(cmd: string) {
    const idx = this.editTournee.commandes.indexOf(cmd);
    if (idx > -1) {
      this.editTournee.commandes.splice(idx, 1);
    } else {
      this.editTournee.commandes.push(cmd);
    }
  }

  /** Modal Modifier : une commande est-elle sélectionnée (mock). */
  isCommandeSelected(cmd: string): boolean {
    return this.editTournee.commandes.includes(cmd);
  }

  getSelectedCommandesLabel(): string {
    if (this.newTournee.orderIds.length === 0) {
      return 'Sélectionner les commandes (multiple)';
    }
    return `${this.newTournee.orderIds.length} commande(s) sélectionnée(s)`;
  }

  /** deliveryDate (input date) -> yyyy-MM-dd pour l'API */
  toIsoDate(dateStr: string): string {
    if (!dateStr) return '';
    return dateStr; // input type="date" donne déjà yyyy-MM-dd
  }

  saveTournee() {
    this.errors = {
      deliveryDate: '',
      driverId: '',
      vehiclePlate: '',
      vehicleType: '',
      orderIds: ''
    };
    let isValid = true;

    if (!this.newTournee.deliveryDate) {
      this.errors.deliveryDate = 'La date est obligatoire';
      isValid = false;
    } else {
      const selectedDate = new Date(this.newTournee.deliveryDate);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      if (selectedDate < today) {
        this.errors.deliveryDate = 'La date ne peut pas être dans le passé';
        isValid = false;
      }
    }
    if (this.newTournee.driverId == null || this.newTournee.driverId === undefined) {
      this.errors.driverId = 'Le chauffeur est obligatoire';
      isValid = false;
    }
    if (!this.newTournee.vehiclePlate?.trim()) {
      this.errors.vehiclePlate = 'La plaque est obligatoire';
      isValid = false;
    }
    if (!this.newTournee.vehicleType?.trim()) {
      this.errors.vehicleType = 'Le type de véhicule est obligatoire';
      isValid = false;
    }
    if (!this.newTournee.orderIds?.length) {
      this.errors.orderIds = 'Sélectionnez au moins une commande';
      isValid = false;
    }
    if (!isValid) return;

    const payload = {
      deliveryDate: this.toIsoDate(this.newTournee.deliveryDate),
      driverId: this.newTournee.driverId!,
      vehiclePlate: this.newTournee.vehiclePlate.trim(),
      vehicleType: this.newTournee.vehicleType.trim(),
      orderIds: this.newTournee.orderIds,
      notes: this.newTournee.notes?.trim() || undefined
    };

    this.logistics.createDeliveryTour(payload).subscribe({
      next: () => {
        Swal.fire({ title: 'Succès', text: 'Tournée créée avec succès', icon: 'success', confirmButtonText: 'OK' });
        this.closeModal();
        // TODO: rafraîchir la liste des livraisons si elle vient de l'API
      },
      error: (err) => {
        const msg = err?.error?.message || err?.message || 'Erreur lors de la création de la tournée';
        const isNetworkError = err?.status === 0 || (typeof msg === 'string' && msg.toLowerCase().includes('fetch'));
        const displayMsg = isNetworkError
          ? 'Impossible de joindre le serveur. Vérifiez que le back-office est démarré (port 8082) et que la base de données est à jour (enum status, updated_by_id).'
          : msg;
        Swal.fire({ title: 'Erreur', text: displayMsg, icon: 'error', confirmButtonText: 'OK' });
      }
    });
  }

  // Action Menu Logic
  activeActionId: string | null = null;
  menuPosition = { left: 0, top: 0 };

  actionOptions = [
    { label: 'Voir détails', icon: '/icones/voir details.svg', action: 'view' },
    { label: 'Modifier', icon: '/icones/modifier.svg', action: 'edit' },
    { label: 'Confirmer', icon: '/icones/confirmer.svg', action: 'confirm', color: 'text-[#374151]' },
    { label: 'Reporter', icon: '/icones/reporter.svg', action: 'postpone' },
    { label: 'Annuler', icon: '/icones/annuler.svg', action: 'cancel', color: 'text-[#374151]' }
  ];

  toggleActionMenu(id: string, event: Event) {
    event.stopPropagation();
    if (this.activeActionId === id) {
      this.activeActionId = null;
    } else {
      this.activeActionId = id;
    }
  }

  closeActionMenu() {
    this.activeActionId = null;
  }

  handleAction(action: string, livraison: Livraison) {
    if (action === 'view') {
      this.openDetailModal(livraison);
    } else if (action === 'confirm') {
      this.confirmTournee(livraison);
    } else if (action === 'cancel') {
      this.cancelTournee(livraison);
    } else if (action === 'postpone') {
      this.openReportModal(livraison);
    } else if (action === 'edit') {
      this.openEditModal(livraison);
    } else {
      console.log(`Action: ${action} on livraison ${livraison.id}`);
    }
    this.closeActionMenu();
  }

  openDetailModal(livraison: Livraison) {
    this.selectedLivraison = livraison;
    this.showDetailModal = true;
  }

  closeDetailModal() {
    this.showDetailModal = false;
    this.selectedLivraison = null;
  }

  confirmTournee(livraison: Livraison) {
    Swal.fire({
      title: 'Confirmer la tournée ?',
      text: `${livraison.client.name} • ${livraison.date}`,
      iconHtml: `<img src="/icones/alerte.svg" alt="alert" style="width: 95px; height: 95px; margin: 0 auto;" />`,
      showCancelButton: true,
      confirmButtonText: 'Confirmer',
      cancelButtonText: 'Annuler',
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-3xl p-6',
        title: 'text-2xl font-medium text-gray-900',
        htmlContainer: 'text-lg text-gray-600',
        confirmButton: 'bg-[#22C55E] hover:bg-[#16A34A] text-white px-8 py-3 rounded-lg font-medium text-base shadow-none border-none ',
        cancelButton: 'bg-[#F3F4F6] hover:bg-gray-200 text-gray-700 px-8 py-3 rounded-lg font-medium text-base shadow-none border-none ',
        actions: 'flex justify-center w-full gap-2',
        icon: 'border-none'
      },
      backdrop: `rgba(0,0,0,0.2)`,
      width: '580px',
      showClass: {
        popup: 'animate__animated animate__fadeIn animate__faster'
      }
    }).then((result) => {
      if (result.isConfirmed) {
        livraison.statut = 'Confirmé';
        this.showSuccessMessage();
        this.closeDetailModal();
      }
    });
  }

  cancelTournee(livraison: Livraison) {
    this.selectedLivraisonToCancel = livraison;
    this.cancelReason = '';
    this.showCancelModal = true;
  }

  closeCancelModal() {
    this.showCancelModal = false;
    this.selectedLivraisonToCancel = null;
    this.cancelReason = '';
  }

  confirmCancelTournee() {
    if (!this.cancelReason.trim()) {
      return;
    }

    if (this.selectedLivraisonToCancel) {
      this.selectedLivraisonToCancel.statut = 'Annulé';
      this.closeCancelModal();
      this.showCancelSuccessMessage();
      this.closeDetailModal();
    }
  }

  showSuccessMessage() {
    Swal.fire({
      title: 'Tournée confirmée',
      iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
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

  showCancelSuccessMessage() {
    Swal.fire({
      title: 'Tournée annulée',
      iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
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

  openReportModal(livraison: Livraison) {
    this.selectedLivraisonToReport = livraison;
    this.newReportDate = livraison.date.split('/').reverse().join('-');
    this.reportDateError = '';
    this.showReportModal = true;
  }

  closeReportModal() {
    this.showReportModal = false;
    this.selectedLivraisonToReport = null;
    this.newReportDate = '';
    this.reportDateError = '';
  }

  reportLivraison() {
    this.reportDateError = '';

    if (!this.newReportDate) {
      this.reportDateError = 'La date est obligatoire';
      return;
    }

    const selectedDate = new Date(this.newReportDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (selectedDate < today) {
      this.reportDateError = 'La date ne peut pas être dans le passé';
      return;
    }

    if (this.selectedLivraisonToReport) {
      // Convertir la date au format DD/MM/YYYY
      const [year, month, day] = this.newReportDate.split('-');
      this.selectedLivraisonToReport.date = `${day}/${month}/${year}`;

      this.closeReportModal();
      this.showReportSuccessMessage();
      this.closeDetailModal();
    }
  }

  showReportSuccessMessage() {
    Swal.fire({
      title: 'Tournée reportée',
      iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
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

  openEditModal(livraison: Livraison) {
    this.selectedLivraisonToEdit = livraison;
    // Convertir la date DD/MM/YYYY en YYYY-MM-DD pour l'input date
    const [day, month, year] = livraison.date.split('/');
    this.editTournee = {
      date: `${year}-${month}-${day}`,
      creneau: 'Matin (08h-12h)', // Valeur par défaut, peut être personnalisée
      zone: 'Dakar', // Valeur par défaut, peut être personnalisée
      transporteur: livraison.transporteur,
      chauffeur: livraison.chauffeur?.name || '',
      vehicule: livraison.chauffeur?.vehicle || '',
      commandes: [],
      note: ''
    };
    this.errors = {
      date: '',
      chauffeur: '',
      vehicule: ''
    };
    this.showEditModal = true;
  }

  closeEditModal() {
    this.showEditModal = false;
    this.selectedLivraisonToEdit = null;
    this.editTournee = {
      date: '',
      creneau: '',
      zone: '',
      transporteur: '',
      chauffeur: '',
      vehicule: '',
      commandes: [],
      note: ''
    };
    this.errors = {
      date: '',
      chauffeur: '',
      vehicule: ''
    };
  }

  saveEditTournee() {
    // Reset errors
    this.errors = {
      date: '',
      chauffeur: '',
      vehicule: ''
    };

    let isValid = true;

    // Validation Date
    if (!this.editTournee.date) {
      this.errors.date = 'La date est obligatoire';
      isValid = false;
    } else {
      const selectedDate = new Date(this.editTournee.date);
      const today = new Date();
      today.setHours(0, 0, 0, 0);
      if (selectedDate < today) {
        this.errors.date = 'La date ne peut pas être dans le passé';
        isValid = false;
      }
    }

    // Validation Chauffeur
    if (!this.editTournee.chauffeur) {
      this.errors.chauffeur = 'Le chauffeur est obligatoire';
      isValid = false;
    }

    // Validation Véhicule
    if (!this.editTournee.vehicule) {
      this.errors.vehicule = 'Le véhicule est obligatoire';
      isValid = false;
    }

    if (!isValid) {
      return;
    }

    if (this.selectedLivraisonToEdit) {
      // Convertir la date au format DD/MM/YYYY
      const [year, month, day] = this.editTournee.date.split('-');
      this.selectedLivraisonToEdit.date = `${day}/${month}/${year}`;
      this.selectedLivraisonToEdit.transporteur = this.editTournee.transporteur;
      this.selectedLivraisonToEdit.chauffeur = {
        name: this.editTournee.chauffeur,
        vehicle: this.editTournee.vehicule
      };

      this.closeEditModal();
      this.showEditSuccessMessage();
      this.closeDetailModal();
    }
  }

  showEditSuccessMessage() {
    Swal.fire({
      title: 'Tournée modifiée',
      iconHtml: `<img src="/icones/message success.svg" alt="success" style="width: 95px; height: 95px; margin: 0 auto;" />`,
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

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    if (this.activeActionId) {
      this.activeActionId = null;
    }
  }
}
