import { Component, OnInit, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { Subscription, finalize } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { NgChartsModule } from 'ng2-charts';
import { ChartConfiguration } from 'chart.js';
import Swal from 'sweetalert2';
import { ProductService } from '../../../shared/services/product.service';
import { PAGE_SIZE_OPTIONS } from '../../../shared/constants/pagination';

interface MetricCard {
  title: string;
  value: string;
  icon: string;
}

interface Product {
  id: string;
  name: string;
  reference: string;
  category: string;
  price: string;
  stock: number;
  updatedAt: string;
  status: 'Actif' | 'Inactif';
  icon: string;
  description?: string;
  /** "En stock" ou "Rupture" selon le stock (aligné sur le DTO détail backend). */
  currentStockStatus?: string;
}

@Component({
  selector: 'app-catalogue',
  standalone: true,
  imports: [MainLayoutComponent, HeaderComponent, CommonModule, FormsModule, NgChartsModule],
  templateUrl: './catalogue.component.html',
  styles: ``
})
export class CatalogueComponent implements OnInit, OnDestroy {
  isBrowser = false;
  private routerSub?: Subscription;

  searchText = '';
  selectedCategory = 'Toutes les catégories';
  selectedCategoryId: number | null = null;
  selectedStatus = 'Tous les status';
  showCategoryDropdown = false;
  showStatusDropdown = false;
  currentPage = 1;
  itemsPerPage = 6;
  pageSizeOptions = PAGE_SIZE_OPTIONS;
  totalPages = 1;
  totalElements = 0;
  showProductModal = false;
  selectedProduct: Product | null = null;
  categories: { id: number; name: string }[] = [];

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private productService: ProductService,
    @Inject(PLATFORM_ID) private platformId: object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  // Bar Chart Configuration - Top 5 Produits commandés (utilisation en %)
  public barChartData: ChartConfiguration<'bar'>['data'] = {
    labels: [],
    datasets: [
      {
        data: [],
        backgroundColor: (ctx) => {
          const { chart } = ctx;
          const { ctx: c, chartArea } = chart as any;
          if (!chartArea) {
            return '#FF6B00';
          }
          const gradient = c.createLinearGradient(chartArea.left, 0, chartArea.right, 0);
          gradient.addColorStop(0, '#FF6B00');
          gradient.addColorStop(1, '#FF914D');
          return gradient;
        },
        barThickness: 30,
        hoverBackgroundColor: '#FF914D'
      }
    ]
  };

  public barChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    indexAxis: 'y',
    plugins: {
      legend: {
        display: true,
        position: 'top',
        align: 'start',
        labels: {
          boxWidth: 40,
          boxHeight: 12,
          padding: 15,
          font: { size: 12 },
          generateLabels: () => [
            { text: 'Utilisation (%)', fillStyle: '#FF6B00', strokeStyle: '#FF6B00', lineWidth: 0 }
          ]
        }
      },
      tooltip: {
        enabled: true,
        callbacks: {
          label: (context) => `Utilisation : ${Number(context.parsed.x).toFixed(1)} %`
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
          font: { size: 12 }
        },
        grid: { display: true, color: '#F2F5F9' }
      },
      y: {
        ticks: { font: { size: 12 } },
        grid: { display: true, color: '#F2F5F9' }
      }
    }
  };

  // Doughnut Chart Configuration - Répartition des statuts
  public doughnutChartData: ChartConfiguration<'doughnut'>['data'] = {
    labels: ['Actifs', 'Inactifs'],
    datasets: [
      {
        data: [83, 17],
        backgroundColor: ['#22C55F', '#FFD3D3'],
        hoverBackgroundColor: ['#22C55E', '#eeb8b8ff'],
        borderWidth: 2,
        hoverBorderColor: '#FFFFFF'
      }
    ]
  };

  public doughnutChartOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '60%',
    plugins: {
      legend: {
        display: true,
        position: 'right',
        labels: {
          usePointStyle: true,
          pointStyle: 'circle',
          boxWidth: 6,
          boxHeight: 6,
          padding: 20,
          font: {
            size: 12
          },
          generateLabels: (chart) => {
            const data = chart.data;
            if (data.labels && data.datasets.length) {
              return data.labels.map((label, i) => ({
                text: label as string,
                fillStyle: (data.datasets[0].backgroundColor as string[])[i],
                strokeStyle: (data.datasets[0].backgroundColor as string[])[i],
                lineWidth: 0,
                hidden: false,
                index: i
              }));
            }
            return [];
          }
        }
      },
      tooltip: {
        enabled: true,
        callbacks: {
          label: (context) => {
            return `${context.label}: ${Number(context.parsed).toFixed(1)}%`;
          }
        }
      }
    }
  };

  ngOnInit() {
    this.loadCategories();
    this.loadProducts();
    this.loadProductStats();
    this.loadTop5ProductUsage();

    // Au premier chargement avec ?refresh=1 (retour après ajout/modif produit), recharger puis retirer le param
    if (this.route.snapshot.queryParamMap.get('refresh') === '1') {
      this.loadProducts();
      this.loadProductStats();
      this.loadTop5ProductUsage();
      this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
    }

    // Si le routeur réutilise le composant, NavigationEnd permet de recharger quand on revient avec ?refresh=1
    this.routerSub = this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd)
    ).subscribe(() => {
      if (this.route.snapshot.queryParamMap.get('refresh') === '1') {
        this.loadProducts();
        this.loadProductStats();
        this.loadTop5ProductUsage();
        this.router.navigate([], { relativeTo: this.route, queryParams: {}, replaceUrl: true });
      }
    });
  }

  ngOnDestroy() {
    this.routerSub?.unsubscribe();
  }

  /** True pendant l’appel API du graphique « Top 5 Produits commandés ». */
  loadingChartTop5 = false;

  /** Charge le top 5 des produits les plus commandés (en % d'utilisation) pour le graphique. */
  loadTop5ProductUsage(): void {
    this.loadingChartTop5 = true;
    this.productService.getTop5ProductsUsage().pipe(
      finalize(() => { this.loadingChartTop5 = false; })
    ).subscribe({
      next: (list) => {
        const labels = list?.length
          ? list.map((x: { productName: string }) => x.productName)
          : [];
        const data = list?.length
          ? list.map((x: { usagePercent: number }) => x.usagePercent)
          : [];
        this.barChartData = {
          ...this.barChartData,
          labels,
          datasets: [{ ...this.barChartData.datasets[0], data }]
        };
      },
      error: () => {
        this.barChartData = {
          ...this.barChartData,
          labels: [],
          datasets: [{ ...this.barChartData.datasets[0], data: [] }]
        };
      }
    });
  }

  metricsData: MetricCard[] = [];

  products: Product[] = [];
  loadingList = false;
  loadingExport = false;

  updateMetrics(stats?: { totalProducts: number; activeProducts: number; inactiveProducts: number }) {
    const total = stats?.totalProducts ?? this.products.length;
    const actifs = stats?.activeProducts ?? this.products.filter(p => p.status === 'Actif').length;
    const inactifs = stats?.inactiveProducts ?? this.products.filter(p => p.status === 'Inactif').length;

    this.metricsData = [
      {
        title: 'Total',
        value: String(total).padStart(2, '0'),
        icon: '/icones/commandefour.svg'
      },
      {
        title: 'Actifs',
        value: String(actifs).padStart(2, '0'),
        icon: '/icones/livraisonavenir.svg'
      },
      {
        title: 'Inactifs',
        value: String(inactifs).padStart(2, '0'),
        icon: '/icones/temps.svg'
      }
    ];
  }

  get uniqueCategories(): { id: number | null; name: string }[] {
    return [{ id: null, name: 'Toutes les catégories' }, ...this.categories];
  }

  get uniqueStatuses(): string[] {
    return ['Tous les status', 'Actif', 'Inactif'];
  }

  get filteredProducts(): Product[] {
    // Filtre local par catégorie (le backend ne fournit pas d'endpoint catégories)
    if (this.selectedCategory === 'Toutes les catégories') {
      return this.products;
    }
    return this.products.filter(product => product.category === this.selectedCategory);
  }

  toggleCategoryDropdown() {
    this.showCategoryDropdown = !this.showCategoryDropdown;
    this.showStatusDropdown = false;
  }

  toggleStatusDropdown() {
    this.showStatusDropdown = !this.showStatusDropdown;
    this.showCategoryDropdown = false;
  }

  selectCategory(category: { id: number | null; name: string }) {
    this.selectedCategory = category.name;
    this.selectedCategoryId = category.id;
    this.showCategoryDropdown = false;
    this.currentPage = 1;
    this.loadProducts();
  }

  selectStatus(status: string) {
    this.selectedStatus = status;
    this.showStatusDropdown = false;
    this.currentPage = 1;
    this.loadProducts();
  }

  getStatusClass(status: string): string {
    return status === 'Actif'
      ? 'bg-[#0A97480F] text-[#0A9748]'
      : 'bg-red-50 text-[#FF0909]';
  }

  getStatusDotClass(status: string): string {
    return status === 'Actif' ? 'bg-[#0A9748]' : 'bg-[#FF0909]';
  }

  nouveauProduit() {
    this.router.navigate(['/admin/add-produit']);
  }

  viewProduct(product: Product) {
    this.productService.getProductDetails(product.id).subscribe({
      next: (details) => {
        this.selectedProduct = this.mapProductDetailsToProduct(product, details);
        this.showProductModal = true;
      },
      error: () => {
        this.selectedProduct = product;
        this.showProductModal = true;
      }
    });
  }

  closeProductModal() {
    this.showProductModal = false;
    this.selectedProduct = null;
  }

  toggleModalProductStatus() {
    if (this.selectedProduct) {
      this.toggleProductStatus(this.selectedProduct);
    }
  }

  editProduct(product: Product) {
    this.router.navigate(['/admin/add-produit'], {
      queryParams: { id: product.id },
      state: { product }
    });
  }

  toggleProductStatus(product: Product) {
    const isActivating = product.status === 'Inactif';
    const titleText = isActivating ? 'Activer ce produit' : 'Désactiver ce produit ?';
    const descriptionText = isActivating ? 'Il sera visible et commandable.' : 'Il ne sera plus affiché ni commandable.';
    const confirmButtonText = isActivating ? 'Activer' : 'Oui';
    const confirmButtonClass = isActivating
      ? 'bg-[#16A34A] hover:bg-[#16A34A] text-white px-8 py-3 rounded-lg font-medium text-base shadow-none border-none'
      : 'bg-[#EF4444] hover:bg-[#DC2626] text-white px-8 py-3 rounded-lg font-medium text-base shadow-none border-none';

    Swal.fire({
      title: titleText,
      text: descriptionText,
      iconHtml: `<img src="/icones/alerte.svg" alt="alert" style=" margin: 0 auto;" />`,
      showCancelButton: true,
      confirmButtonText: confirmButtonText,
      cancelButtonText: 'Annuler',
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-3xl p-6',
        title: 'text-2xl font-medium text-gray-900',
        htmlContainer: 'text-lg text-gray-600',
        confirmButton: confirmButtonClass,
        cancelButton: 'bg-[#F3F4F6] hover:bg-gray-200 text-gray-700 px-8 py-3 rounded-lg font-medium text-base shadow-none border-none',
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
        const nextStatus = product.status === 'Actif' ? false : true;
        this.productService.updateProductStatus(product.id, nextStatus).subscribe({
          next: () => {
            const newLabel: 'Actif' | 'Inactif' = nextStatus ? 'Actif' : 'Inactif';

            // Mettre à jour l'objet courant (modale) + la ligne liste si elle existe,
            // puis recharger la liste (important si un filtre Actif/Inactif est appliqué).
            product.status = newLabel;
            if (this.selectedProduct?.id === product.id) {
              this.selectedProduct.status = newLabel;
            }
            const row = this.products.find((p) => p.id === product.id);
            if (row) {
              row.status = newLabel;
            }

            this.loadProductStats();
            this.loadProducts();
            this.showToggleSuccessMessage(newLabel);
          },
          error: (error) => {
            console.error('Erreur lors de la mise à jour du statut:', error);
          }
        });
      }
    });
  }

  showToggleSuccessMessage(newStatus: 'Actif' | 'Inactif') {
    Swal.fire({
      title: newStatus === 'Inactif' ? 'Le produit a été désactivé' : 'Le produit a été activé',
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

  exportData(): void {
    if (!this.isBrowser || this.loadingExport) return;
    this.loadingExport = true;
    const statusFilter = this.getStatusFilter();
    this.productService
      .exportProducts(this.searchText, this.selectedCategoryId ?? undefined, statusFilter)
      .pipe(finalize(() => { this.loadingExport = false; }))
      .subscribe({
        next: (blob) => {
          const url = window.URL.createObjectURL(blob);
          const a = document.createElement('a');
          a.href = url;
          a.download = `produits_${new Date().toISOString().slice(0, 10)}.xlsx`;
          a.click();
          window.URL.revokeObjectURL(url);
        },
        error: (error) => {
          console.error('Erreur export:', error);
        }
      });
  }

  previousPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.loadProducts();
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.loadProducts();
    }
  }

  onSearch() {
    this.currentPage = 1;
    this.loadProducts();
  }

  onPageSizeChange(size: number) {
    this.itemsPerPage = size;
    this.currentPage = 1;
    this.loadProducts();
  }

  private loadProducts() {
    this.loadingList = true;
    const statusFilter = this.getStatusFilter();

    this.productService
      .getProducts(this.currentPage - 1, this.itemsPerPage, this.searchText, this.selectedCategoryId ?? undefined, statusFilter)
      .subscribe({
        next: (response) => {
          const products = response?.content ?? [];
          this.products = products.map((item: any) => this.mapApiProductToFrontend(item));
          this.totalElements = response?.totalElements ?? this.products.length;
          this.totalPages = Math.max(1, response?.totalPages ?? 1);
          this.loadingList = false;
        },
        error: (error) => {
          console.error('Erreur lors du chargement des produits:', error);
          this.loadingList = false;
        }
      });
  }

  private loadProductStats() {
    this.productService.getProductStats().subscribe({
      next: (stats) => {
        this.updateMetrics(stats);
        const total = stats?.totalProducts ?? 0;
        const actifs = stats?.activeProducts ?? 0;
        const inactifs = stats?.inactiveProducts ?? 0;
        const pctActifs = total > 0 ? Math.round((actifs / total) * 1000) / 10 : 0;
        const pctInactifs = total > 0 ? Math.round((inactifs / total) * 1000) / 10 : 0;
        this.doughnutChartData = {
          ...this.doughnutChartData,
          datasets: [{
            ...this.doughnutChartData.datasets[0],
            data: [pctActifs, pctInactifs]
          }]
        };
      },
      error: (error) => {
        console.error('Erreur lors du chargement des statistiques:', error);
      }
    });
  }

  private loadCategories() {
    this.productService.getCategories().subscribe({
      next: (categories) => {
        this.categories = Array.isArray(categories) ? categories : [];
      },
      error: (error) => {
        console.error('Erreur lors du chargement des catégories:', error);
      }
    });
  }

  private getStatusFilter(): boolean | undefined {
    if (this.selectedStatus === 'Actif') return true;
    if (this.selectedStatus === 'Inactif') return false;
    return undefined;
  }

  private mapApiProductToFrontend(item: any): Product {
    const stock = item.currentStock ?? 0;
    return {
      id: item.id?.toString() ?? '',
      name: item.name ?? '',
      reference: item.productCode ?? '',
      category: item.categoryName ?? '',
      price: this.formatPrice(item.price),
      stock,
      updatedAt: this.formatDate(item.updatedAt),
      status: this.normalizeStatus(item.status),
      icon: this.buildImageUrl(item.image),
      description: item.description,
      currentStockStatus: stock > 0 ? 'En stock' : 'Rupture'
    };
  }

  private mapProductDetailsToProduct(base: Product, details: any): Product {
    const stock = details?.currentStock ?? base.stock ?? 0;
    const currentStockStatus = details?.currentStockStatus ?? (stock > 0 ? 'En stock' : 'Rupture');
    return {
      ...base,
      name: details?.name ?? base.name,
      reference: details?.productCode ?? base.reference,
      category: details?.categoryName ?? base.category,
      price: this.formatPrice(details?.price) || base.price,
      stock,
      status: details?.status === true ? 'Actif' : details?.status === false ? 'Inactif' : base.status,
      icon: this.buildImageUrl(details?.image) || base.icon,
      description: details?.description ?? base.description,
      currentStockStatus
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
    const datePart = updatedAt.split(' ')[0]; // dd-MM-yyyy
    return datePart ? datePart.replace(/-/g, '/') : updatedAt;
  }

  private buildImageUrl(image: string | undefined): string {
    if (!image) return '/icones/default-product.svg';
    if (image.startsWith('file://')) return '/icones/default-product.svg'; // chemin local → pas servable par l’API
    if (image.startsWith('http') || image.startsWith('/')) return image;
    const base = environment.imageServerUrl;
    return `${base}/files/${image}`;
  }
}
