import { Component, HostListener, ViewChild, ElementRef, AfterViewInit, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { ProductService, Product } from '../../../shared/services/product.service';
import { LogisticsService, EmployeeOrderDetails } from '../../../shared/services/logistics.service';
import { PAGE_SIZE_OPTIONS } from '../../../shared/constants/pagination';
import { environment } from '../../../../environments/environment';
import { isPlatformBrowser } from '@angular/common';
import Swal from 'sweetalert2';

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
  statut: 'En cours' | 'Livrée' | 'En attente' | 'Validée' | 'Annulée' | 'Échec';
  reference: string;
  note?: string;
  produitsDetails?: { productId: string; quantity: number }[];
  /** Raison de l'échec de livraison (affichée dans la liste pour le RL). */
  failureReason?: string | null;
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
  allProducts: Product[] = [];
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

  commandes: Commande[] = [];

  constructor(
    private router: Router,
    private productService: ProductService,
    private logisticsService: LogisticsService,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
    this.loadAllProducts();
    this.loadCommandes();
    this.loadStats();
  }

  private loadStats(): void {
    this.loadingStats = true;
    this.statsError = false;
    this.logisticsService.getEmployeeOrderStats().subscribe({
      next: (stats) => {
        this.metricsData = [
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
    if (!this.isBrowser) return;
    this.loadFrequentProductsChart();
    this.loadDailyOrdersChart();
  }

  private loadFrequentProductsChart(): void {
    this.logisticsService.getTop5ProductsUsage().subscribe({
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

    this.logisticsService.getCommandesParJour().subscribe({
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
    return ['Tous les statuts', 'En attente', 'Validée', 'En cours', 'Livrée', 'Échec', 'Annulée'];
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
        },
        error: () => {
          this.commandes = [];
          this.loadingList = false;
        }
      });
  }

  private mapStatutToApi(statut: string): string | undefined {
    if (!statut || statut === 'Tous les statuts') return undefined;
    const map: Record<string, string> = {
      'En attente': 'EN_ATTENTE',
      'Validée': 'VALIDEE',
      'En cours': 'EN_COURS_DE_LIVRAISON',
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
    failureReason?: string | null;
  }): Commande {
    let statut: Commande['statut'] = 'En attente';
    if (item.status === 'Livrée') statut = 'Livrée';
    else if (item.status === 'Annulée') statut = 'Annulée';
    else if (item.status === 'Échec de livraison' || item.status === 'ECHEC_LIVRAISON') statut = 'Échec';
    else if (item.status === 'En cours de livraison' || item.status === 'En cours') statut = 'En cours';
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
      reference: item.orderNumber,
      failureReason: item.failureReason ?? null
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
  }

  /** Replanifier une commande en échec : EN_ATTENTE, notifie le salarié. */
  replanOrder(commande: Commande): void {
    this.logisticsService.replanOrder(commande.id).subscribe({
      next: () => {
        Swal.fire({ title: 'Succès', text: 'Commande replanifiée. Elle réapparaît dans En attente.', icon: 'success', timer: 2500, showConfirmButton: false });
        this.loadCommandes();
        this.loadStats();
      },
      error: (err) => {
        const msg = err?.error ?? err?.message ?? 'Impossible de replanifier la commande';
        Swal.fire({ title: 'Erreur', text: String(msg), icon: 'error', confirmButtonText: 'OK' });
      }
    });
  }

  /** Annuler définitivement une commande après échec (avec confirmation). */
  cancelOrderAfterFailure(commande: Commande): void {
    Swal.fire({
      title: 'Annuler ' + commande.numero + ' ?',
      html: 'Cette action est irréversible. La commande de <strong>' + commande.salarie + '</strong> sera définitivement annulée.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#DC2626',
      cancelButtonText: 'Retour',
      confirmButtonText: 'Confirmer l\'annulation'
    }).then((result) => {
      if (result.isConfirmed) {
        this.logisticsService.cancelOrderAfterFailure(commande.id).subscribe({
          next: () => {
            Swal.fire({ title: 'Succès', text: 'Commande annulée. Stock réintégré.', icon: 'success', timer: 2500, showConfirmButton: false });
            this.loadCommandes();
            this.loadStats();
          },
          error: (err) => {
            const msg = err?.error ?? err?.message ?? 'Impossible d\'annuler la commande';
            Swal.fire({ title: 'Erreur', text: String(msg), icon: 'error', confirmButtonText: 'OK' });
          }
        });
      }
    });
  }

  /** URL complète pour l'image produit (backend renvoie souvent un chemin relatif). */
  getProductImageUrl(image: string | null | undefined): string {
    if (!image) return '';
    if (image.startsWith('http://') || image.startsWith('https://')) return image;
    const base = environment.imageServerUrl;
    return `${base}/files/${image}`;
  }

  exportData(): void {
    console.log('Export des données');
  }

  getStatusClass(statut: string): string {
    switch (statut) {
      case 'En cours':
        return 'bg-[#EAB3080F] text-[#EAB308]';
      case 'Livrée':
        return 'bg-[#0A97480F] text-[#0A9748]';
      case 'Validée':
        return 'bg-[#2B36740F] text-[#2B3674]';
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

  private loadAllProducts(): void {
    this.productService.getProducts(0, 1000).subscribe({
      next: (response) => {
        const products = response?.content ?? [];
        this.allProducts = products.map((item: any) => this.mapApiProductToFrontend(item));
      },
      error: (error) => {
        console.error('Erreur lors du chargement des produits:', error);
      }
    });
  }

  private mapApiProductToFrontend(item: any): Product {
    return {
      id: item.id?.toString() ?? '',
      name: item.name ?? '',
      reference: item.productCode ?? '',
      category: item.categoryName ?? '',
      price: this.formatPrice(item.price),
      stock: item.currentStock ?? 0,
      updatedAt: this.formatDate(item.updatedAt),
      status: this.normalizeStatus(item.status),
      icon: this.buildImageUrl(item.image),
      description: item.description
    };
  }

  private normalizeStatus(status: string | boolean | undefined): 'Actif' | 'Inactif' {
    if (status === true || status === 'ACTIF' || status === 'ACTIVE' || status === 'Actif') return 'Actif';
    if (status === false || status === 'INACTIF' || status === 'INACTIVE' || status === 'Inactif') return 'Inactif';
    return 'Inactif';
  }

  private formatPrice(price: any): string {
    if (price === null || price === undefined || price === '') return '';
    const value = typeof price === 'number' ? price : Number(price);
    if (Number.isNaN(value)) return `${price}`;
    return `${value.toLocaleString('fr-FR')} F`;
  }

  private formatDate(updatedAt: string | undefined): string {
    if (!updatedAt) return '';
    const datePart = updatedAt.split(' ')[0];
    return datePart ? datePart.replace(/-/g, '/') : updatedAt;
  }

  private buildImageUrl(image: string | undefined): string {
    if (!image) return '/icones/default-product.svg';
    if (image.startsWith('file://')) return '/icones/default-product.svg';
    if (image.startsWith('http') || image.startsWith('/')) return image;
    const base = environment.imageServerUrl;
    return `${base}/files/${image}`;
  }
}
