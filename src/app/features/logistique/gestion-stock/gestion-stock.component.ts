import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import Swal from 'sweetalert2';

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
export class GestionStockComponent {
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

  get uniqueCategories(): string[] {
    const categories = new Set(this.stockItems.map(item => item.category));
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
    return this.stockItems.filter(item => {
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
    const statuses = new Set(this.stockItems.map(item => item.status));
    return ['Tous les statuts', ...Array.from(statuses)];
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

  errors: any = {
    fournisseur: '',
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
      quantite: '',
      eta: ''
    };

    let isValid = true;

    // Validation Fournisseur
    if (!this.newOrder.fournisseur || this.newOrder.fournisseur.trim() === '') {
      this.errors.fournisseur = 'Le fournisseur est obligatoire';
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

    const newCmd: SupplierOrder = {
      id: `CMD-${Math.floor(Math.random() * 10000)}`,
      productName: this.newOrder.produit,
      productReference: 'REF-' + Math.floor(Math.random() * 1000),
      supplier: this.newOrder.fournisseur,
      quantity: parseInt(this.newOrder.quantite) || 0,
      eta: this.newOrder.eta ? new Date(this.newOrder.eta).toLocaleDateString('fr-FR') : '',
      status: 'Ouverte',
      productId: 'PROD-' + Math.floor(Math.random() * 1000)
    };

    this.supplierOrders.unshift(newCmd);
    this.closeNewOrderModal();
  }

  openApprovisionnementModal(item: StockItem) {
    const today = new Date().toISOString().split('T')[0];

    this.showNewOrderModal = true;
    this.newOrder = {
      fournisseur: '',
      produit: item.name,
      quantite: '',
      eta: today,
      note: ''
    };
    this.errors = {
      fournisseur: '',
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
      // In a real app, this would call a service. 
      // For now, we just update the local state to reflect the change visually if needed, 
      // or just close the modal as requested "Mettre à jour la quantié lorsqu'on clique sur le bouton Enregistrer"
      // The user probably wants to see the stock increase.
      this.selectedStockItem.stock += this.stockEntryQuantity;
      this.closeStockEntryModal();
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
      this.selectedStockItem.stock -= this.stockExitQuantity;
      this.closeStockExitModal();
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
      this.selectedStockItem.minThreshold = this.thresholdValue;
      // Update status based on new threshold if needed
      if (this.selectedStockItem.stock <= this.thresholdValue) {
        this.selectedStockItem.status = this.selectedStockItem.stock === 0 ? 'Rupture' : 'Sous seuil';
      } else {
        this.selectedStockItem.status = 'Suffisant';
      }
      this.closeThresholdModal();
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
    item.minThreshold = Math.ceil(item.minThreshold * 1.10);

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

  get alertItems(): StockItem[] {
    return this.stockItems.filter(item => item.status === 'Sous seuil' || item.status === 'Rupture');
  }

  metricsData: MetricCard[] = [
    {
      title: 'Total',
      value: '07',
      subtitle: 'Catalogue suivi',
      icon: 'box-blue'
    },
    {
      title: 'Stocks sous seuil',
      value: '02',
      subtitle: 'À réapprovisionner',
      icon: 'warning-yellow'
    },
    {
      title: 'Ruptures',
      value: '0',
      subtitle: 'Stock = 0',
      icon: 'box-red'
    },
  ];

  stockItems: StockItem[] = [
    {
      id: '1',
      name: 'Riz parfumé 25kg',
      reference: 'CP-2025-05',
      category: 'Épicerie',
      stock: 42,
      minThreshold: 30,
      status: 'Suffisant',
      image: '/icones/riz.svg'
    },
    {
      id: '2',
      name: 'Huile 5L',
      reference: 'CP-2025-04',
      category: 'Épicerie',
      stock: 30,
      minThreshold: 25,
      status: 'Sous seuil',
      image: '/icones/huile.svg'
    },
    {
      id: '3',
      name: 'Eau 1.5L (x6)',
      reference: 'CP-2025-03',
      category: 'Boissons',
      stock: 90,
      minThreshold: 40,
      status: 'Suffisant',
      image: '/icones/eau.svg'
    },
    {
      id: '4',
      name: 'Lait 1L',
      reference: 'CP-2025-02',
      category: 'Frais',
      stock: 0,
      minThreshold: 30,
      status: 'Rupture',
      image: '/icones/lait.svg'
    },
    {
      id: '5',
      name: 'Savon 250g',
      reference: 'CP-2025-01',
      category: 'Hygiène',
      stock: 9,
      minThreshold: 15,
      status: 'Sous seuil',
      image: '/icones/savon.svg'
    }
  ];

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
