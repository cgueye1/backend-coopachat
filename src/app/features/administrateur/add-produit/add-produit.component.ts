import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { ProductService } from '../../../shared/services/product.service';
import Swal from 'sweetalert2';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-add-produit',
  standalone: true,
  imports: [MainLayoutComponent, HeaderComponent, CommonModule, FormsModule],
  templateUrl: './add-produit.component.html',
  styles: ``
})
export class AddProduitComponent implements OnInit {
  newProduct = {
    name: '',
    categoryId: null as number | null,
    categoryName: '',
    price: null as number | null,
    stock: null as number | null,
    description: ''
  };

  errors = {
    name: '',
    category: '',
    price: '',
    stock: '',
    description: '',
    image: ''
  };

  categories: { id: number; name: string }[] = [];
  showCategoryDropdown = false;
  selectedFile: File | null = null;
  imagePreview: string | null = null;

  isEditMode = false;
  editingProductId: string | null = null;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private productService: ProductService
  ) { }

  ngOnInit(): void {
    const productFromState = history.state?.product;
    const id = this.route.snapshot.queryParamMap.get('id');

    if (id) {
      this.isEditMode = true;
      this.editingProductId = id;
    }

    if (productFromState) {
      this.newProduct.name = productFromState.name || '';
      this.newProduct.categoryName = productFromState.category || '';
      this.newProduct.categoryId = this.findCategoryIdByName(productFromState.category);
      this.newProduct.price = this.parsePrice(productFromState.price);
      this.newProduct.stock = productFromState.stock ?? null;
      this.newProduct.description = productFromState.description || '';
      if (productFromState.icon) {
        this.imagePreview = productFromState.icon;
      }
    }

    this.loadCategories();

    if (this.editingProductId) {
      this.productService.getProductDetails(this.editingProductId).subscribe({
        next: (details) => {
          this.newProduct.name = details?.name ?? this.newProduct.name;
          this.newProduct.description = details?.description ?? this.newProduct.description;
          this.newProduct.categoryName = details?.categoryName ?? this.newProduct.categoryName;
          this.newProduct.categoryId = this.findCategoryIdByName(this.newProduct.categoryName);
          this.newProduct.price = details?.price ?? this.newProduct.price;
          if (details?.image) {
            this.imagePreview = this.buildImageUrl(details.image);
          }
        },
        error: (error) => {
          console.error('Erreur lors du chargement du produit:', error);
        }
      });
    }
  }

  goBack() {
    this.router.navigate(['/admin/catalogue']);
  }

  toggleCategoryDropdown() {
    this.showCategoryDropdown = !this.showCategoryDropdown;
  }

  selectCategory(category: { id: number; name: string }) {
    this.newProduct.categoryId = category.id;
    this.newProduct.categoryName = category.name;
    this.showCategoryDropdown = false;
    this.errors.category = '';
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      this.handleFile(input.files[0]);
    }
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    event.stopPropagation();

    if (event.dataTransfer?.files && event.dataTransfer.files[0]) {
      this.handleFile(event.dataTransfer.files[0]);
    }
  }

  handleFile(file: File) {
    // Validate file type
    if (!file.type.match(/image\/(jpeg|png)/)) {
      this.errors.image = 'Format de fichier non accepté. Utilisez JPG ou PNG.';
      return;
    }

    // Validate file size (5 MB max)
    if (file.size > 5 * 1024 * 1024) {
      this.errors.image = 'La taille du fichier ne doit pas dépasser 5 Mo.';
      return;
    }

    this.selectedFile = file;
    this.errors.image = '';

    // Generate preview
    const reader = new FileReader();
    reader.onload = (e) => {
      this.imagePreview = e.target?.result as string;
    };
    reader.readAsDataURL(file);
  }

  removeFile(event: Event) {
    event.stopPropagation();
    this.selectedFile = null;
    this.imagePreview = null;
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }

  validateForm(): boolean {
    let isValid = true;
    this.errors = {
      name: '',
      category: '',
      price: '',
      stock: '',
      description: '',
      image: ''
    };

    if (!this.newProduct.name.trim()) {
      this.errors.name = 'Le nom du produit est requis';
      isValid = false;
    }

    if (!this.newProduct.categoryId) {
      this.errors.category = 'La catégorie est requise';
      isValid = false;
    }

    if (this.newProduct.price === null || this.newProduct.price <= 0) {
      this.errors.price = 'Le prix unitaire doit être supérieur à 0';
      isValid = false;
    }

    if (!this.isEditMode) {
      if (this.newProduct.stock === null || this.newProduct.stock < 0) {
        this.errors.stock = 'Le stock initial doit être supérieur ou égal à 0';
        isValid = false;
      }

      if (!this.newProduct.description.trim()) {
        this.errors.description = 'La description est requise';
        isValid = false;
      }

      if (!this.selectedFile) {
        this.errors.image = 'L\'image du produit est requise';
        isValid = false;
      }
    }

    return isValid;
  }

  enregistrer() {
    if (this.validateForm()) {
      const formData = new FormData();
      formData.append('name', this.newProduct.name);
      formData.append('categoryId', String(this.newProduct.categoryId));
      formData.append('price', String(this.newProduct.price));
      if (!this.isEditMode) {
        formData.append('currentStock', String(this.newProduct.stock ?? 0));
        formData.append('description', this.newProduct.description);
        formData.append('minThreshold', '0');
        formData.append('status', 'true');
      } else {
        if (this.newProduct.description) {
          formData.append('description', this.newProduct.description);
        }
        formData.append('minThreshold', '0');
      }

      if (this.selectedFile) {
        formData.append('image', this.selectedFile);
      }

      const request$ = this.isEditMode && this.editingProductId
        ? this.productService.updateProduct(this.editingProductId, formData)
        : this.productService.createProduct(formData);

      request$.subscribe({
        next: () => this.showSuccessMessage(),
        error: (error) => {
          console.error('Erreur lors de la sauvegarde:', error);
        }
      });
    }
  }

  showSuccessMessage() {
    Swal.fire({
      iconHtml: '<img src="/icones/message success.svg" style="width: 95px; height: 95px; margin: 0 auto;" />',
      title: this.isEditMode ? 'Produit modifié avec succès' : 'Produit ajouté avec succès',
      showConfirmButton: false,
      timer: 800,
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
    }).then(() => {
      this.router.navigate(['/admin/catalogue']);
    });
  }

  annuler() {
    this.router.navigate(['/admin/catalogue']);
  }

  private findCategoryIdByName(name: string): number | null {
    const match = this.categories.find(c => c.name === name);
    return match ? match.id : null;
  }

  private parsePrice(price: string): number | null {
    if (!price) return null;
    const normalized = price.replace(/[^\d]/g, '');
    const value = Number(normalized);
    return Number.isNaN(value) ? null : value;
  }

  private buildImageUrl(image: string): string {
    if (!image) return '';
    if (image.startsWith('http') || image.startsWith('/')) return image;
    return `${environment.apiUrl}/files/${image}`;
  }

  private loadCategories(): void {
    this.productService.getCategories().subscribe({
      next: (categories) => {
        this.categories = Array.isArray(categories) ? categories : [];
        if (this.newProduct.categoryName && !this.newProduct.categoryId) {
          this.newProduct.categoryId = this.findCategoryIdByName(this.newProduct.categoryName);
        }
      },
      error: (error) => {
        console.error('Erreur lors du chargement des catégories:', error);
      }
    });
  }
}
