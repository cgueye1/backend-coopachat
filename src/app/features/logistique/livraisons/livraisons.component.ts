import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { LogisticsService, EligibleOrder, EligibleOrderLot, AvailableDriver, DeliveryTourListItem, DeliveryTourDetails, DeliveryPlanningCalendarDay, DeliveryPlanningCalendarResponse, OrderInTour } from '../../../shared/services/logistics.service';
import { PAGE_SIZE_OPTIONS } from '../../../shared/constants/pagination';
import { finalize } from 'rxjs';
import Swal from 'sweetalert2';

// MODIF: interfaces conservées pour les métriques (pourraient venir de l'API plus tard)
interface Metric {
  title: string;
  value: string;
  icon: string;
}

/** Une entrée du menu « … » sur une ligne de tournée (références stables pour *ngFor). */
interface TourRowActionOption {
  label: string;
  icon: string;
  action: string;
  color?: string;
  disabled?: boolean;
  tooltip?: string;
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
    this.initCalendar();
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
  /** Export Excel des tournées en cours. */
  loadingExport = false;

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

  /** Charge les statistiques des tournées (total + 4 statuts en haut de page). */
  loadDeliveryTourStats() {
    this.loadingStats = true;
    this.logistics.getDeliveryTourStats().subscribe({
      next: (stats) => {
        this.metricsData = [
          { title: 'Total', value: String(stats.totalTours ?? 0), icon: 'check-blue' },
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

  /** Charge la liste des tournées depuis l'API (N° Tour | Date | Livreur | Véhicule | Nb Cmd | Statut). */
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

  /** Plaque sénégalaise attendue : AA-12345-AB (2 lettres, 5 chiffres, 2 lettres). */
  private static readonly VEHICLE_PLATE_REGEX = /^[A-Z]{2}-\d{5}-[A-Z]{2}$/;

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

  exportDeliveryToursList(): void {
    if (this.loadingExport) return;
    this.loadingExport = true;
    const tourNumber = this.searchText?.trim() || undefined;
    const status =
      this.selectedStatus === 'Tous les statuts' ? undefined : this.getStatusApiValue(this.selectedStatus);
    this.logistics
      .exportDeliveryTours(tourNumber, status)
      .pipe(finalize(() => { this.loadingExport = false; }))
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `tournees_livraison_${new Date().toISOString().slice(0, 10)}.xlsx`;
          a.click();
          window.URL.revokeObjectURL(url);
        },
        error: () => { /* blob ou erreur réseau */ }
      });
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

  /** Nom salarié sur une ligne (prénom + nom), sans labels « Prénom » / « Nom ». */
  tourOrderEmployeeDisplayName(order: OrderInTour): string {
    const f = order.employeeFirstName?.trim() ?? '';
    const l = order.employeeLastName?.trim() ?? '';
    const fromParts = `${f} ${l}`.trim();
    if (fromParts.length > 0) return fromParts;
    const single = order.employeeName?.trim() ?? '';
    return single.length > 0 ? single : '—';
  }

  /** Montant affichage (détail tournée / commandes). */
  formatTourMoney(amount: number | string | undefined | null): string {
    if (amount == null || amount === '') return '—';
    const n = typeof amount === 'string' ? parseFloat(amount) : Number(amount);
    if (Number.isNaN(n)) return '—';
    return `${Math.round(n).toLocaleString('fr-FR')} F CFA`;
  }

  /** Badge statut commande (OrderStatus côté API). */
  getOrderStatusBadgeClass(status: string | undefined): string {
    switch (status) {
      case 'LIVREE': return 'bg-emerald-50 text-emerald-700 border border-emerald-200';
      case 'ECHEC_LIVRAISON': return 'bg-red-50 text-red-700 border border-red-200';
      case 'EN_COURS': return 'bg-amber-50 text-amber-800 border border-amber-200';
      case 'ARRIVE': return 'bg-sky-50 text-sky-800 border border-sky-200';
      case 'EN_PREPARATION': return 'bg-indigo-50 text-indigo-700 border border-indigo-200';
      case 'VALIDEE': return 'bg-slate-50 text-slate-700 border border-slate-200';
      case 'ANNULEE': return 'bg-gray-100 text-gray-600 border border-gray-200';
      default: return 'bg-gray-50 text-gray-700 border border-gray-200';
    }
  }

  tourRecapTotalOrders(d: DeliveryTourDetails | null): number {
    if (!d) return 0;
    if (d.orderCount != null) return d.orderCount;
    return d.orders?.length ?? 0;
  }

  tourRecapDelivered(d: DeliveryTourDetails | null): number {
    if (!d) return 0;
    if (d.deliveredOrderCount != null) return d.deliveredOrderCount;
    return (d.orders || []).filter(o => o.orderStatus === 'LIVREE').length;
  }

  tourRecapFailed(d: DeliveryTourDetails | null): number {
    if (!d) return 0;
    if (d.failedOrderCount != null) return d.failedOrderCount;
    return (d.orders || []).filter(o => o.orderStatus === 'ECHEC_LIVRAISON').length;
  }

  tourRecapAmount(d: DeliveryTourDetails | null): string {
    if (!d) return '—';
    if (d.totalTourAmount != null) return this.formatTourMoney(d.totalTourAmount);
    const sum = (d.orders || []).reduce((s, o) => s + (Number(o.totalAmount) || 0), 0);
    return this.formatTourMoney(sum);
  }

  /** Ligne véhicule : type + plaque si distinctes. */
  tourVehicleLine(details: DeliveryTourDetails, fallback?: string): string {
    const t = details.vehicleType?.trim();
    const p = details.vehiclePlate?.trim();
    const v = details.vehicle?.trim();
    if (t && p && t !== p) return `${t} — ${p}`;
    if (t) return t;
    if (v) return v;
    return fallback?.trim() || '—';
  }

  /** Type + matricule (API séparée ou parsing "Camion — AB-12"). */
  /** Type + matricule pour le modal détail (évite double appel template). */
  get tourDetailVehicleParts(): { type: string; plate: string } {
    if (!this.selectedTourDetails) return { type: '—', plate: '—' };
    return this.tourVehicleParts(this.selectedTourDetails, this.selectedLivraison?.vehicle);
  }

  /** Affichage plaque (majuscules, format AA-12345-AB si possible). */
  formatVehiclePlate(plate: string | undefined | null): string {
    if (plate == null || plate === '' || plate === '—') return '—';
    const u = plate.trim().toUpperCase();
    const m = u.match(/^([A-Z]{2})-(\d{5})-([A-Z]{2})$/);
    if (m) return `${m[1]}-${m[2]}-${m[3]}`;
    return u;
  }

  tourVehicleParts(details: DeliveryTourDetails, fallback?: string): { type: string; plate: string } {
    const plateApi = details.vehiclePlate?.trim();
    const typeApi = details.vehicleType?.trim();
    if (plateApi) {
      return { type: typeApi || '—', plate: plateApi };
    }
    const line = typeApi || details.vehicle?.trim() || fallback?.trim() || '';
    if (!line) return { type: '—', plate: '—' };
    const seps = [' — ', ' – ', ' - ', '—', '–', '-'];
    for (const s of seps) {
      const i = line.indexOf(s);
      if (i > 0) {
        return {
          type: line.slice(0, i).trim() || '—',
          plate: line.slice(i + s.length).trim() || '—'
        };
      }
    }
    return { type: line, plate: '—' };
  }

  /** Motif d'annulation (champ API `cancellationReason` uniquement). */
  get cancellationReasonText(): string {
    const d = this.selectedTourDetails;
    if (!d || d.status !== 'ANNULEE') {
      return '';
    }
    return (d.cancellationReason ?? '').trim();
  }

  // Modal Logic (MODIF: types alignés sur l'API tournées)
  showModal = false;
  // Vue calendrier (vue globale avant planification)
  showCalendarModal = false;
  /** Tiroir (slide-over) affiché après clic sur une date du calendrier. */
  showCalendarQuickPlan = false;
  calendarYear: number = new Date().getFullYear();
  calendarMonth: number = new Date().getMonth() + 1; // 1-12
  calendarDays: DeliveryPlanningCalendarDay[] = [];
  /** Retards planifiables (toutes dates), renvoyé par l'API — pas limité au mois affiché. */
  calendarTotalOverdueGlobal = 0;
  calendarLoading = false;
  selectedCalendarDay: DeliveryPlanningCalendarDay | null = null;
  /** Commandes par tournée (taille lot) saisie par le RL depuis le calendrier. */
  calendarLotSize: number = 2;
  /**
   * Total commandes éligibles pour la date sélectionnée (GET eligible-orders/count), même périmètre que « Voir les lots ».
   * null pendant le chargement ou si l'appel échoue (le getter retombe alors sur le total calendrier).
   */
  selectedEligibleOrdersCount: number | null = null;
  loadingEligibleOrdersCount = false;
  /** Étape du modal Planifier : 2 = lots, 3 = attribution. (Étape 1 = calendrier) */
  planifierStep: 2 | 3 = 2;
  showDetailModal = false;
  selectedLivraison: DeliveryTourListItem | null = null;
  /** Détails complets de la tournée (chargés via API à l'ouverture du modal). */
  selectedTourDetails: DeliveryTourDetails | null = null;
  loadingTourDetails = false;
  /** Message si GET détails échoue (droits, réseau, etc.). */
  tourDetailsLoadError: string | null = null;
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

  /** Autocomplete livreur — étape Planifier (étape 3). */
  planDriverSearchQuery = '';
  planDriverDropdownOpen = false;
  private planDriverInputLock = false;

  /** Autocomplete livreur — modal Modifier. */
  editDriverSearchQuery = '';
  editDriverDropdownOpen = false;
  private editDriverInputLock = false;
  /** POST création tournée (bouton Planifier). */
  savingNewTour = false;
  /** PATCH modification tournée (bouton Enregistrer). */
  savingEditTour = false;
  /** POST annulation tournée (modal Annuler). */
  cancellingTour = false;

  // Pour le modal Modifier (données chargées via API)
  editTourDetails: DeliveryTourDetails | null = null;
  /** IDs des commandes cochées dans le modal Modifier (pour pouvoir décocher). */
  editTourSelectedOrderIds: number[] = [];
  loadingEditDetails = false;
  /** yyyy-MM-dd pour le champ date du modal Modifier */
  editTourDeliveryDateIso = '';
  /** Livreur sélectionné (id API) — modal Modifier */
  editTourDriverId: number | null = null;
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
    this.planifierStep = 2;
    this.loadLotsMessage = '';
    this.showCommandesDropdown = false;
    this.planDriverSearchQuery = '';
    this.planDriverDropdownOpen = false;
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
  }

  initCalendar() {
    const now = new Date();
    this.calendarYear = now.getFullYear();
    this.calendarMonth = now.getMonth() + 1;
    this.loadPlanningCalendar();
  }

  openCalendar() {
    this.showCalendarModal = true;
    this.selectedCalendarDay = null;
    this.showCalendarQuickPlan = false;
    if (this.calendarDays.length === 0) {
      this.loadPlanningCalendar();
    }
  }

  closeCalendar(clearSelection: boolean = true) {
    this.showCalendarModal = false;
    if (clearSelection) {
      this.selectedCalendarDay = null;
      this.showCalendarQuickPlan = false;
      this.selectedEligibleOrdersCount = null;
      this.loadingEligibleOrdersCount = false;
    }
  }

  prevMonth() {
    if (this.calendarMonth === 1) {
      this.calendarMonth = 12;
      this.calendarYear -= 1;
    } else {
      this.calendarMonth -= 1;
    }
    this.selectedCalendarDay = null;
    this.showCalendarQuickPlan = false;
    this.loadPlanningCalendar();
  }

  nextMonth() {
    if (this.calendarMonth === 12) {
      this.calendarMonth = 1;
      this.calendarYear += 1;
    } else {
      this.calendarMonth += 1;
    }
    this.selectedCalendarDay = null;
    this.showCalendarQuickPlan = false;
    this.loadPlanningCalendar();
  }

  loadPlanningCalendar() {
    this.calendarLoading = true;
    this.logistics.getPlanningCalendar(this.calendarYear, this.calendarMonth).subscribe({
      next: (res: DeliveryPlanningCalendarResponse | DeliveryPlanningCalendarDay[]) => {
        if (Array.isArray(res)) {
          this.calendarDays = res;
          this.calendarTotalOverdueGlobal = (res || []).reduce(
            (sum, d) => sum + (Number((d as DeliveryPlanningCalendarDay)?.overdueOrders) || 0),
            0
          );
        } else {
          this.calendarDays = res?.days || [];
          this.calendarTotalOverdueGlobal = Number(res?.totalOverdueGlobal) || 0;
        }
        this.resyncCalendarSelectionAfterReload();
        // Alignement strict avec la logique "éligible" (deliveryDate <= aujourd'hui) :
        // on recalcule le "retard global" à partir du compteur éligible du jour - commandes du jour.
        // (Ainsi, le badge "en retard" correspond au même périmètre que les lots.)
        this.refreshOverdueFromEligibleCount();
        this.calendarLoading = false;
      },
      error: () => {
        this.calendarDays = [];
        this.calendarTotalOverdueGlobal = 0;
        this.calendarLoading = false;
      }
    });
  }

  /**
   * Après rechargement du calendrier, réattache le jour sélectionné au nouvel objet du mois
   * (sinon les compteurs affichés restent ceux de l’ancienne référence).
   */
  private resyncCalendarSelectionAfterReload(): void {
    if (!this.selectedCalendarDay?.date) {
      return;
    }
    const key = this.selectedCalendarDay.date;
    const updated = this.calendarDays.find((d) => d.date === key);
    if (updated) {
      this.selectedCalendarDay = updated;
      this.refreshEligibleOrdersCountForSelectedDay();
    }
  }

  /** Recalcule les retards à partir du compteur éligible (today). */
  private refreshOverdueFromEligibleCount(): void {
    const todayIso = new Date().toISOString().slice(0, 10); // yyyy-MM-dd
    const apiDate = this.toApiDate(todayIso); // dd-MM-yyyy
    if (!apiDate) {
      return;
    }
    const todayDay = this.calendarDays.find((d) => this.isCalendarDayToday(d));
    const pendingToday = Number(todayDay?.pendingOrders) || 0;
    this.logistics.getEligibleOrdersCount(apiDate).subscribe({
      next: (n) => {
        const eligibleToday = Number(n) || 0;
        // eligibleToday inclut (retards + commandes du jour) → retards = eligibleToday - pendingToday
        this.calendarTotalOverdueGlobal = Math.max(0, Math.floor(eligibleToday - pendingToday));
      },
      error: () => {
        // En cas d'échec on garde la valeur renvoyée par l'API calendrier.
      }
    });
  }

  /** Libellé ex: "Mars 2026" */
  get calendarTitle(): string {
    const monthNames = [
      'Janvier','Février','Mars','Avril','Mai','Juin',
      'Juillet','Août','Septembre','Octobre','Novembre','Décembre'
    ];
    const label = monthNames[this.calendarMonth - 1] ?? '';
    return `${label} ${this.calendarYear}`;
  }

  /** Convertit dd/MM/yyyy -> yyyy-MM-dd pour input date et API grouped lots (via toApiDate ensuite). */
  private calendarDayToIso(day: DeliveryPlanningCalendarDay): string {
    const parts = day.date.split('/');
    if (parts.length !== 3) return '';
    const [dd, mm, yyyy] = parts;
    return `${yyyy}-${mm.padStart(2, '0')}-${dd.padStart(2, '0')}`;
  }

  /** True si le jour du calendrier correspond à aujourd'hui (jour/mois/année). */
  isCalendarDayToday(day: DeliveryPlanningCalendarDay): boolean {
    const iso = this.calendarDayToIso(day); // yyyy-MM-dd
    if (!iso) return false;
    const todayIso = new Date().toISOString().slice(0, 10); // yyyy-MM-dd (localisation indifférente pour jour)
    return iso === todayIso;
  }

  /** True si le jour du calendrier est dans le passé (strictement avant aujourd'hui). */
  isCalendarDayPast(day: DeliveryPlanningCalendarDay): boolean {
    const iso = this.calendarDayToIso(day);
    if (!iso) return false;
    const todayIso = new Date().toISOString().slice(0, 10);
    return iso < todayIso;
  }

  /** Clic sur un jour : on prépare le panneau "saisir un chiffre" (taille lot) avant de lancer le workflow. */
  selectCalendarDay(day: DeliveryPlanningCalendarDay) {
    if (this.isCalendarDayPast(day)) {
      return;
    }
    this.selectedCalendarDay = day;
    this.showCalendarQuickPlan = true;
    this.refreshEligibleOrdersCountForSelectedDay();
  }

  /** Ferme le tiroir calendrier sans fermer la modale mois. */
  dismissCalendarQuickPlan(): void {
    this.showCalendarQuickPlan = false;
    this.selectedCalendarDay = null;
    this.selectedEligibleOrdersCount = null;
    this.loadingEligibleOrdersCount = false;
  }

  /** Charge le décompte éligible (API) pour le jour sélectionné. */
  private refreshEligibleOrdersCountForSelectedDay(): void {
    const day = this.selectedCalendarDay;
    this.selectedEligibleOrdersCount = null;
    if (!day) {
      this.loadingEligibleOrdersCount = false;
      return;
    }
    const iso = this.calendarDayToIso(day);
    const apiDate = iso ? this.toApiDate(iso) : '';
    if (!apiDate) {
      this.loadingEligibleOrdersCount = false;
      return;
    }
    this.loadingEligibleOrdersCount = true;
    this.logistics.getEligibleOrdersCount(apiDate).subscribe({
      next: (n) => {
        this.selectedEligibleOrdersCount = Number(n);
        this.loadingEligibleOrdersCount = false;
      },
      error: () => {
        this.selectedEligibleOrdersCount = null;
        this.loadingEligibleOrdersCount = false;
      }
    });
  }

  /**
   * Total affiché dans le panneau planification : priorité au décompte API (éligibles),
   * sinon estimation à partir du calendrier (pending + retards pour aujourd'hui).
   */
  get displayTotalOrdersForPlanning(): number {
    if (this.selectedEligibleOrdersCount != null && !Number.isNaN(this.selectedEligibleOrdersCount)) {
      return Math.max(0, Math.floor(this.selectedEligibleOrdersCount));
    }
    return this.selectedTotalOrdersForPlanning;
  }

  /** Nb tournées à créer = ceil(total / commandesParTournee). */
  get toursToCreate(): number {
    const total = this.displayTotalOrdersForPlanning;
    const size = Math.max(1, Math.floor(Number(this.calendarLotSize || 1)));
    return total > 0 ? Math.ceil(total / size) : 0;
  }

  /**
   * Total des commandes en retard planifiables (toutes dates), fourni par l'API (totalOverdueGlobal).
   * Rétrocompat : si l'API renvoie encore un tableau seul, loadPlanningCalendar remplit calendarTotalOverdueGlobal via la somme des overdueOrders du mois.
   */
  get totalOverdueOrdersInView(): number {
    return Math.max(0, Math.floor(Number(this.calendarTotalOverdueGlobal) || 0));
  }

  /**
   * Total à prendre en compte pour le calcul des tournées.
   * - Si on sélectionne aujourd'hui: commandes du jour + toutes les commandes en retard (jours < aujourd'hui).
   * - Sinon: seulement les commandes du jour sélectionné.
   */
  get selectedTotalOrdersForPlanning(): number {
    const selected = this.selectedCalendarDay;
    if (!selected) return 0;
    const pending = Number(selected.pendingOrders) || 0;
    return this.isCalendarDayToday(selected) ? (pending + this.totalOverdueOrdersInView) : pending;
  }

  /** Ouvre le workflow existant de planification, prérempli avec la date choisie + taille lot. */
  startPlanningFromCalendar() {
    if (!this.selectedCalendarDay) return;
    const iso = this.calendarDayToIso(this.selectedCalendarDay);
    if (!iso) return;
    // On conserve la sélection du calendrier pour pouvoir revenir via "Retour" (lots -> calendrier).
    this.closeCalendar(false);
    this.openModal();
    this.newTournee.deliveryDate = iso;
    // Le backend groupe par "lotSize" = commandes par tournée.
    this.newTournee.lotSize = Math.max(1, Math.floor(Number(this.calendarLotSize || 1)));
    // On évite de ré-afficher "Date" : on charge directement les lots et on passe à l'étape 2.
    this.loadGroupedLotsAndGoNext();
  }

  /**
   * Livreurs pour la création de tournée : exclus s'ils ont déjà ASSIGNEE/EN_COURS à la date de livraison choisie.
   */
  loadAvailableDrivers() {
    const dateStr = this.newTournee.deliveryDate?.trim();
    const apiDate = dateStr ? this.toApiDate(dateStr) : undefined;
    this.loadingDrivers = true;
    this.logistics.getAvailableDrivers(apiDate).subscribe({
      next: (list) => {
        this.availableDrivers = list || [];
        if (this.newTournee.driverId != null && !this.availableDrivers.some((d) => d.driverId === this.newTournee.driverId)) {
          this.newTournee.driverId = null;
          this.planDriverSearchQuery = '';
        }
        if (this.newTournee.driverId != null) {
          const picked = this.availableDrivers.find((x) => x.driverId === this.newTournee.driverId);
          if (picked) {
            this.planDriverSearchQuery = picked.fullName ?? '';
          }
        }
        this.loadingDrivers = false;
      },
      error: () => {
        this.loadingDrivers = false;
        this.availableDrivers = [];
        this.planDriverSearchQuery = '';
        this.newTournee.driverId = null;
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
    this.savingNewTour = false;
    this.showModal = false;
    this.planifierStep = 2;
    this.showCommandesDropdown = false;
    this.planDriverSearchQuery = '';
    this.planDriverDropdownOpen = false;
  }

  /** Filtre livreurs par nom (autocomplete). */
  private filterDriversByQuery(list: AvailableDriver[], q: string): AvailableDriver[] {
    const t = q.trim().toLowerCase();
    if (!t) {
      return list;
    }
    return list.filter((d) => (d.fullName ?? '').toLowerCase().includes(t));
  }

  get filteredPlanningDrivers(): AvailableDriver[] {
    return this.filterDriversByQuery(this.availableDrivers, this.planDriverSearchQuery);
  }

  get filteredEditDrivers(): AvailableDriver[] {
    return this.filterDriversByQuery(this.availableDrivers, this.editDriverSearchQuery);
  }

  selectPlanDriver(event: MouseEvent, d: AvailableDriver) {
    event.preventDefault();
    this.planDriverInputLock = true;
    this.newTournee.driverId = d.driverId;
    this.planDriverSearchQuery = d.fullName ?? '';
    this.planDriverDropdownOpen = false;
    this.errors.driverId = '';
    queueMicrotask(() => {
      this.planDriverInputLock = false;
    });
  }

  onPlanDriverInput() {
    if (this.planDriverInputLock) {
      return;
    }
    this.errors.driverId = '';
    this.newTournee.driverId = null;
  }

  onPlanDriverFocus() {
    this.planDriverDropdownOpen = true;
  }

  onPlanDriverBlur() {
    setTimeout(() => {
      this.planDriverDropdownOpen = false;
    }, 200);
  }

  togglePlanDriverDropdown(event: MouseEvent) {
    event.preventDefault();
    if (this.loadingDrivers) {
      return;
    }
    this.planDriverDropdownOpen = !this.planDriverDropdownOpen;
  }

  selectEditDriver(event: MouseEvent, d: AvailableDriver) {
    event.preventDefault();
    this.editDriverInputLock = true;
    this.editTourDriverId = d.driverId;
    this.editDriverSearchQuery = d.fullName ?? '';
    this.editDriverDropdownOpen = false;
    this.errors.chauffeur = '';
    queueMicrotask(() => {
      this.editDriverInputLock = false;
    });
  }

  onEditDriverInput() {
    if (this.editDriverInputLock) {
      return;
    }
    this.errors.chauffeur = '';
    this.editTourDriverId = null;
  }

  onEditDriverFocus() {
    this.editDriverDropdownOpen = true;
  }

  onEditDriverBlur() {
    setTimeout(() => {
      this.editDriverDropdownOpen = false;
    }, 200);
  }

  toggleEditDriverDropdown(event: MouseEvent) {
    event.preventDefault();
    if (this.loadingDrivers) {
      return;
    }
    this.editDriverDropdownOpen = !this.editDriverDropdownOpen;
  }

  /** Étape 2 → 3 : lots chargés + au moins une commande cochée. */
  get canGoToPlanningStep3(): boolean {
    return (
      !this.loadingLots &&
      this.groupedLots.length > 0 &&
      this.newTournee.orderIds.length > 0
    );
  }

  /** Validation live de la plaque en étape 3 (AA-12345-AB). */
  get isPlanningPlateInvalid(): boolean {
    const plateRaw = this.newTournee.vehiclePlate?.trim() || '';
    if (!plateRaw) return false;
    return !LivraisonsComponent.VEHICLE_PLATE_REGEX.test(plateRaw.toUpperCase());
  }

  /** Étape 3 : champs obligatoires remplis avant envoi API. */
  get canSubmitPlanningTour(): boolean {
    if (this.planifierStep !== 3 || this.savingNewTour || this.loadingDrivers) {
      return false;
    }
    if (!this.newTournee.deliveryDate?.trim() || this.newTournee.orderIds.length === 0) {
      return false;
    }
    if (this.newTournee.driverId == null) {
      return false;
    }
    const plateRaw = this.newTournee.vehiclePlate?.trim() || '';
    if (!plateRaw || this.isPlanningPlateInvalid) {
      return false;
    }
    return !!this.newTournee.vehicleType?.trim();
  }

  /** Passer à l’étape suivante (2→3). */
  nextPlanifierStep() {
    if (this.planifierStep === 2) {
      this.errors.orderIds = '';
      if (this.loadingLots) {
        return;
      }
      if (this.groupedLots.length === 0) {
        this.loadLotsMessage = 'Aucun lot chargé. Revenez au calendrier et cliquez sur "Voir les lots".';
        return;
      }
      if (this.newTournee.orderIds.length === 0) {
        this.errors.orderIds = 'Sélectionnez au moins une commande pour cette tournée.';
        return;
      }
      this.planifierStep = 3;
      this.loadAvailableDrivers();
    }
  }

  /** Revenir à l’étape précédente. */
  prevPlanifierStep() {
    if (this.planifierStep === 3) {
      this.planifierStep = 2;
      return;
    }
    // Depuis les lots -> retour au calendrier + tiroir "commandes par tournée"
    this.showModal = false;
    this.showCalendarModal = true;
    this.showCalendarQuickPlan = true;
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
    if (this.newTournee.orderIds.length > 0) {
      this.errors.orderIds = '';
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
    if (this.planifierStep !== 3) {
      void Swal.fire({
        icon: 'warning',
        title: 'Étapes incomplètes',
        text: 'Validez d’abord l’étape des lots (commandes à inclure), puis l’attribution (livreur, véhicule), avant de planifier.',
        confirmButtonText: 'OK'
      });
      return;
    }
    if (this.loadingLots || this.loadingDrivers) {
      return;
    }

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
      this.errors.driverId = 'Le livreur est obligatoire';
      isValid = false;
    }
    const plateRaw = this.newTournee.vehiclePlate?.trim() || '';
    if (!plateRaw) {
      this.errors.vehiclePlate = 'La plaque est obligatoire';
      isValid = false;
    } else {
      const plateNorm = plateRaw.toUpperCase();
      if (!LivraisonsComponent.VEHICLE_PLATE_REGEX.test(plateNorm)) {
        this.errors.vehiclePlate = 'Format plaque : AA-12345-AB (2 lettres, tiret, 5 chiffres, tiret, 2 lettres)';
        isValid = false;
      }
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
      vehiclePlate: this.newTournee.vehiclePlate.trim().toUpperCase(),
      vehicleType: this.newTournee.vehicleType.trim(),
      orderIds: this.newTournee.orderIds,
      notes: this.newTournee.notes?.trim() || undefined
    };

    this.savingNewTour = true;
    this.logistics
      .createDeliveryTour(payload)
      .pipe(finalize(() => (this.savingNewTour = false)))
      .subscribe({
        next: () => {
          Swal.fire({ title: 'Succès', text: 'Tournée créée avec succès', icon: 'success', confirmButtonText: 'OK' });
          this.closeModal();
          this.loadDeliveryTours();
          this.loadDeliveryTourStats();
          this.loadPlanningCalendar();
          this.selectedCalendarDay = null;
          this.showCalendarQuickPlan = false;
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

  /**
   * Cache des entrées de menu par statut : même référence de tableau / d’objets à chaque CD.
   * Sans cela, *ngFor recréait les lignes (et les <img>) à chaque cycle ; l’événement load des images
   * relançait Zone.js → détection des changements en boucle → gel du navigateur (« Page ne répond pas »).
   */
  private readonly tourActionOptionsByStatus = new Map<string, TourRowActionOption[]>();

  /** trackBy lignes tournées */
  trackByTourId(_index: number, item: DeliveryTourListItem): number {
    return item.id;
  }

  /** trackBy entrées du menu contextuel */
  trackByTourActionOption(_index: number, option: TourRowActionOption): string {
    return `${option.action}-${option.disabled ? '1' : '0'}`;
  }

  private ensureTourActionMenusInitialized(): void {
    if (this.tourActionOptionsByStatus.size > 0) {
      return;
    }
    const view: TourRowActionOption = { label: 'Voir détails', icon: '/icones/voir details.svg', action: 'view' };
    const editOff: TourRowActionOption = {
      label: 'Modifier',
      icon: '/icones/modifier.svg',
      action: 'edit',
      disabled: true,
      tooltip: 'Tournée déjà démarrée'
    };
    const editOn: TourRowActionOption = {
      label: 'Modifier',
      icon: '/icones/modifier.svg',
      action: 'edit',
      disabled: false
    };
    const cancel: TourRowActionOption = {
      label: 'Annuler la tournée',
      icon: '/icones/alerte.svg',
      action: 'cancel',
      color: 'text-red-600'
    };
    const viewOnly: TourRowActionOption[] = [view];
    this.tourActionOptionsByStatus.set('ASSIGNEE', [view, editOn, cancel]);
    this.tourActionOptionsByStatus.set('EN_COURS', [view, editOff]);
    this.tourActionOptionsByStatus.set('TERMINEE', viewOnly);
    this.tourActionOptionsByStatus.set('ANNULEE', viewOnly);
  }

  getTourActionOptions(item: DeliveryTourListItem): TourRowActionOption[] {
    this.ensureTourActionMenusInitialized();
    const key = item.status ?? '';
    const cached = this.tourActionOptionsByStatus.get(key);
    if (cached) {
      return cached;
    }
    const fallback = this.tourActionOptionsByStatus.get('TERMINEE')!;
    return fallback;
  }

  onTourActionClick(option: TourRowActionOption, item: DeliveryTourListItem, event: Event) {
    if (option.disabled) {
      event.preventDefault();
      event.stopPropagation();
      return;
    }
    event.stopPropagation();
    this.handleAction(option.action, item);
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
    } else if (action === 'edit') {
      this.openEditModal(tour);
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
    this.tourDetailsLoadError = null;
    this.showDetailModal = true;
    this.loadTourDetails(tour.id);
  }

  loadTourDetails(tourId: number) {
    this.loadingTourDetails = true;
    this.tourDetailsLoadError = null;
    this.logistics.getDeliveryTourDetails(tourId).subscribe({
      next: (details) => {
        this.selectedTourDetails = details;
        this.loadingTourDetails = false;
      },
      error: (err: unknown) => {
        this.loadingTourDetails = false;
        this.selectedTourDetails = null;
        const status = (err as { status?: number })?.status;
        if (status === 401 || status === 403) {
          this.tourDetailsLoadError =
            'Accès refusé ou session expirée. Reconnectez-vous ou vérifiez les droits logistique.';
        } else if (status === 404) {
          this.tourDetailsLoadError = 'Cette tournée est introuvable (déjà supprimée ou identifiant invalide).';
        } else if (status === 0 || status === undefined) {
          this.tourDetailsLoadError =
            'Impossible de joindre le serveur. Vérifiez la connexion et que l’API est démarrée.';
        } else {
          this.tourDetailsLoadError = 'Impossible de charger le détail de la tournée. Réessayez plus tard.';
        }
      }
    });
  }

  closeDetailModal() {
    this.showDetailModal = false;
    this.selectedLivraison = null;
    this.selectedTourDetails = null;
    this.tourDetailsLoadError = null;
  }

  /** Format date API (yyyy-MM-dd | dd-MM-yyyy | dd/MM/yyyy) vers affichage dd/MM/yyyy */
  formatTourDate(dateStr: string | undefined): string {
    if (!dateStr) return '—';
    const raw = String(dateStr).substring(0, 10);
    const sep = raw.includes('-') ? '-' : (raw.includes('/') ? '/' : null);
    if (!sep) return raw;
    const parts = raw.split(sep);
    if (parts.length < 3) return raw;
    // yyyy-MM-dd -> dd/MM/yyyy
    if (parts[0].length === 4) {
      const [y, m, d] = parts;
      return `${String(d).padStart(2, '0')}/${String(m).padStart(2, '0')}/${String(y)}`;
    }
    // dd-MM-yyyy or dd/MM/yyyy -> dd/MM/yyyy
    if (parts[2].length === 4) {
      const [d, m, y] = parts;
      return `${String(d).padStart(2, '0')}/${String(m).padStart(2, '0')}/${String(y)}`;
    }
    return raw;
  }

  /** Ouvre le modal Modifier à partir du détail (même tournée). Charge les détails API puis affiche note + commandes. */
  openEditModalFromDetail() {
    if (!this.selectedLivraison || !this.selectedTourDetails) return;
    if (this.selectedTourDetails.status !== 'ASSIGNEE') return;
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
          vehicule: this.formatVehicleLineForEdit(details),
          commandes: [],
          note: details.notes ?? ''
        };
        this.editTourSelectedOrderIds = (details.orders ?? []).map(o => o.orderId);
        this.editTourDeliveryDateIso = dateStr;
        this.editTourDriverId = details.driverId ?? null;
        this.editDriverSearchQuery = (details.driverName ?? '').trim();
        this.ensureAvailableDriversForEdit();
        this.loadingEditDetails = false;
      },
      error: () => {
        this.loadingEditDetails = false;
        this.editTourDetails = null;
        this.editTourSelectedOrderIds = [];
        this.editTourDeliveryDateIso = '';
        this.editTourDriverId = null;
        this.editDriverSearchQuery = '';
      }
    });
  }

  /** Liste des livreurs pour le select du modal Modifier (hors occupés à la date, sauf la tournée en cours d’édition). */
  ensureAvailableDriversForEdit() {
    const iso = this.editTourDeliveryDateIso?.trim();
    const apiDate = iso ? this.toApiDate(iso) : undefined;
    const excludeTourId = this.selectedLivraisonToEdit?.id;
    this.loadingDrivers = true;
    this.logistics.getAvailableDrivers(apiDate, excludeTourId).subscribe({
      next: (list) => {
        this.availableDrivers = list || [];
        if (this.editTourDriverId != null && !this.availableDrivers.some((d) => d.driverId === this.editTourDriverId)) {
          this.editTourDriverId = null;
          this.editDriverSearchQuery = '';
        }
        if (this.editTourDriverId != null) {
          const picked = this.availableDrivers.find((x) => x.driverId === this.editTourDriverId);
          if (picked) {
            this.editDriverSearchQuery = picked.fullName ?? '';
          }
        }
        this.loadingDrivers = false;
      },
      error: () => {
        this.availableDrivers = [];
        this.loadingDrivers = false;
        this.editDriverSearchQuery = '';
        this.editTourDriverId = null;
      }
    });
  }

  /** Recharge les livreurs quand la date de livraison change dans le modal Modifier. */
  onEditTourDeliveryDateChanged() {
    this.errors.date = '';
    this.ensureAvailableDriversForEdit();
  }

  /** Type + plaque pour le champ « Véhicule » du modal Modifier (aligné sur le stockage « Type — Plaque »). */
  formatVehicleLineForEdit(details: DeliveryTourDetails): string {
    const t = (details.vehicleType ?? '').trim();
    const p = (details.vehiclePlate ?? '').trim();
    if (t && p) {
      return `${t} — ${p}`;
    }
    if (t) return t;
    if (p) return p;
    return (details.vehicle ?? '').trim();
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
    const dateStr = this.editTourDeliveryDateIso || this.formatDeliveryDateForInput(this.editTourDetails?.deliveryDate) || this.editTournee.date;
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
    this.cancellingTour = false;
    this.showCancelModal = true;
  }

  closeCancelModal() {
    this.cancellingTour = false;
    this.showCancelModal = false;
    this.selectedLivraisonToCancel = null;
    this.cancelReason = '';
  }

  confirmCancelTournee() {
    if (this.cancellingTour) {
      return;
    }
    if (!this.cancelReason.trim()) {
      return;
    }
    const tour = this.selectedLivraisonToCancel;
    if (!tour) return;
    this.cancellingTour = true;
    this.logistics
      .cancelDeliveryTour(tour.id, { reason: this.cancelReason.trim() })
      .pipe(finalize(() => (this.cancellingTour = false)))
      .subscribe({
        next: () => {
          this.closeCancelModal();
          this.showCancelSuccessMessage();
          this.closeDetailModal();
          this.loadDeliveryTours();
          this.loadDeliveryTourStats();
          this.loadPlanningCalendar();
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
    if (tour.status !== 'ASSIGNEE') {
      void Swal.fire({
        title: 'Modification impossible',
        text:
          tour.status === 'EN_COURS'
            ? 'Tournée déjà démarrée.'
            : tour.status === 'TERMINEE'
              ? 'Tournée terminée.'
              : tour.status === 'ANNULEE'
                ? 'Tournée annulée.'
                : 'Seules les tournées au statut Assignée peuvent être modifiées.',
        icon: 'warning',
        confirmButtonText: 'OK'
      });
      return;
    }
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
    this.editDriverSearchQuery = '';
    this.editDriverDropdownOpen = false;
    this.showEditModal = true;
    this.loadTourDetailsForEdit(tour.id);
  }

  closeEditModal() {
    this.savingEditTour = false;
    this.showEditModal = false;
    this.editDriverSearchQuery = '';
    this.editDriverDropdownOpen = false;
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
    this.editTourDeliveryDateIso = '';
    this.editTourDriverId = null;
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
    if (!this.editTourDeliveryDateIso?.trim()) {
      this.errors.date = 'La date de livraison est obligatoire';
      return;
    }
    if (this.editTourDriverId == null) {
      this.errors.chauffeur = 'Le livreur est obligatoire';
      return;
    }
    const veh = this.editTournee.vehicule?.trim() || '';
    if (!veh) {
      this.errors.vehicule = 'Le véhicule est obligatoire';
      return;
    }
    const plateInLine = veh.toUpperCase().match(/[A-Z]{2}-\d{5}-[A-Z]{2}/);
    if (!plateInLine) {
      this.errors.vehicule = 'Incluez la plaque au format AA-12345-AB (ex. Camion — AA-12345-AB)';
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
    const vehicleInfo = veh.replace(/[A-Za-z]{2}-\d{5}-[A-Za-z]{2}/g, (m: string) => m.toUpperCase());
    this.savingEditTour = true;
    this.logistics
      .updateDeliveryTour(tour.id, {
        vehicleInfo,
        notes: this.editTournee.note?.trim() || null,
        orderIds: this.editTourSelectedOrderIds,
        deliveryDate: this.editTourDeliveryDateIso.trim(),
        driverId: this.editTourDriverId
      })
      .pipe(finalize(() => (this.savingEditTour = false)))
      .subscribe({
        next: () => {
          this.closeEditModal();
          this.showEditSuccessMessage();
          this.loadDeliveryTours();
          this.loadDeliveryTourStats();
          this.loadPlanningCalendar();
        },
        error: (err: unknown) => {
          const e = err as { error?: { message?: string } | string; message?: string };
          let msg: string | null = null;
          if (typeof e?.error === 'string' && e.error.trim()) {
            msg = e.error;
          } else if (typeof e?.error === 'object' && e?.error?.message) {
            msg = String(e.error.message);
          }
          if (!msg) {
            msg = e?.message ?? 'Erreur lors de la modification.';
          }
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
    const t = event.target;
    // SVG (path, etc.) n’est pas HTMLElement : closest doit quand même être testé.
    if (t instanceof Element && typeof t.closest === 'function' && t.closest('[data-tour-actions]')) {
      return;
    }
    if (this.activeActionId !== null) {
      this.activeActionId = null;
    }
  }
}
