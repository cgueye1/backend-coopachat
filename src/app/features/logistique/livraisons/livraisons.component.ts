import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { LogisticsService, EligibleOrder, EligibleOrderLot, AvailableDriver, DeliveryTourListItem, DeliveryTourDetails } from '../../../shared/services/logistics.service';
import { PAGE_SIZE_OPTIONS } from '../../../shared/constants/pagination';
import Swal from 'sweetalert2';

// MODIF: interfaces conservées pour les métriques (pourraient venir de l'API plus tard)
interface Metric {
  title: string;
  value: string;
  icon: string;
}


@Component({
  selector: 'app-livraisons',
  standalone: true,
  imports: [MainLayoutComponent, CommonModule, FormsModule],
  templateUrl: './livraisons.component.html',
  styles: []
})
export class LivraisonsComponent implements OnInit {
  constructor(private logistics: LogisticsService) {}

  ngOnInit(): void {
    this.loadDeliveryTours();
    this.loadDeliveryTourStats();
  }

  // MODIF: recherche par N° tour (envoyée à l'API)
  searchText: string = '';
  // MODIF: pagination côté API (currentPage 0-based pour l'API)
  currentPage: number = 0;
  totalPages: number = 0;
  totalElements: number = 0;
  pageSize: number = 10;
  pageSizeOptions = PAGE_SIZE_OPTIONS;

  // MODIF: liste chargée depuis l'API (plus de données statiques)
  deliveryTours: DeliveryTourListItem[] = [];
  loadingTours = false;

  /** Cartes statistiques (remplies par l'API GET /logistics/delivery-tours/stats) */
  metricsData: Metric[] = [
    { title: 'Assignées', value: '0', icon: 'box-blue' },
    { title: 'En cours', value: '0', icon: 'warning-yellow' },
    { title: 'Terminées', value: '0', icon: 'check-green' },
    { title: 'Annulées', value: '0', icon: 'box-red' }
  ];
  loadingStats = false;

  // Filtres : uniquement Statut et N° tour (alignés sur l'API)
  selectedStatus = 'Tous les statuts';
  showStatusDropdown = false;
  /** Valeurs pour le filtre statut (backend: ASSIGNEE, EN_COURS, TERMINEE, ANNULEE) */
  statusFilterOptions = [
    { label: 'Tous les statuts', value: '' },
    { label: 'Assignée', value: 'ASSIGNEE' },
    { label: 'En cours', value: 'EN_COURS' },
    { label: 'Terminée', value: 'TERMINEE' },
    { label: 'Annulée', value: 'ANNULEE' }
  ];

  /** Charge les statistiques des tournées (4 cartes en haut de page). */
  loadDeliveryTourStats() {
    this.loadingStats = true;
    this.logistics.getDeliveryTourStats().subscribe({
      next: (stats) => {
        this.metricsData = [
          { title: 'Assignées', value: String(stats.assignedTours ?? 0), icon: 'box-blue' },
          { title: 'En cours', value: String(stats.inProgressTours ?? 0), icon: 'warning-yellow' },
          { title: 'Terminées', value: String(stats.completedTours ?? 0), icon: 'check-green' },
          { title: 'Annulées', value: String(stats.cancelledTours ?? 0), icon: 'box-red' }
        ];
        this.loadingStats = false;
      },
      error: () => {
        this.loadingStats = false;
      }
    });
  }

  /** Charge la liste des tournées depuis l'API (N° Tour | Date | Chauffeur | Véhicule | Nb Cmd | Statut). */
  loadDeliveryTours() {
    this.loadingTours = true;
    const tourNumber = this.searchText?.trim() || undefined;
    const status = this.selectedStatus === 'Tous les statuts' ? undefined : this.getStatusApiValue(this.selectedStatus);
    this.logistics.getDeliveryTours(this.currentPage, this.pageSize, tourNumber, status).subscribe({
      next: (res) => {
        this.deliveryTours = res.content ?? [];
        this.totalElements = res.totalElements ?? 0;
        this.totalPages = res.totalPages ?? 0;
        this.currentPage = res.currentPage ?? 0;
        this.loadingTours = false;
      },
      error: () => {
        this.deliveryTours = [];
        this.loadingTours = false;
      }
    });
  }

  /** Retourne la valeur API du statut (ex. "Planifiée" -> "PLANIFIEE"). */
  getStatusApiValue(label: string): string | undefined {
    const opt = this.statusFilterOptions.find(o => o.label === label);
    return opt?.value || undefined;
  }

  goToPage(page: number) {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.loadDeliveryTours();
  }

  onPageSizeChange(size: number) {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadDeliveryTours();
  }

  /** Libellé français du statut backend pour l'affichage (4 statuts : ASSIGNEE, EN_COURS, TERMINEE, ANNULEE) */
  statusDisplay(status: string): string {
    const map: Record<string, string> = {
      ASSIGNEE: 'Assignée',
      EN_COURS: 'En cours',
      TERMINEE: 'Terminée',
      ANNULEE: 'Annulée'
    };
    return map[status] ?? status;
  }

  // MODIF: plus de filtres fournisseur/transporteur (non fournis par l'API liste)
  toggleStatusDropdown() {
    this.showStatusDropdown = !this.showStatusDropdown;
  }

  selectStatus(label: string) {
    this.selectedStatus = label;
    this.showStatusDropdown = false;
    this.currentPage = 0;
    this.loadDeliveryTours();
  }

  /** Recherche par N° tour : relancer la liste (page 0) */
  onSearch() {
    this.currentPage = 0;
    this.loadDeliveryTours();
  }

  /** Classes CSS pour le badge statut (ASSIGNEE, EN_COURS, TERMINEE, ANNULEE) */
  getStatusClass(status: string): string {
    switch (status) {
      case 'ASSIGNEE': return 'bg-[#4F46E50F] text-[#4F46E5]';
      case 'EN_COURS': return 'bg-[#EAB3080F] text-[#EAB308]';
      case 'TERMINEE': return 'bg-emerald-50 text-emerald-600';
      case 'ANNULEE': return 'bg-[#FF09090F] text-[#FF0909]';
      default: return 'bg-gray-50 text-gray-600';
    }
  }

  getStatusDotClass(status: string): string {
    switch (status) {
      case 'ASSIGNEE': return 'bg-[#4F46E5]';
      case 'EN_COURS': return 'bg-[#EAB308]';
      case 'TERMINEE': return 'bg-emerald-500';
      case 'ANNULEE': return 'bg-[#FF0909]';
      default: return 'bg-gray-500';
    }
  }

  // Modal Logic (MODIF: types alignés sur l'API tournées)
  showModal = false;
  /** Étape du modal Planifier : 1 = date + lots, 2 = sélection commandes + préférences, 3 = assigner (chauffeur, véhicule) */
  planifierStep: 1 | 2 | 3 = 1;
  showDetailModal = false;
  selectedLivraison: DeliveryTourListItem | null = null;
  /** Détails complets de la tournée (chargés via API à l'ouverture du modal). */
  selectedTourDetails: DeliveryTourDetails | null = null;
  loadingTourDetails = false;
  showCommandesDropdown = false;
  showReportModal = false;
  showEditModal = false;
  showCancelModal = false;
  cancelReason: string = '';
  selectedLivraisonToEdit: DeliveryTourListItem | null = null;
  selectedLivraisonToReport: DeliveryTourListItem | null = null;
  selectedLivraisonToCancel: DeliveryTourListItem | null = null;
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

  // Pour le modal Modifier (données chargées via API)
  editTourDetails: DeliveryTourDetails | null = null;
  /** IDs des commandes cochées dans le modal Modifier (pour pouvoir décocher). */
  editTourSelectedOrderIds: number[] = [];
  loadingEditDetails = false;
  creneaux = ['Matin (08h-12h)', 'Après-midi (14h-18h)', 'Soir (18h-20h)'];
  zones = ['Dakar', 'Thiès', 'Saint-Louis', 'Touba'];
  transporteursList = ['DHL', 'Chrono SN', 'Colis SN', 'Maersk'];
  commandesDisponibles = ['CMD-001', 'CMD-002', 'CMD-003', 'CMD-004'];

  get minDate(): string {
    return new Date().toISOString().split('T')[0];
  }

  /** Jour de la semaine en français pour la date de livraison (ex. "Samedi"). */
  getDeliveryDateDayName(): string {
    const dateStr = this.newTournee.deliveryDate;
    if (!dateStr) return '';
    const d = new Date(dateStr + 'T12:00:00');
    const days = ['Dimanche', 'Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi'];
    return days[d.getDay()] ?? '';
  }

  openModal() {
    this.showModal = true;
    this.planifierStep = 1;
    this.loadLotsMessage = '';
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

  /** Message d'erreur ou d'info après clic sur "Afficher les lots" (étape 1). */
  loadLotsMessage = '';

  loadGroupedLots() {
    this.loadLotsMessage = '';
    this.errors.deliveryDate = '';
    const dateStr = this.newTournee.deliveryDate;
    const lotSize = this.newTournee.lotSize ?? 5;
    if (!dateStr || !dateStr.trim()) {
      this.errors.deliveryDate = 'Veuillez choisir une date de livraison.';
      return;
    }
    if (lotSize < 1) {
      this.loadLotsMessage = 'La taille des lots doit être au moins 1.';
      return;
    }
    this.loadingLots = true;
    this.logistics.getGroupedEligibleOrders(this.toApiDate(dateStr), lotSize).subscribe({
      next: (lots) => {
        this.groupedLots = lots || [];
        this.loadingLots = false;
        if (this.groupedLots.length === 0) {
          this.loadLotsMessage = 'Aucune commande éligible pour cette date.';
        }
      },
      error: () => {
        this.groupedLots = [];
        this.loadingLots = false;
        this.loadLotsMessage = 'Erreur lors du chargement. Vérifiez votre connexion et réessayez.';
      }
    });
  }

  /** Charge les lots puis passe à l'étape 2 (même effet que Suivant). */
  loadGroupedLotsAndGoNext() {
    this.loadLotsMessage = '';
    this.errors.deliveryDate = '';
    const dateStr = this.newTournee.deliveryDate;
    const lotSize = this.newTournee.lotSize ?? 5;
    if (!dateStr || !dateStr.trim()) {
      this.errors.deliveryDate = 'Veuillez choisir une date de livraison.';
      return;
    }
    if (lotSize < 1) {
      this.loadLotsMessage = 'La taille des lots doit être au moins 1.';
      return;
    }
    this.loadingLots = true;
    this.logistics.getGroupedEligibleOrders(this.toApiDate(dateStr), lotSize).subscribe({
      next: (lots) => {
        this.groupedLots = lots || [];
        this.loadingLots = false;
        if (this.groupedLots.length === 0) {
          this.loadLotsMessage = 'Aucune commande éligible pour cette date.';
        } else {
          this.planifierStep = 2;
        }
      },
      error: () => {
        this.groupedLots = [];
        this.loadingLots = false;
        this.loadLotsMessage = 'Erreur lors du chargement. Vérifiez votre connexion et réessayez.';
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
    this.planifierStep = 1;
    this.showCommandesDropdown = false;
  }

  /** Passer à l’étape suivante (1→2 ou 2→3). */
  nextPlanifierStep() {
    if (this.planifierStep === 1) {
      this.errors.deliveryDate = '';
      if (!this.newTournee.deliveryDate) {
        this.errors.deliveryDate = 'Choisissez une date de livraison';
        return;
      }
      if (this.groupedLots.length === 0) {
        this.errors.deliveryDate = 'Cliquez sur "Afficher les lots" pour charger les commandes.';
        return;
      }
      this.planifierStep = 2;
    } else if (this.planifierStep === 2) {
      // On peut passer à l'étape 3 même sans sélection (validation au Planifier)
      if (false && this.newTournee.orderIds.length === 0) {
        return; // on peut bloquer ou laisser passer et valider à l’envoi
      }
      this.planifierStep = 3;
    }
  }

  /** Revenir à l’étape précédente. */
  prevPlanifierStep() {
    if (this.planifierStep === 2) this.planifierStep = 1;
    else if (this.planifierStep === 3) this.planifierStep = 2;
  }

  /** Nombre de commandes sélectionnées qui sont hors préférences (pour le message étape 3). */
  get countOrdersOutOfPreferences(): number {
    return this.allEligibleOrders.filter(o => this.newTournee.orderIds.includes(o.orderId) && this.getOrderPreferenceStatus(o) === 'out').length;
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

  /**
   * Libellé des préférences pour une commande (ex: "Lundi/Mercredi · Matin · Domicile").
   * Utilisé dans le multi-select pour afficher les préférences de livraison du client.
   */
  getOrderPreferenceLabel(order: EligibleOrder): string {
    if (!order.hasPreferences) return '';
    const days = (order.preferredDays ?? []).length
      ? order.preferredDays!.map(d => this.formatDayName(d)).join('/')
      : '';
    const slot = order.preferredTimeSlot ?? '';
    const mode = order.preferredDeliveryMode ?? '';
    const parts = [days, slot, mode].filter(Boolean);
    return parts.join(' · ');
  }

  /** Affiche un jour en français (LUNDI -> Lundi). */
  private formatDayName(day: string): string {
    const map: Record<string, string> = {
      LUNDI: 'Lundi', MARDI: 'Mardi', MERCREDI: 'Mercredi', JEUDI: 'Jeudi',
      VENDREDI: 'Vendredi', SAMEDI: 'Samedi', DIMANCHE: 'Dimanche',
      MONDAY: 'Lundi', TUESDAY: 'Mardi', WEDNESDAY: 'Mercredi', THURSDAY: 'Jeudi',
      FRIDAY: 'Vendredi', SATURDAY: 'Samedi', SUNDAY: 'Dimanche'
    };
    return map[day?.toUpperCase()] ?? day;
  }

  /**
   * Statut préférence pour l’icône : 'match' | 'out' | 'none'.
   * match = correspond aux préférences, out = hors préférences (livrable), none = aucune préférence.
   */
  getOrderPreferenceStatus(order: EligibleOrder): 'match' | 'out' | 'none' {
    if (!order.hasPreferences) return 'none';
    return order.matchesPreferences === true ? 'match' : 'out';
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
        this.loadDeliveryTours();
        this.loadDeliveryTourStats();
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

  // Action Menu Logic (MODIF: id peut être number pour les tournées API)
  activeActionId: string | number | null = null;
  menuPosition = { left: 0, top: 0 };

  /** Menu actions : Voir détails ; si statut Assignée, ajouter Annuler la tournée. */
  private readonly actionView: { label: string; icon: string; action: string; color?: string } = { label: 'Voir détails', icon: '/icones/voir details.svg', action: 'view' };
  private readonly actionCancel: { label: string; icon: string; action: string; color?: string } = { label: 'Annuler la tournée', icon: '/icones/alerte.svg', action: 'cancel', color: 'text-red-600' };

  getTourActionOptions(item: DeliveryTourListItem): { label: string; icon: string; action: string; color?: string }[] {
    const options: { label: string; icon: string; action: string; color?: string }[] = [this.actionView];
    if (item.status === 'ASSIGNEE') {
      options.push(this.actionCancel);
    }
    return options;
  }

  toggleActionMenu(id: string | number, event: Event) {
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

  handleAction(action: string, tour: DeliveryTourListItem) {
    if (action === 'view') {
      this.openDetailModal(tour);
    } else if (action === 'cancel') {
      this.cancelTournee(tour);
    } else if (action === 'postpone') {
      this.openReportModal(tour);
    }
    this.closeActionMenu();
  }

  openDetailModal(tour: DeliveryTourListItem) {
    this.selectedLivraison = tour;
    this.selectedTourDetails = null;
    this.showDetailModal = true;
    this.loadTourDetails(tour.id);
  }

  loadTourDetails(tourId: number) {
    this.loadingTourDetails = true;
    this.logistics.getDeliveryTourDetails(tourId).subscribe({
      next: (details) => {
        this.selectedTourDetails = details;
        this.loadingTourDetails = false;
      },
      error: () => {
        this.loadingTourDetails = false;
        this.selectedTourDetails = null;
      }
    });
  }

  closeDetailModal() {
    this.showDetailModal = false;
    this.selectedLivraison = null;
    this.selectedTourDetails = null;
  }

  /** Format date API (yyyy-MM-dd ou dd-MM-yyyy) vers affichage dd/MM/yyyy */
  formatTourDate(dateStr: string | undefined): string {
    if (!dateStr) return '—';
    const parts = dateStr.split(/[-T]/);
    if (parts.length >= 3) {
      const [y, m, d] = parts;
      return `${d.padStart(2, '0')}/${m.padStart(2, '0')}/${y}`;
    }
    if (dateStr.includes('/')) return dateStr;
    return dateStr;
  }

  removeOrderFromTour(tourId: number, orderId: number) {
    this.logistics.removeOrderFromTour(tourId, orderId).subscribe({
      next: () => {
        Swal.fire({ title: 'Succès', text: 'Commande retirée de la tournée', icon: 'success', timer: 2000, showConfirmButton: false });
        this.loadTourDetails(tourId);
        this.loadDeliveryTours();
        this.loadDeliveryTourStats();
      },
      error: (err) => {
        const msg = err?.error?.message || err?.message || 'Impossible de retirer la commande';
        Swal.fire({ title: 'Erreur', text: msg, icon: 'error', confirmButtonText: 'OK' });
      }
    });
  }

  /** Ouvre le modal Modifier à partir du détail (même tournée). Charge les détails API puis affiche note + commandes. */
  openEditModalFromDetail() {
    if (!this.selectedLivraison) return;
    this.selectedLivraisonToEdit = this.selectedLivraison;
    this.showDetailModal = false;
    this.showEditModal = true;
    this.editTourDetails = null;
    this.editTourSelectedOrderIds = [];
    this.loadTourDetailsForEdit(this.selectedLivraison.id);
  }

  /** Charge les détails de la tournée pour le modal Modifier (note + commandes comme à l'ajout). */
  loadTourDetailsForEdit(tourId: number) {
    this.loadingEditDetails = true;
    this.logistics.getDeliveryTourDetails(tourId).subscribe({
      next: (details) => {
        this.editTourDetails = details;
        const dateStr = details.deliveryDate != null ? this.formatDeliveryDateForInput(details.deliveryDate) : '';
        this.editTournee = {
          date: dateStr,
          creneau: this.editTournee.creneau ?? '',
          zone: this.editTournee.zone ?? '',
          transporteur: this.editTournee.transporteur ?? '',
          chauffeur: details.driverName ?? '',
          vehicule: (details.vehicleType ?? details.vehiclePlate ?? details.vehicle ?? '').trim(),
          commandes: [],
          note: details.notes ?? ''
        };
        this.editTourSelectedOrderIds = (details.orders ?? []).map(o => o.orderId);
        this.loadingEditDetails = false;
      },
      error: () => {
        this.loadingEditDetails = false;
        this.editTourDetails = null;
    this.editTourSelectedOrderIds = [];
      }
    });
  }

  /** Formate la date API (yyyy-MM-dd ou string) pour input type="date". */
  formatDeliveryDateForInput(deliveryDate: string | undefined): string {
    if (!deliveryDate) return '';
    const parts = String(deliveryDate).split(/[-T]/);
    if (parts.length >= 3) return `${parts[0]}-${parts[1]}-${parts[2]}`;
    return String(deliveryDate);
  }

  /** Jour de la semaine pour la date du modal Modifier. */
  getEditTourDayName(): string {
    const dateStr = this.editTourDetails?.deliveryDate ?? this.editTournee.date;
    if (!dateStr) return '';
    const s = String(dateStr).substring(0, 10);
    const d = new Date(s + 'T12:00:00');
    const days = ['Dimanche', 'Lundi', 'Mardi', 'Mercredi', 'Jeudi', 'Vendredi', 'Samedi'];
    return days[d.getDay()] ?? '';
  }

  /** Ajouter des commandes à la tournée (à venir : modal de sélection par date). */
  openAddOrderToTour() {
    Swal.fire({ title: 'À venir', text: 'L\'ajout de commandes à une tournée sera disponible prochainement.', icon: 'info', confirmButtonText: 'OK' });
  }

  confirmTournee(tour: DeliveryTourListItem) {
    Swal.fire({
      title: 'Confirmer la tournée ?',
      text: `${tour.tourNumber} • ${tour.deliveryDate}`,
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
        // TODO: appeler l'API de confirmation puis this.loadDeliveryTours()
        this.showSuccessMessage();
        this.closeDetailModal();
      }
    });
  }

  cancelTournee(tour: DeliveryTourListItem) {
    this.selectedLivraisonToCancel = tour;
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
    const tour = this.selectedLivraisonToCancel;
    if (!tour) return;
    this.logistics.cancelDeliveryTour(tour.id, { reason: this.cancelReason.trim() }).subscribe({
      next: () => {
        this.closeCancelModal();
        this.showCancelSuccessMessage();
        this.closeDetailModal();
        this.loadDeliveryTours();
        this.loadDeliveryTourStats();
      },
      error: (err: unknown) => {
        const msg = (err as { error?: { message?: string }; message?: string })?.error?.message
          ?? (err as { message?: string })?.message
          ?? 'Erreur lors de l\'annulation de la tournée.';
        Swal.fire({ title: 'Erreur', text: msg, icon: 'error', confirmButtonText: 'OK' });
      }
    });
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

  openReportModal(tour: DeliveryTourListItem) {
    this.selectedLivraisonToReport = tour;
    this.newReportDate = tour.deliveryDate || ''; // API renvoie déjà yyyy-MM-dd
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
      // TODO: appeler l'API de report puis this.loadDeliveryTours()
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

  openEditModal(tour: DeliveryTourListItem) {
    this.selectedLivraisonToEdit = tour;
    this.editTournee = {
      date: tour.deliveryDate || '',
      creneau: 'Matin (08h-12h)',
      zone: 'Dakar',
      transporteur: '',
      chauffeur: tour.driverName || '',
      vehicule: tour.vehicle || '',
      commandes: [],
      note: ''
    };
    this.errors = { date: '', chauffeur: '', vehicule: '' };
    this.showEditModal = true;
    this.loadTourDetailsForEdit(tour.id);
  }

  closeEditModal() {
    this.showEditModal = false;
    this.selectedLivraisonToEdit = null;
    this.editTourDetails = null;
    this.editTourSelectedOrderIds = [];
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

  /** Décocher / cocher une commande dans le modal Modifier. */
  toggleEditTourOrder(orderId: number) {
    const idx = this.editTourSelectedOrderIds.indexOf(orderId);
    if (idx >= 0) {
      const next = this.editTourSelectedOrderIds.filter(id => id !== orderId);
      if (next.length === 0) {
        Swal.fire({
          title: 'Impossible',
          text: 'Pas de tournée sans commande. Gardez au moins une commande.',
          icon: 'warning',
          confirmButtonText: 'OK'
        });
        return;
      }
      this.editTourSelectedOrderIds = next;
    } else {
      this.editTourSelectedOrderIds = [...this.editTourSelectedOrderIds, orderId];
    }
  }

  isEditTourOrderSelected(orderId: number): boolean {
    return this.editTourSelectedOrderIds.includes(orderId);
  }

  saveEditTournee() {
    this.errors = { date: '', chauffeur: '', vehicule: '' };
    if (!this.editTournee.vehicule?.trim()) {
      this.errors.vehicule = 'Le véhicule est obligatoire';
      return;
    }
    if (this.editTourSelectedOrderIds.length === 0) {
      Swal.fire({
        title: 'Impossible',
        text: 'Pas de tournée sans commande. Gardez au moins une commande.',
        icon: 'warning',
        confirmButtonText: 'OK'
      });
      return;
    }
    const tour = this.selectedLivraisonToEdit;
    if (!tour) return;
    this.logistics.updateDeliveryTour(tour.id, {
      vehicleInfo: this.editTournee.vehicule.trim(),
      notes: this.editTournee.note?.trim() || null,
      orderIds: this.editTourSelectedOrderIds
    }).subscribe({
      next: () => {
        this.closeEditModal();
        this.showEditSuccessMessage();
        this.loadDeliveryTours();
        this.loadDeliveryTourStats();
      },
      error: (err: unknown) => {
        const e = err as { error?: { message?: string } | string; message?: string };
        const msg = (typeof e?.error === 'object' && e?.error?.message)
          ?? (typeof e?.error === 'string' ? e.error : null)
          ?? e?.message
          ?? 'Erreur lors de la modification.';
        Swal.fire({ title: 'Erreur', text: String(msg), icon: 'error', confirmButtonText: 'OK' });
      }
    });
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
