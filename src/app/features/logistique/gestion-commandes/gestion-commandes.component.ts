import { Component, HostListener, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { ProductService, Product } from '../../../shared/services/product.service';
import { LogisticsService } from '../../../shared/services/logistics.service';
import { environment } from '../../../../environments/environment';

Chart.register(...registerables);

interface MetricCard {
  title: string;
  value: string;
  icon: string;
  subtitle?: string;
  description?: string;
}

interface Commande {
  numero: string;
  salarie: string;
  dateValidation: string;
  produits: string;
  frequence: string;
  statut: 'En cours' | 'Livrée' | 'En attente' | 'Annulée';
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

  showDetailModal = false;
  selectedCommande: Commande | null = null;
  allProducts: Product[] = [];

  metricsData: MetricCard[] = [
    {
      title: 'Total commandes',
      value: '43',
      icon: '/icones/commandefour.svg',
      subtitle: '24 commandes cette semaine'
    },
    {
      title: 'Temps moyen de préparation',
      value: '18 min',
      icon: '/icones/minuteur.svg',
      description: '3 % vs le mois dernier'
    },
    {
      title: 'Taux de satisfaction',
      value: '94%',
      icon: '/icones/zigzag.svg',
      description: '3 % vs le mois dernier'
    }
  ];

  searchTerm = '';
  selectedStatutFilter = 'Tous les statuts';
  showStatutDropdown = false;
  currentPage = 1;
  itemsPerPage = 10;
  totalPages = 1;
  totalElements = 0;
  loadingList = false;

  commandes: Commande[] = [];

  constructor(
    private router: Router,
    private productService: ProductService,
    private logisticsService: LogisticsService
  ) {
    this.loadAllProducts();
    this.loadCommandes();
  }

  ngAfterViewInit(): void {
    this.initFrequentProductsChart();
    this.initDailyOrdersChart();
  }

  private initFrequentProductsChart(): void {
    const ctx = this.frequentProductsChart.nativeElement.getContext('2d');
    if (!ctx) return;

    const gradient = ctx.createLinearGradient(0, 0, 500, 0);
    gradient.addColorStop(0, '#FF6B00');
    gradient.addColorStop(1, '#FF914D');

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: ['Riz parfumé 25kg', 'Lait 1L', 'Huile', 'Savon'],
        datasets: [{
          label: 'Utilisation(%)',
          data: [50, 40, 32, 25],
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
              font: {
                size: 11,
                family: 'Inter, sans-serif'
              },
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
            titleFont: {
              size: 12
            },
            bodyFont: {
              size: 12
            }
          }
        },
        scales: {
          x: {
            beginAtZero: true,
            max: 50,
            ticks: {
              stepSize: 5,
              font: {
                size: 11
              },
              color: '#6B7280'
            },
            grid: {
              display: true,
              color: '#F3F4F6',
              drawBorder: false
            }
          },
          y: {
            ticks: {
              font: {
                size: 11
              },
              color: '#4B4848'
            },
            grid: {
              display: false
            }
          }
        }
      }
    };

    this.frequentProductsChartInstance = new Chart(ctx, config);
  }

  private initDailyOrdersChart(): void {
    const ctx = this.dailyOrdersChart.nativeElement.getContext('2d');
    if (!ctx) return;

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: ['Lun.', 'Mar.', 'Mer.', 'Jeu.', 'Ven.', 'Sam.', 'Dim.'],
        datasets: [{
          label: 'Commandes',
          data: [13, 21, 17, 19, 25, 12, 7],
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
            titleFont: {
              size: 12
            },
            bodyFont: {
              size: 12
            }
          }
        },
        scales: {
          x: {
            ticks: {
              font: {
                size: 11
              },
              color: '#6B7280'
            },
            grid: {
              display: false
            }
          },
          y: {
            beginAtZero: true,
            max: 25,
            ticks: {
              stepSize: 5,
              font: {
                size: 11
              },
              color: '#6B7280'
            },
            grid: {
              display: true,
              color: '#F3F4F6',
              drawBorder: false
            }
          }
        }
      }
    };

    this.dailyOrdersChartInstance = new Chart(ctx, config);
  }

  get uniqueStatuts(): string[] {
    return ['Tous les statuts', 'En attente', 'Validée', 'En cours', 'Livrée', 'Annulée'];
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
      'Annulée': 'ANNULEE'
    };
    return map[statut];
  }

  private mapApiOrderToCommande(item: {
    id: number;
    orderNumber: string;
    employeeName: string;
    validationDate: string;
    products: string[];
    deliveryFrequency: string | null;
    status: string;
  }): Commande {
    const produitsStr = (item.products || []).join('    ');
    let statut: Commande['statut'] = 'En attente';
    if (item.status === 'Livrée') statut = 'Livrée';
    else if (item.status === 'Annulée') statut = 'Annulée';
    else if (item.status === 'En cours de livraison' || item.status === 'En cours') statut = 'En cours';
    else if (item.status === 'En attente' || item.status === 'Validée') statut = 'En attente';
    return {
      numero: item.orderNumber,
      salarie: item.employeeName ?? '',
      dateValidation: item.validationDate ?? '',
      produits: produitsStr,
      frequence: item.deliveryFrequency ?? '—',
      statut,
      reference: item.orderNumber
    };
  }

  viewCommande(numero: string): void {
    const commande = this.commandes.find(c => c.numero === numero);
    if (commande) {
      this.selectedCommande = commande;
      this.showDetailModal = true;
    }
  }

  closeDetailModal(): void {
    this.showDetailModal = false;
    this.selectedCommande = null;
  }

  getCommandeProducts(): Product[] {
    if (!this.selectedCommande?.produitsDetails) return [];
    return this.allProducts.filter(p =>
      this.selectedCommande!.produitsDetails!.some(pd => pd.productId === p.id)
    );
  }

  getProductQuantity(productId: string): number {
    if (!this.selectedCommande?.produitsDetails) return 0;
    const detail = this.selectedCommande.produitsDetails.find(pd => pd.productId === productId);
    return detail?.quantity || 0;
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
      case 'En attente':
        return 'bg-[#F2F2F2] text-[#2C3E50]';
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
      case 'En attente':
        return 'bg-[#2C3E50]';
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
    if (image.startsWith('http') || image.startsWith('/')) return image;
    return `${environment.apiUrl}/files/${image}`;
  }
}
