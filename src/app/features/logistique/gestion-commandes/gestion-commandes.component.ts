import { Component, HostListener, ViewChild, ElementRef, AfterViewInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { LogisticsService, EmployeeOrderDetails } from '../../../shared/services/logistics.service';
import { PAGE_SIZE_OPTIONS } from '../../../shared/constants/pagination';
import { environment } from '../../../../environments/environment';
import { isPlatformBrowser } from '@angular/common';
import { finalize } from 'rxjs';

Chart.register(...registerables);

interface MetricCard {
  title: string;
  value: string;
  icon: string;
  subtitle?: string;
  description?: string;
}

interface Commande {
  id: number;
  numero: string;
  salarie: string;
  /** Date à laquelle le salarié a passé la commande. */
  dateCommande: string;
  /** Date à laquelle la commande est passée à son statut actuel. */
  dateStatut?: string;
  /** Liste des noms de produits (chaque produit dans son propre bloc) */
  produitsList: string[];
  frequence: string;
  statut: 'En cours' | 'Livrée' | 'En attente' | 'Validée' | 'En préparation' | 'Annulée' | 'Échec';
  reference: string;
  note?: string;
  produitsDetails?: { productId: string; quantity: number }[];
}

@Component({
  selector: 'app-gestion-commandes',
  standalone: true,
  imports: [MainLayoutComponent, CommonModule, FormsModule],
  templateUrl: './gestion-commandes.component.html',
  styleUrl: './gestion-commandes.component.css'
})
export class GestionCommandesComponent implements AfterViewInit {
  @ViewChild('frequentProductsChart') frequentProductsChart!: ElementRef<HTMLCanvasElement>;
  @ViewChild('dailyOrdersChart') dailyOrdersChart!: ElementRef<HTMLCanvasElement>;

  private frequentProductsChartInstance?: Chart;
  private dailyOrdersChartInstance?: Chart;
  private readonly isBrowser: boolean;

  showDetailModal = false;
  selectedCommande: Commande | null = null;
  /** Détails de la commande (produits avec image) chargés via API à l'ouverture du modal */
  selectedOrderDetails: EmployeeOrderDetails | null = null;
  loadingOrderDetails = false;
  loadOrderDetailsError = false;
  loadingStats = false;
  statsError = false;

  /** Cartes de stats (EN ATTENTE, EN RETARD, EN COURS, LIVRÉES) chargées via API. */
  metricsData: MetricCard[] = [];

  searchTerm = '';
  selectedStatutFilter = 'Tous les statuts';
  showStatutDropdown = false;
  currentPage = 1;
  itemsPerPage = 10;
  pageSizeOptions = PAGE_SIZE_OPTIONS;
  totalPages = 1;
  totalElements = 0;
  loadingList = false;
  /** Export Excel en cours. */
  loadingExport = false;

  /** Loaders graphiques (Produits fréquents, Commandes par jour). */
  // Initialisé à false pour éviter NG0100 côté SSR (AfterViewInit ne doit pas basculer true -> false).
  loadingChartFrequentProducts = false;
  loadingChartDailyOrders = false;
  /** True si le chargement de la liste a échoué (erreur API). */
  listError = false;

  commandes: Commande[] = [];

  constructor(
    private router: Router,
    private logisticsService: LogisticsService,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
    // Évite les appels API pendant le SSR (Node ne peut souvent pas joindre l’URL du backend → status 0 / ENETUNREACH).
    if (!this.isBrowser) {
      return;
    }
    this.loadCommandes();
    this.loadStats();
  }

  private loadStats(): void {
    this.loadingStats = true;
    this.statsError = false;
    this.logisticsService.getEmployeeOrderStats().subscribe({
      next: (stats) => {
        this.metricsData = [
          { title: 'TOTAL', value: String(stats.totalCommandes ?? 0), icon: '/icones/commandefour.svg', subtitle: 'tous statuts sauf annulées' },
          { title: 'EN ATTENTE', value: String(stats.enAttente), icon: '/icones/attente.svg', subtitle: 'À planifier' },
          { title: 'EN RETARD', value: String(stats.enRetard), icon: '/icones/alerte.svg', subtitle: 'date dépassée' },
          { title: 'EN COURS', value: String(stats.enCours), icon: '/icones/temps.svg', subtitle: 'tournées actives' },
          { title: 'VALIDÉES', value: String(stats.validees), icon: '/icones/confirmer.svg', subtitle: 'affectées à une tournée' },
          { title: 'LIVRÉES', value: String(stats.livreesCeMois), icon: '/icones/zigzag.svg', subtitle: 'ce mois' }
        ];
        this.loadingStats = false;
      },
      error: () => {
        this.statsError = true;
        this.loadingStats = false;
        this.metricsData = [];
      }
    });
  }

  ngAfterViewInit(): void {
    if (!this.isBrowser) {
      return;
    }
    this.loadFrequentProductsChart();
    this.loadDailyOrdersChart();
  }

  private loadFrequentProductsChart(): void {
    this.loadingChartFrequentProducts = true;
    this.logisticsService.getTop5ProductsUsage().pipe(finalize(() => (this.loadingChartFrequentProducts = false))).subscribe({
      next: (list) => {
        const labels = (list ?? []).map((x) => x.productName);
        const data = (list ?? []).map((x) => x.usagePercent);
        this.initFrequentProductsChart(labels, data);
      },
      error: () => {
        this.initFrequentProductsChart([], []);
      }
    });
  }

  private initFrequentProductsChart(labels: string[], data: number[]): void {
    const ctx = this.frequentProductsChart?.nativeElement?.getContext('2d');
    if (!ctx) return;

    const gradient = ctx.createLinearGradient(0, 0, 500, 0);
    gradient.addColorStop(0, '#FF6B00');
    gradient.addColorStop(1, '#FF914D');

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: labels.length ? labels : [],
        datasets: [{
          label: 'Utilisation (%)',
          data: data.length ? data : [],
          backgroundColor: gradient,
          borderRadius: 4,
          barThickness: 24,
        }]
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: true,
            position: 'top',
            align: 'start',
            labels: {
              usePointStyle: true,
              pointStyle: 'rect',
              padding: 15,
              font: { size: 11, family: 'Inter, sans-serif' },
              color: '#6B7280',
              boxWidth: 20,
              boxHeight: 12
            }
          },
          tooltip: {
            enabled: true,
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            padding: 10,
            cornerRadius: 8,
            titleFont: { size: 12 },
            bodyFont: { size: 12 },
            callbacks: {
              label: (c) => `Utilisation : ${Number(c.parsed.x).toFixed(1)} %`
            }
          }
        },
        scales: {
          x: {
            beginAtZero: true,
            max: 100,
            ticks: {
              stepSize: 10,
              callback: (value) => value + ' %',
              font: { size: 11 },
              color: '#6B7280'
            },
            grid: { display: true, color: '#F3F4F6', drawBorder: false }
          },
          y: {
            ticks: { font: { size: 11 }, color: '#4B4848' },
            grid: { display: false }
          }
        }
      }
    };

    this.frequentProductsChartInstance = new Chart(ctx, config);
  }

  /** Charge les 7 derniers jours via l’API puis initialise le graphique « Commandes par jour ». */
  private loadDailyOrdersChart(): void {
    // Pas de données par défaut : si l'API échoue, le graphique reste vide.
    this.dailyOrdersChartInstance?.destroy();
    this.dailyOrdersChartInstance = undefined;

    this.loadingChartDailyOrders = true;
    this.logisticsService.getCommandesParJour().pipe(finalize(() => (this.loadingChartDailyOrders = false))).subscribe({
      next: (list) => {
        const labels = (list ?? []).map((x) => x.date);
        const data = (list ?? []).map((x) => x.nbCommandes);
        // Si aucune donnée, on laisse le canvas vide (pas de barres statiques).
        if (!labels.length || !data.length) return;
        this.initDailyOrdersChart(labels, data);
      },
      error: () => {
        // On ne montre rien si l'API est en erreur.
      }
    });
  }

  private initDailyOrdersChart(labels: string[], data: number[]): void {
    const ctx = this.dailyOrdersChart?.nativeElement?.getContext('2d');
    if (!ctx) return;

    // Assure qu'on ne superpose jamais un graphique sur l'ancien.
    this.dailyOrdersChartInstance?.destroy();
    this.dailyOrdersChartInstance = undefined;

    const maxVal = data.length ? Math.max(...data, 1) : 1;
    const suggestedMax = Math.max(10, (Math.floor(maxVal / 5) + 1) * 5);

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: labels.length ? labels : [],
        datasets: [{
          label: 'Commandes',
          data: data.length ? data : [],
          backgroundColor: '#318F3F',
          borderRadius: 4,
          barThickness: 32,
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            display: false
          },
          tooltip: {
            enabled: true,
            backgroundColor: 'rgba(0, 0, 0, 0.8)',
            padding: 10,
            cornerRadius: 8,
            titleFont: { size: 12 },
            bodyFont: { size: 12 }
          }
        },
        scales: {
          x: {
            ticks: { font: { size: 11 }, color: '#6B7280' },
            grid: { display: false }
          },
          y: {
            beginAtZero: true,
            suggestedMax,
            ticks: {
              stepSize: 5,
              font: { size: 11 },
              color: '#6B7280'
            },
            grid: { display: true, color: '#F3F4F6', drawBorder: false }
          }
        }
      }
    };

    this.dailyOrdersChartInstance = new Chart(ctx, config);
  }

  get uniqueStatuts(): string[] {
    return ['Tous les statuts', 'En attente', 'Validée', 'En préparation', 'En cours', 'Livrée', 'Échec', 'Annulée'];
  }

  get filteredCommandes(): Commande[] {
    return this.commandes;
  }

  get paginatedCommandes(): Commande[] {
    return this.commandes;
  }

  get totalPagesCount(): number {
    return this.totalPages;
  }

  toggleStatutDropdown(): void {
    this.showStatutDropdown = !this.showStatutDropdown;
  }

  selectStatut(statut: string): void {
    this.selectedStatutFilter = statut;
    this.showStatutDropdown = false;
    this.currentPage = 1;
    this.loadCommandes();
  }

  previousPage(): void {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadCommandes();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPagesCount) {
      this.currentPage++;
      this.loadCommandes();
    }
  }

  onPageSizeChange(size: number): void {
    this.itemsPerPage = size;
    this.currentPage = 1;
    this.loadCommandes();
  }

  loadCommandes(): void {
    this.loadingList = true;
    const statusParam = this.mapStatutToApi(this.selectedStatutFilter);
    this.logisticsService
      .getEmployeeOrders(
        this.currentPage - 1,
        this.itemsPerPage,
        this.searchTerm?.trim() || undefined,
        statusParam
      )
      .subscribe({
        next: (res) => {
          this.commandes = (res.content || []).map((item) => this.mapApiOrderToCommande(item));
          this.totalElements = res.totalElements ?? 0;
          this.totalPages = res.totalPages ?? 1;
          this.loadingList = false;
          this.listError = false;
        },
        error: () => {
          this.commandes = [];
          this.loadingList = false;
          this.listError = true;
        }
      });
  }

  private mapStatutToApi(statut: string): string | undefined {
    if (!statut || statut === 'Tous les statuts') return undefined;
    const map: Record<string, string> = {
      'En attente': 'EN_ATTENTE',
      'Validée': 'VALIDEE',
      'En préparation': 'EN_PREPARATION',
      'En cours': 'EN_COURS',
      'Livrée': 'LIVREE',
      'Échec': 'ECHEC_LIVRAISON',
      'Annulée': 'ANNULEE'
    };
    return map[statut];
  }

  private mapApiOrderToCommande(item: {
    id: number;
    orderNumber: string;
    employeeName: string;
    orderDate?: string;
    statusDate?: string;
    products: string[];
    deliveryFrequency: string | null;
    status: string;
  }): Commande {
    let statut: Commande['statut'] = 'En attente';
    if (item.status === 'Livrée') statut = 'Livrée';
    else if (item.status === 'Annulée') statut = 'Annulée';
    else if (item.status === 'Échec de livraison' || item.status === 'ECHEC_LIVRAISON') statut = 'Échec';
    else if (item.status === 'En cours de livraison' || item.status === 'En cours') statut = 'En cours';
    else if (item.status === 'En préparation' || item.status === 'EN_PREPARATION') statut = 'En préparation';
    else if (item.status === 'Validée' || item.status === 'VALIDEE') statut = 'Validée';
    else if (item.status === 'En attente' || item.status === 'EN_ATTENTE') statut = 'En attente';
    return {
      id: item.id,
      numero: item.orderNumber,
      salarie: item.employeeName ?? '',
      dateCommande: item.orderDate ?? '',
      dateStatut: item.statusDate ?? undefined,
      produitsList: item.products ?? [],
      frequence: item.deliveryFrequency ?? '—',
      statut,
      reference: item.orderNumber
    };
  }

  /** Comme fournisseur : ouvrir le modal tout de suite avec les infos liste, charger les produits en arrière-plan. */
  viewCommande(numero: string): void {
    const commande = this.commandes.find(c => c.numero === numero);
    if (!commande) return;
    this.selectedCommande = commande;
    this.showDetailModal = true;
    this.selectedOrderDetails = null;
    this.loadingOrderDetails = true;
    this.loadOrderDetailsError = false;
    this.logisticsService.getEmployeeOrderDetails(commande.id).subscribe({
      next: (details) => {
        this.selectedOrderDetails = details;
        this.loadingOrderDetails = false;
        this.loadOrderDetailsError = false;
      },
      error: () => {
        this.loadingOrderDetails = false;
        this.loadOrderDetailsError = true;
      }
    });
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedCommande = null;
    this.selectedOrderDetails = null;
    this.loadOrderDetailsError = false;
    // La liste garde l’ancien statut tant qu’on ne recharge pas ; les détails viennent d’une autre requête (ex. annulation côté salarié).
    if (this.isBrowser) {
      this.loadCommandes();
      this.loadStats();
    }
  }

  /** URL complète pour l'image produit (backend renvoie souvent un chemin relatif). */
  getProductImageUrl(image: string | null | undefined): string {
    if (!image) return '';
    if (image.startsWith('http://') || image.startsWith('https://')) return image;
    const base = environment.imageServerUrl;
    return `${base}/files/${image}`;
  }

  /** Libellé de statut affiché dans le modal (API si chargé, sinon liste). */
  getDetailModalStatusLabel(): string {
    const d = this.selectedOrderDetails;
    if (d?.status) return d.status;
    return this.selectedCommande?.statut ?? '—';
  }

  getDetailModalStatusBadgeClass(): string {
    return this.statusBadgeClassFromLabel(this.getDetailModalStatusLabel());
  }

  getDetailModalStatusDotClass(): string {
    return this.statusDotClassFromLabel(this.getDetailModalStatusLabel());
  }

  /** Encadré « MOTIF » (échec de livraison) : affiché quand le statut API indique un échec. */
  showDeliveryFailureMotifBlock(): boolean {
    if (!this.selectedOrderDetails || this.loadingOrderDetails) {
      return false;
    }
    return this.isDeliveryFailureStatusLabel(this.selectedOrderDetails.status);
  }

  formatStatusChangedAtLongFr(iso: string | null | undefined): string {
    if (!iso) return '';
    const d = new Date(iso);
    if (Number.isNaN(d.getTime())) return '';
    return d.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' });
  }

  /**
   * Rôle pour la phrase « par le livreur … » : première lettre en minuscule (libellé API ex. « Livreur »).
   */
  formatRoleForFailureSentence(role: string | null | undefined): string {
    const t = role?.trim();
    if (!t) return '';
    return t.charAt(0).toLocaleLowerCase('fr-FR') + t.slice(1);
  }

  private isDeliveryFailureStatusLabel(status: string | undefined | null): boolean {
    if (!status) return false;
    const s = status.toLowerCase();
    return (s.includes('échec') || s.includes('echec')) && s.includes('livraison');
  }

  private statusBadgeClassFromLabel(label: string): string {
    const L = (label || '').toLowerCase();
    if (L.includes('échec') || L.includes('echec')) return 'bg-[#DC26260F] text-[#DC2626]';
    if (L.includes('livrée') || L.includes('livree')) return 'bg-[#0A97480F] text-[#0A9748]';
    if (L.includes('préparation') || L.includes('preparation')) return 'bg-[#0369A10F] text-[#0369A1]';
    if (L.includes('valid')) return 'bg-[#2B36740F] text-[#2B3674]';
    if (L.includes('cours') || L.includes('livraison')) return 'bg-[#EAB3080F] text-[#EAB308]';
    if (L.includes('attente')) return 'bg-[#F2F2F2] text-[#2C3E50]';
    if (L.includes('annul')) return 'bg-[#FF09090F] text-[#FF0909]';
    return 'bg-gray-100 text-gray-600';
  }

  private statusDotClassFromLabel(label: string): string {
    const L = (label || '').toLowerCase();
    if (L.includes('échec') || L.includes('echec')) return 'bg-[#DC2626]';
    if (L.includes('livrée') || L.includes('livree')) return 'bg-[#0A9748]';
    if (L.includes('préparation') || L.includes('preparation')) return 'bg-[#0369A1]';
    if (L.includes('valid')) return 'bg-[#2B3674]';
    if (L.includes('cours') || (L.includes('livraison') && !L.includes('échec') && !L.includes('echec'))) return 'bg-[#EAB308]';
    if (L.includes('attente')) return 'bg-[#2C3E50]';
    if (L.includes('annul')) return 'bg-[#FF0909]';
    return 'bg-gray-400';
  }

  exportData(): void {
    if (!this.isBrowser || this.loadingExport) return;
    this.loadingExport = true;
    const statusParam = this.mapStatutToApi(this.selectedStatutFilter);
    this.logisticsService
      .exportEmployeeOrders(this.searchTerm?.trim() || undefined, statusParam)
      .pipe(finalize(() => { this.loadingExport = false; }))
      .subscribe({
        next: (blob) => this.downloadExportBlob(blob, 'commandes_salaries'),
        error: () => { /* blob ou erreur réseau */ }
      });
  }

  private downloadExportBlob(blob: Blob, baseName: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${baseName}_${new Date().toISOString().slice(0, 10)}.xlsx`;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  getStatusClass(statut: string): string {
    switch (statut) {
      case 'En cours':
        return 'bg-[#EAB3080F] text-[#EAB308]';
      case 'Livrée':
        return 'bg-[#0A97480F] text-[#0A9748]';
      case 'Validée':
        return 'bg-[#2B36740F] text-[#2B3674]';
      case 'En préparation':
        return 'bg-[#0369A10F] text-[#0369A1]';
      case 'En attente':
        return 'bg-[#F2F2F2] text-[#2C3E50]';
      case 'Échec':
        return 'bg-[#DC26260F] text-[#DC2626]';
      case 'Annulée':
        return 'bg-[#FF09090F] text-[#FF0909]';
      default:
        return 'bg-gray-100 text-gray-600';
    }
  }

  getStatusDotClass(statut: string): string {
    switch (statut) {
      case 'En cours':
        return 'bg-[#EAB308]';
      case 'Livrée':
        return 'bg-[#0A9748]';
      case 'Validée':
        return 'bg-[#2B3674]';
      case 'En préparation':
        return 'bg-[#0369A1]';
      case 'En attente':
        return 'bg-[#2C3E50]';
      case 'Échec':
        return 'bg-[#DC2626]';
      case 'Annulée':
        return 'bg-[#FF0909]';
      default:
        return 'bg-gray-400';
    }
  }

  private searchDebounce: ReturnType<typeof setTimeout> | null = null;

  onSearchChange(): void {
    if (this.searchDebounce) clearTimeout(this.searchDebounce);
    this.searchDebounce = setTimeout(() => {
      this.currentPage = 1;
      this.loadCommandes();
      this.searchDebounce = null;
    }, 400);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (this.showStatutDropdown) {
      this.showStatutDropdown = false;
    }
  }

}
