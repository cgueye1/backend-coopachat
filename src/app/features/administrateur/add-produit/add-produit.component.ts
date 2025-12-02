import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { ProductService } from '../../../shared/services/product.service';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-add-produit',
  standalone: true,
  imports: [MainLayoutComponent, HeaderComponent, CommonModule, FormsModule],
  templateUrl: './add-produit.component.html',
  styles: ``
})
export class AddProduitComponent {
  newProduct = {
    name: '',
    category: '',
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

  categories = ['Épicerie', 'Boissons', 'Frais', 'Hygiène', 'Surgelés'];
  showCategoryDropdown = false;
  selectedFile: File | null = null;
  imagePreview: string | null = null;

  constructor(private router: Router, private productService: ProductService) { }

  goBack() {
    this.router.navigate(['/admin/catalogue']);
  }

  toggleCategoryDropdown() {
    this.showCategoryDropdown = !this.showCategoryDropdown;
  }

  selectCategory(category: string) {
    this.newProduct.category = category;
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

    if (!this.newProduct.category) {
      this.errors.category = 'La catégorie est requise';
      isValid = false;
    }

    if (this.newProduct.price === null || this.newProduct.price <= 0) {
      this.errors.price = 'Le prix unitaire doit être supérieur à 0';
      isValid = false;
    }

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

    return isValid;
  }

  enregistrer() {
    if (this.validateForm()) {
      // Convert image to base64 or use a default icon
      const icon = this.imagePreview || '/icones/default-product.svg';

      this.productService.addProduct({
        name: this.newProduct.name,
        category: this.newProduct.category,
        price: this.newProduct.price!,
        stock: this.newProduct.stock!,
        description: this.newProduct.description,
        icon: icon
      });

      this.showSuccessMessage();
    }
  }

  showSuccessMessage() {
    Swal.fire({
      iconHtml: '<img src="/icones/message success.svg" style="width: 95px; height: 95px; margin: 0 auto;" />',
      title: 'Produit ajouté avec succès',
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
    }).then(() => {
      this.router.navigate(['/admin/catalogue']);
    });
  }

  annuler() {
    this.router.navigate(['/admin/catalogue']);
  }
}
