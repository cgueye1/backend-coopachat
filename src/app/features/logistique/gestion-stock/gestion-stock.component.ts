import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import Swal from 'sweetalert2';
import { LogisticsService } from '../../../shared/services/logistics.service';
import { ProductService } from '../../../shared/services/product.service';
import { environment } from '../../../../environments/environment';

interface MetricCard {
  title: string;
  value: string;
  subtitle: string;
  icon: string; // Placeholder for icon path or name
  iconBg?: string;
}

interface StockItem {
  id: string;
  name: string;
  reference: string;
  category: string;
  stock: number;
  minThreshold: number;
  status: 'Suffisant' | 'Sous seuil' | 'Rupture';
  image: string;
}

interface SupplierOrder {
  id: string;
  productName: string;
  productReference: string;
  supplier: string;
  quantity: number;
  eta: string;
  status: 'Ouverte' | 'Receptionée' | 'Annulée';
  productId: string;
}


@Component({
  selector: 'app-gestion-stock',
  standalone: true,
  imports: [MainLayoutComponent, CommonModule, FormsModule],
  templateUrl: './gestion-stock.component.html',
  styles: ``
})
export class GestionStockComponent implements OnInit {
  constructor(
    private logisticsService: LogisticsService,
    private productService: ProductService
  ) { }

  ngOnInit(): void {
    this.loadCategories();
    this.loadStockStats();
    this.loadStockList();
    this.loadStockAlerts();
    this.loadSuppliers();
    this.loadAllProducts();
  }
  activeTab = 'suivi';
  searchText = '';
  selectedCategory = 'Toutes les catégories';
  selectedStatus = 'Tous les statuts';
  showCategoryDropdown = false;
  showStatusDropdown = false;

  selectedSupplier = 'Tous les fournisseurs';
  selectedOrderStatus = 'Tous les statuts';
  showSupplierDropdown = false;
  showOrderStatusDropdown = false;

  categories: { id: number; name: string }[] = [];

  get uniqueCategories(): string[] {
    const categories = new Set([
      ...this.stockItems.map(item => item.category),
      ...this.alertItems.map(item => item.category)
    ]);
    return ['Toutes les catégories', ...Array.from(categories)];
  }

  get uniqueSuppliers(): string[] {
    const suppliers = new Set(this.supplierOrders.map(order => order.supplier));
    return ['Tous les fournisseurs', ...Array.from(suppliers)];
  }

  get uniqueOrderStatuses(): string[] {
    const statuses = new Set(this.supplierOrders.map(order => order.status));
    return ['Tous les statuts', ...Array.from(statuses)];
  }

  get filteredAlertItems(): StockItem[] {
    return this.alertItems.filter(item => {
      const isAlert = item.status === 'Sous seuil' || item.status === 'Rupture';
      const matchesSearch =
        item.name.toLowerCase().includes(this.searchText.toLowerCase()) ||
        item.reference.toLowerCase().includes(this.searchText.toLowerCase());

      const matchesCategory = this.selectedCategory === 'Toutes les catégories' ||
        item.category === this.selectedCategory;

      return isAlert && matchesSearch && matchesCategory;
    });
  }

  get filteredSupplierOrders(): SupplierOrder[] {
    return this.supplierOrders.filter(order => {
      const matchesSearch =
        order.productName.toLowerCase().includes(this.searchText.toLowerCase()) ||
        order.productReference.toLowerCase().includes(this.searchText.toLowerCase()) ||
        order.supplier.toLowerCase().includes(this.searchText.toLowerCase());

      const matchesSupplier = this.selectedSupplier === 'Tous les fournisseurs' ||
        order.supplier === this.selectedSupplier;

      const matchesStatus = this.selectedOrderStatus === 'Tous les statuts' ||
        order.status === this.selectedOrderStatus;

      return matchesSearch && matchesSupplier && matchesStatus;
    });
  }

  toggleSupplierDropdown() {
    this.showSupplierDropdown = !this.showSupplierDropdown;
    this.showOrderStatusDropdown = false;
  }

  toggleOrderStatusDropdown() {
    this.showOrderStatusDropdown = !this.showOrderStatusDropdown;
    this.showSupplierDropdown = false;
  }

  selectSupplier(supplier: string) {
    this.selectedSupplier = supplier;
    this.showSupplierDropdown = false;
  }

  selectOrderStatus(status: string) {
    this.selectedOrderStatus = status;
    this.showOrderStatusDropdown = false;
  }

  get uniqueStatuses(): string[] {
    return ['Tous les statuts', 'Suffisant', 'Sous seuil', 'Rupture'];
  }

  get filteredStockItems(): StockItem[] {
    return this.stockItems.filter(item => {
      const matchesSearch =
        item.name.toLowerCase().includes(this.searchText.toLowerCase()) ||
        item.reference.toLowerCase().includes(this.searchText.toLowerCase());

      const matchesCategory = this.selectedCategory === 'Toutes les catégories' ||
        item.category === this.selectedCategory;

      const matchesStatus = this.selectedStatus === 'Tous les statuts' ||
        item.status === this.selectedStatus;

      return matchesSearch && matchesCategory && matchesStatus;
    });
  }

  toggleCategoryDropdown() {
    this.showCategoryDropdown = !this.showCategoryDropdown;
    this.showStatusDropdown = false;
  }

  toggleStatusDropdown() {
    this.showStatusDropdown = !this.showStatusDropdown;
    this.showCategoryDropdown = false;
  }

  selectCategory(category: string) {
    this.selectedCategory = category;
    this.showCategoryDropdown = false;
    this.loadStockList();
    this.loadStockAlerts();
  }

  selectStatus(status: string) {
    this.selectedStatus = status;
    this.showStatusDropdown = false;
  }

  // Modal state
  showStockEntryModal = false;
  selectedStockItem: StockItem | null = null;
  stockEntryQuantity: number = 1;

  // New Order Modal
  showNewOrderModal = false;
  newOrder: any = {
    fournisseur: '',
    produit: '',
    quantite: '',
    eta: '',
    note: ''
  };

  suppliers: { id: number; name: string }[] = [];
  allProducts: { id: number; name: string }[] = [];

  errors: any = {
    fournisseur: '',
    produit: '',
    quantite: '',
    eta: ''
  };

  get minDate(): string {
    return new Date().toISOString().split('T')[0];
  }

  openNewOrderModal() {
    this.showNewOrderModal = true;
    this.newOrder = {
      fournisseur: '',
      produit: '',
      quantite: '',
      eta: '',
      note: ''
    };
    this.errors = {
      fournisseur: '',
      produit: '',
      quantite: '',
      eta: ''
    };
  }

  closeNewOrderModal() {
    this.showNewOrderModal = false;
  }

  saveNewOrder() {
    // Reset errors
    this.errors = {
      fournisseur: '',
      produit: '',
      quantite: '',
      eta: ''
    };

    let isValid = true;

    // Validation Fournisseur
    if (!this.newOrder.fournisseur) {
      this.errors.fournisseur = 'Le fournisseur est obligatoire';
      isValid = false;
    }

    // Validation Produit
    if (!this.newOrder.produit) {
      this.errors.produit = 'Le produit est obligatoire';
      isValid = false;
    }

    // Validation Quantité
    if (!this.newOrder.quantite) {
      this.errors.quantite = 'La quantité est obligatoire';
      isValid = false;
    } else if (isNaN(Number(this.newOrder.quantite))) {
      this.errors.quantite = 'La quantité doit être un nombre';
      isValid = false;
    }

    // Validation Date (ETA)
    if (this.newOrder.eta) {
      const selectedDate = new Date(this.newOrder.eta);
      const today = new Date();
      today.setHours(0, 0, 0, 0);

      if (selectedDate < today) {
        this.errors.eta = 'La date ne peut pas être dans le passé';
        isValid = false;
      }
    }

    if (!isValid) {
      return;
    }

    const payload = {
      supplierId: Number(this.newOrder.fournisseur),
      items: [
        {
          productId: Number(this.newOrder.produit),
          quantite: Number(this.newOrder.quantite)
        }
      ],
      expectedDate: this.formatDateForApi(this.newOrder.eta),
      notes: this.newOrder.note?.trim() || undefined
    };

    this.logisticsService.createSupplierOrder(payload).subscribe({
      next: () => {
        this.closeNewOrderModal();
        Swal.fire({
          iconHtml: '<img src="/icones/message success.svg" style="width: 95px; height: 95px; margin: 0 auto;" />',
          title: 'Commande fournisseur créée avec succès',
          showConfirmButton: false,
          timer: 1500,
          buttonsStyling: false,
          customClass: {
            popup: 'rounded-3xl p-6',
            title: 'text-xl font-medium text-gray-900',
            icon: 'border-none'
          },
          backdrop: `rgba(0,0,0,0.2)`,
          width: '580px'
        });
        this.loadStockStats();
        this.loadStockList();
        this.loadStockAlerts();
      },
      error: (error) => {
        console.error('Erreur lors de la création de commande:', error);
        Swal.fire({
          iconHtml: `<svg width="95" height="95" viewBox="0 0 95 95" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M47.5 0C21.266 0 0 21.266 0 47.5C0 73.734 21.266 95 47.5 95C73.734 95 95 73.734 95 47.5C95 21.266 73.734 0 47.5 0ZM47.5 86.25C26.095 86.25 8.75 68.905 8.75 47.5C8.75 26.095 26.095 8.75 47.5 8.75C68.905 8.75 86.25 26.095 86.25 47.5C86.25 68.905 68.905 86.25 47.5 86.25ZM43.125 24.375V52.5H51.875V24.375H43.125ZM43.125 60.625V69.375H51.875V60.625H43.125Z" fill="#F87171"/>
          </svg>`,
          title: 'Création échouée',
          text: error?.error?.message || 'Une erreur est survenue lors de la création de la commande.',
          showConfirmButton: true,
          confirmButtonText: 'OK',
          buttonsStyling: false,
          customClass: {
            popup: 'rounded-3xl p-6',
            title: 'text-xl font-medium text-gray-900',
            htmlContainer: 'text-base text-gray-600',
            confirmButton: 'bg-[#2C2D5B] hover:bg-[#232b5c] text-white px-8 py-3 rounded-lg font-medium text-base shadow-none border-none'
          },
          backdrop: `rgba(0,0,0,0.2)`,
          width: '580px'
        });
      }
    });
  }

  openApprovisionnementModal(item: StockItem) {
    const today = new Date().toISOString().split('T')[0];

    this.showNewOrderModal = true;
    this.newOrder = {
      fournisseur: '',
      produit: item.id,
      quantite: '',
      eta: today,
      note: ''
    };
    this.errors = {
      fournisseur: '',
      produit: '',
      quantite: '',
      eta: ''
    };
  }

  openStockEntryModal(item: StockItem) {
    this.selectedStockItem = item;
    this.stockEntryQuantity = 1;
    this.showStockEntryModal = true;
  }

  closeStockEntryModal() {
    this.showStockEntryModal = false;
    this.selectedStockItem = null;
    this.stockEntryQuantity = 1;
  }

  saveStockEntry() {
    if (this.selectedStockItem && this.stockEntryQuantity > 0) {
      this.logisticsService.increaseStock(this.selectedStockItem.id, this.stockEntryQuantity).subscribe({
        next: () => {
          this.closeStockEntryModal();
          this.loadStockStats();
          this.loadStockList();
          this.loadStockAlerts();
        },
        error: (error) => {
          console.error('Erreur lors de l\'entrée de stock:', error);
        }
      });
    }
  }

  // Stock Exit Modal
  showStockExitModal = false;
  stockExitQuantity: number = 1;

  openStockExitModal(item: StockItem) {
    this.selectedStockItem = item;
    this.stockExitQuantity = 1;
    this.showStockExitModal = true;
  }

  closeStockExitModal() {
    this.showStockExitModal = false;
    this.selectedStockItem = null;
    this.stockExitQuantity = 1;
  }

  saveStockExit() {
    if (this.selectedStockItem && this.stockExitQuantity > 0) {
      this.logisticsService.decreaseStock(this.selectedStockItem.id, this.stockExitQuantity).subscribe({
        next: () => {
          this.closeStockExitModal();
          this.loadStockStats();
          this.loadStockList();
          this.loadStockAlerts();
        },
        error: (error) => {
          console.error('Erreur lors de la sortie de stock:', error);
        }
      });
    }
  }

  // Threshold Modal
  showThresholdModal = false;
  thresholdValue: number = 0;

  openThresholdModal(item: StockItem) {
    this.selectedStockItem = item;
    this.thresholdValue = item.minThreshold;
    this.showThresholdModal = true;
  }

  closeThresholdModal() {
    this.showThresholdModal = false;
    this.selectedStockItem = null;
    this.thresholdValue = 0;
  }

  saveThreshold() {
    if (this.selectedStockItem && this.thresholdValue >= 0) {
      this.logisticsService.updateMinThreshold(this.selectedStockItem.id, this.thresholdValue).subscribe({
        next: () => {
          this.closeThresholdModal();
          this.loadStockStats();
          this.loadStockList();
          this.loadStockAlerts();
        },
        error: (error) => {
          console.error('Erreur lors de la mise à jour du seuil:', error);
        }
      });
    }
  }

  supplierOrders: SupplierOrder[] = [
    {
      id: '1',
      productName: 'Riz parfumé 25kg',
      productReference: 'CP-2025-05',
      supplier: 'Dakar Foods',
      quantity: 200,
      eta: '12/10/2025',
      status: 'Ouverte',
      productId: 'riz'
    },
    {
      id: '2',
      productName: 'Lait 1L',
      productReference: 'CP-2025-02',
      supplier: 'Sahel Agro',
      quantity: 60,
      eta: '10/10/2025',
      status: 'Receptionée',
      productId: 'lait'
    }
  ];

  getSupplierOrderStatusClass(status: string): string {
    switch (status) {
      case 'Ouverte': return 'bg-green-50 text-green-600';
      case 'Receptionée': return 'bg-red-50 text-red-600';
      case 'Annulée': return 'bg-gray-50 text-gray-600';
      default: return 'bg-gray-50 text-gray-600';
    }
  }

  getSupplierOrderStatusDotClass(status: string): string {
    switch (status) {
      case 'Ouverte': return 'bg-green-500';
      case 'Receptionée': return 'bg-red-500';
      case 'Annulée': return 'bg-gray-500';
      default: return 'bg-gray-500';
    }
  }

  cancelOrder(order: SupplierOrder) {
    Swal.fire({
      title: 'Annuler la commande fournisseur ?',
      text: `${order.productName} x${order.quantity}`,
      iconHtml: `<svg width="95" height="95" viewBox="0 0 95 95" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M51.2402 58.166L43.8184 58.166L43.8184 28.4785L51.2402 28.4785L51.2402 58.166Z" fill="#EFBD8E"/>
        <path d="M47.5 65.291C45.3685 65.291 43.6406 67.0189 43.6406 69.1504C43.6406 71.2819 45.3685 73.0098 47.5 73.0098C49.6315 73.0098 51.3594 71.2819 51.3594 69.1504C51.3594 67.0189 49.6315 65.291 47.5 65.291Z" fill="#EFBD8E"/>
        <path d="M47.5 94.9776C59.6972 95.3703 71.5436 90.595 80.446 81.6968C89.3483 72.7986 94.5815 60.5025 95 47.5C94.5815 34.4975 89.3483 22.2014 80.446 13.3032C71.5436 4.40504 59.6972 -0.370316 47.5 0.0224457C35.3028 -0.370316 23.4564 4.40504 14.554 13.3032C5.65166 22.2014 0.418472 34.4975 0 47.5C0.418472 60.5025 5.65166 72.7986 14.554 81.6968C23.4564 90.595 35.3028 95.3703 47.5 94.9776ZM47.5 7.93537C57.7293 7.54133 67.6886 11.4827 75.1994 18.8972C82.7101 26.3117 87.1609 36.5959 87.5781 47.5C87.1609 58.4041 82.7101 68.6883 75.1994 76.1028C67.6886 83.5174 57.7293 87.4587 47.5 87.0646C37.2707 87.4587 27.3114 83.5174 19.8006 76.1028C12.2899 68.6883 7.83908 58.4041 7.42188 47.5C7.83908 36.5959 12.2899 26.3117 19.8006 18.8972C27.3114 11.4827 37.2707 7.54133 47.5 7.93537Z" fill="#EFBD8E"/>
      </svg>`,
      showCancelButton: true,
      confirmButtonText: 'Oui',
      cancelButtonText: 'Annuler',
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-3xl p-6',
        title: 'text-2xl font-medium text-gray-900',
        htmlContainer: 'text-lg text-gray-600',
        confirmButton: 'bg-[#EF4444] hover:bg-[#DC2626] text-white px-8 py-3 rounded-lg font-medium text-base shadow-none border-none',
        cancelButton: 'bg-[#F3F4F6] hover:bg-gray-200 text-gray-700 px-8 py-3 rounded-lg font-medium text-base shadow-none border-none',
        actions: 'flex justify-center w-full gap-2',
        icon: 'border-none'
      },
      backdrop: `rgba(0,0,0,0.2)`,
      width: '580px'
    }).then((result) => {
      if (result.isConfirmed) {
        order.status = 'Annulée';
      }
    });
  }

  increaseThreshold(item: StockItem) {
    this.logisticsService.updateMinThresholdByPercent(item.id, 10).subscribe({
      next: () => {
        Swal.fire({
          title: 'Seuil augmenté de 10%',
          iconHtml: `<svg width="80" height="80" viewBox="0 0 80 80" fill="none" xmlns="http://www.w3.org/2000/svg">
            <circle cx="40" cy="40" r="37.5" stroke="#388E3C" stroke-width="5"/>
            <path d="M22.5 40L35 52.5L57.5 30" stroke="#388E3C" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>`,
          showConfirmButton: false,
          timer: 2000,
          customClass: {
            popup: 'rounded-3xl p-4',
            title: 'text-xl font-medium text-gray-900',
            icon: 'border-none'
          },
          backdrop: `rgba(0,0,0,0.2)`,
          width: '500px'
        });
        this.loadStockStats();
        this.loadStockList();
        this.loadStockAlerts();
      },
      error: (error) => {
        console.error('Erreur lors de la mise à jour du seuil:', error);
      }
    });
  }

  confirmReception(order: SupplierOrder) {
    Swal.fire({
      title: 'Réceptionner la commande ?',
      text: `${order.productName} x${order.quantity}`,
      iconHtml: `<svg width="95" height="95" viewBox="0 0 95 95" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M51.2402 58.166L43.8184 58.166L43.8184 28.4785L51.2402 28.4785L51.2402 58.166Z" fill="#EFBD8E"/>
        <path d="M47.5 65.291C45.3685 65.291 43.6406 67.0189 43.6406 69.1504C43.6406 71.2819 45.3685 73.0098 47.5 73.0098C49.6315 73.0098 51.3594 71.2819 51.3594 69.1504C51.3594 67.0189 49.6315 65.291 47.5 65.291Z" fill="#EFBD8E"/>
        <path d="M47.5 94.9776C59.6972 95.3703 71.5436 90.595 80.446 81.6968C89.3483 72.7986 94.5815 60.5025 95 47.5C94.5815 34.4975 89.3483 22.2014 80.446 13.3032C71.5436 4.40504 59.6972 -0.370316 47.5 0.0224457C35.3028 -0.370316 23.4564 4.40504 14.554 13.3032C5.65166 22.2014 0.418472 34.4975 0 47.5C0.418472 60.5025 5.65166 72.7986 14.554 81.6968C23.4564 90.595 35.3028 95.3703 47.5 94.9776ZM47.5 7.93537C57.7293 7.54133 67.6886 11.4827 75.1994 18.8972C82.7101 26.3117 87.1609 36.5959 87.5781 47.5C87.1609 58.4041 82.7101 68.6883 75.1994 76.1028C67.6886 83.5174 57.7293 87.4587 47.5 87.0646C37.2707 87.4587 27.3114 83.5174 19.8006 76.1028C12.2899 68.6883 7.83908 58.4041 7.42188 47.5C7.83908 36.5959 12.2899 26.3117 19.8006 18.8972C27.3114 11.4827 37.2707 7.54133 47.5 7.93537Z" fill="#EFBD8E"/>
      </svg>`,
      showCancelButton: true,
      confirmButtonText: 'Réceptionner',
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
      width: '580px'
    }).then((result) => {
      if (result.isConfirmed) {
        order.status = 'Receptionée';
        // Find the corresponding stock item and update its stock
        const stockItem = this.stockItems.find(item => item.id === order.productId || item.reference === order.productReference);
        if (stockItem) {
          stockItem.stock += order.quantity;
          // Update status if needed
          if (stockItem.stock > stockItem.minThreshold) {
            stockItem.status = 'Suffisant';
          }
        }
      }
    });
  }

  onSearch(): void {
    this.loadStockList();
    this.loadStockAlerts();
  }

  exportData(): void {
    const categoryId = this.findCategoryIdByName(this.selectedCategory);
    if (this.activeTab === 'alertes') {
      this.logisticsService.exportStockAlerts(this.searchText, categoryId ?? undefined).subscribe({
        next: (blob) => this.downloadFile(blob, 'alertes_stock'),
        error: (error) => console.error('Erreur export alertes:', error)
      });
      return;
    }
    this.logisticsService.exportStockList(this.searchText, categoryId ?? undefined).subscribe({
      next: (blob) => this.downloadFile(blob, 'suivi_stocks'),
      error: (error) => console.error('Erreur export stocks:', error)
    });
  }

  private loadCategories(): void {
    this.productService.getCategories().subscribe({
      next: (categories) => {
        this.categories = Array.isArray(categories) ? categories : [];
      },
      error: (error) => {
        console.error('Erreur lors du chargement des catégories:', error);
      }
    });
  }

  private loadStockStats(): void {
    this.logisticsService.getStockStats().subscribe({
      next: (stats) => {
        this.metricsData = [
          { title: 'Total', value: String(stats.total), subtitle: 'Catalogue suivi', icon: 'box-blue' },
          { title: 'Stocks sous seuil', value: String(stats.lowStock), subtitle: 'À réapprovisionner', icon: 'warning-yellow' },
          { title: 'Ruptures', value: String(stats.outOfStock), subtitle: 'Stock = 0', icon: 'box-red' }
        ];
      },
      error: (error) => {
        console.error('Erreur lors du chargement des stats:', error);
      }
    });
  }

  private loadStockList(): void {
    const categoryId = this.findCategoryIdByName(this.selectedCategory);
    this.logisticsService.getStockList(0, 1000, this.searchText, categoryId ?? undefined).subscribe({
      next: (response) => {
        const items = response?.content ?? [];
        this.stockItems = items.map((item: any) => this.mapApiStockItem(item));
      },
      error: (error) => {
        console.error('Erreur lors du chargement des stocks:', error);
      }
    });
  }

  private loadStockAlerts(): void {
    const categoryId = this.findCategoryIdByName(this.selectedCategory);
    this.logisticsService.getStockAlerts(0, 1000, this.searchText, categoryId ?? undefined).subscribe({
      next: (response) => {
        const items = response?.content ?? [];
        this.alertItems = items.map((item: any) => this.mapApiStockItem(item));
      },
      error: (error) => {
        console.error('Erreur lors du chargement des alertes:', error);
      }
    });
  }

  private loadSuppliers(): void {
    this.logisticsService.getSuppliers().subscribe({
      next: (suppliers) => {
        this.suppliers = Array.isArray(suppliers) ? suppliers : [];
      },
      error: (error) => {
        console.error('Erreur lors du chargement des fournisseurs:', error);
      }
    });
  }

  private loadAllProducts(): void {
    this.logisticsService.getStockList(0, 1000).subscribe({
      next: (response) => {
        const items = response?.content ?? [];
        this.allProducts = items.map((item: any) => ({
          id: Number(item?.id),
          name: item?.name ?? ''
        }));
      },
      error: (error) => {
        console.error('Erreur lors du chargement des produits:', error);
      }
    });
  }

  private mapApiStockItem(item: any): StockItem {
    return {
      id: item?.id?.toString() ?? '',
      name: item?.name ?? '',
      reference: item?.productCode ?? '',
      category: item?.categoryName ?? '',
      stock: item?.currentStock ?? 0,
      minThreshold: item?.minThreshold ?? 0,
      status: this.normalizeStockStatus(item?.stockStatus),
      image: this.buildImageUrl(item?.image)
    };
  }

  getProductNameById(productId: string): string {
    const id = Number(productId);
    const match = this.allProducts.find(p => p.id === id);
    return match ? match.name : '';
  }

  private normalizeStockStatus(status: string | undefined): StockItem['status'] {
    if (!status) return 'Suffisant';
    if (status === 'SUFFISANT' || status === 'Suffisant') return 'Suffisant';
    if (status === 'SOUS_SEUIL' || status === 'Sous seuil') return 'Sous seuil';
    if (status === 'RUPTURE' || status === 'Rupture') return 'Rupture';
    return 'Suffisant';
  }

  private buildImageUrl(image: string | undefined): string {
    if (!image) return '/icones/default-product.svg';
    if (image.startsWith('http') || image.startsWith('/')) return image;
    return `${environment.apiUrl}/files/${image}`;
  }

  private findCategoryIdByName(name: string): number | null {
    if (!name || name === 'Toutes les catégories') return null;
    const match = this.categories.find(c => c.name === name);
    return match ? match.id : null;
  }

  private formatDateForApi(value?: string): string | undefined {
    if (!value) return undefined;
    const [year, month, day] = value.split('-');
    if (!year || !month || !day) return undefined;
    return `${day}-${month}-${year} 00:00:00`;
  }

  private downloadFile(blob: Blob, baseName: string): void {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${baseName}_${new Date().toISOString().slice(0, 10)}.xlsx`;
    a.click();
    window.URL.revokeObjectURL(url);
  }

  metricsData: MetricCard[] = [];

  stockItems: StockItem[] = [];
  alertItems: StockItem[] = [];

  getStatusClass(status: string): string {
    switch (status) {
      case 'Suffisant':
        return 'bg-[#0A97480F] text-[#0A9748]';
      case 'Sous seuil':
        return 'bg-[#FF914D0F] text-[#FF914D]';
      case 'Rupture':
        return 'bg-[#FF09090F] text-[#FF0909]';
      default:
        return 'bg-gray-100 text-gray-700';
    }
  }

  getStatusDotClass(status: string): string {
    switch (status) {
      case 'Suffisant':
        return 'bg-[#0A9748]';
      case 'Sous seuil':
        return 'bg-[#FF914D]';
      case 'Rupture':
        return 'bg-[#FF0909]';
      default:
        return 'bg-gray-500';
    }
  }
}
