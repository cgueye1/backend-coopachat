import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { Router } from '@angular/router';
import { CommercialService, CreatePromotionPayload } from '../../../shared/services/commercial.service';
import Swal from 'sweetalert2';

type PromotionType = 'product' | 'category';

interface ProductOption {
  id: number;
  name: string;
}

interface ProductRow {
  productId: number;
  productName: string;
  discountValue: number | null;
  selected?: boolean;
}

@Component({
  selector: 'app-promotions-produits',
  standalone: true,
  imports: [CommonModule, FormsModule, MainLayoutComponent, HeaderComponent],
  templateUrl: './promotions-produits.component.html',
})
export class PromotionsProduitsComponent implements OnInit {
  step = 1;
  promotionType: PromotionType = 'product';

  name = '';
  startDate = '';
  endDate = '';

  productSearch = '';
  productOptions: ProductOption[] = [];
  productOptionsLoading = false;
  productRows: ProductRow[] = [];

  categories: ProductOption[] = [];
  selectedCategoryId: number | null = null;
  categoryProducts: ProductRow[] = [];
  categoryProductsLoading = false;

  submitting = false;

  constructor(
    private commercialService: CommercialService,
    private router: Router,
  ) {}

  backToList(): void {
    this.router.navigate(['/com/promotions-produits']);
  }

  ngOnInit(): void {
    this.loadProductOptions();
    this.loadCategories();
  }

  get canGoStep2(): boolean {
    return !!(this.name?.trim() && this.startDate && this.endDate);
  }

  get canSubmit(): boolean {
    const items = this.getProductItems();
    return items.length > 0 && items.every(i => i.discountValue != null && i.discountValue >= 1 && i.discountValue <= 100);
  }

  private loadProductOptions(): void {
    this.productOptionsLoading = true;
    this.commercialService.getProductsForPromotion().subscribe({
      next: (list) => {
        this.productOptions = list;
        this.productOptionsLoading = false;
      },
      error: () => {
        this.productOptionsLoading = false;
      },
    });
  }

  private loadCategories(): void {
    this.commercialService.getCategoriesForCoupon().subscribe({
      next: (list) => {
        this.categories = list;
      },
    });
  }

  onCategoryChange(): void {
    this.categoryProducts = [];
    if (this.selectedCategoryId == null) return;
    this.categoryProductsLoading = true;
    this.commercialService.getProductsForPromotion(this.selectedCategoryId).subscribe({
      next: (list) => {
        this.categoryProducts = list.map(p => ({
          productId: p.id,
          productName: p.name,
          discountValue: null as number | null,
          selected: false,
        }));
        this.categoryProductsLoading = false;
      },
      error: () => {
        this.categoryProductsLoading = false;
      },
    });
  }

  get filteredProductOptions(): ProductOption[] {
    const q = (this.productSearch || '').trim().toLowerCase();
    if (!q) return this.productOptions.slice(0, 20);
    return this.productOptions.filter(p => p.name.toLowerCase().includes(q)).slice(0, 20);
  }

  addProductFromSearch(product: ProductOption): void {
    if (this.productRows.some(r => r.productId === product.id)) return;
    this.productRows.push({
      productId: product.id,
      productName: product.name,
      discountValue: null,
    });
    this.productSearch = '';
  }

  removeProductRow(index: number): void {
    this.productRows.splice(index, 1);
  }

  getProductItems(): { productId: number; discountValue: number }[] {
    if (this.promotionType === 'product') {
      return this.productRows
        .filter(r => r.discountValue != null && r.discountValue >= 1 && r.discountValue <= 100)
        .map(r => ({ productId: r.productId, discountValue: r.discountValue! }));
    }
    return this.categoryProducts
      .filter(r => r.selected && r.discountValue != null && r.discountValue >= 1 && r.discountValue <= 100)
      .map(r => ({ productId: r.productId, discountValue: r.discountValue! }));
  }

  nextToStep2(): void {
    if (!this.canGoStep2) return;
    this.step = 2;
    if (this.promotionType === 'category' && this.selectedCategoryId != null) {
      this.onCategoryChange();
    }
  }

  backToStep1(): void {
    this.step = 1;
  }

  submit(): void {
    const items = this.getProductItems();
    if (items.length === 0) {
      Swal.fire({ icon: 'warning', title: 'Ajoutez au moins un produit avec une réduction (1–100 %).' });
      return;
    }
    const invalid = items.find(i => i.discountValue < 1 || i.discountValue > 100);
    if (invalid) {
      Swal.fire({ icon: 'warning', title: 'Les réductions doivent être entre 1 et 100 %.' });
      return;
    }

    const start = new Date(this.startDate);
    const end = new Date(this.endDate);
    if (end <= start) {
      Swal.fire({ icon: 'warning', title: 'La date de fin doit être après la date de début.' });
      return;
    }

    const payload: CreatePromotionPayload = {
      name: this.name.trim(),
      startDate: start.toISOString(),
      endDate: end.toISOString(),
      productItems: items,
    };

    this.submitting = true;
    this.commercialService.createPromotion(payload).subscribe({
      next: () => {
        this.submitting = false;
        Swal.fire({ icon: 'success', title: 'Promotion créée avec succès.' }).then(() => {
          this.router.navigate(['/com/promotions-produits']);
        });
        this.resetForm();
      },
      error: (err) => {
        this.submitting = false;
        const msg = err?.error?.message || err?.message || err?.error || 'Erreur lors de la création.';
        Swal.fire({ icon: 'error', title: 'Erreur', text: typeof msg === 'string' ? msg : 'Impossible de créer la promotion.' });
      },
    });
  }

  private resetForm(): void {
    this.step = 1;
    this.name = '';
    this.startDate = '';
    this.endDate = '';
    this.promotionType = 'product';
    this.productRows = [];
    this.productSearch = '';
    this.selectedCategoryId = null;
    this.categoryProducts = [];
  }
}
