import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { AdminService, CategoryListItemDTO } from '../../../shared/services/admin.service';
import { environment } from '../../../../environments/environment';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-categories',
  standalone: true,
  imports: [MainLayoutComponent, HeaderComponent, CommonModule, FormsModule],
  templateUrl: './categories.component.html',
  styles: ``
})
export class CategoriesComponent implements OnInit {
  categories: CategoryListItemDTO[] = [];
  searchText = '';
  loadingList = false;
  showModal = false;
  isEdit = false;
  editingId: number | null = null;
  formName = '';
  formIcon = '';
  /** Aperçu local immédiat (data URL) quand on choisit un fichier, comme pour les produits. */
  formIconPreviewDataUrl: string | null = null;
  loading = false;
  uploadingIcon = false;
  errorMessage = '';

  constructor(
    private adminService: AdminService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadCategories();
  }

  /** @param forceRefresh après création/édition, force un rechargement sans cache.
   * @param onLoaded appelé après mise à jour de la liste (ex. afficher un SweetAlert). */
  loadCategories(forceRefresh = false, onLoaded?: () => void): void {
    this.loadingList = true;
    this.iconLoadFailedIds.clear();
    this.adminService.getCategories(forceRefresh).subscribe({
      next: (list) => {
        const data = Array.isArray(list) ? list : [];
        // Dernière catégorie créée en premier (tri décroissant par id)
        this.categories = [...data].sort((a, b) => (b.id ?? 0) - (a.id ?? 0));
        this.loadingList = false;
        onLoaded?.();
      },
      error: (err) => {
        console.error('Erreur chargement catégories', err);
        this.loadingList = false;
        onLoaded?.();
      }
    });
  }

  openCreate(): void {
    this.isEdit = false;
    this.editingId = null;
    this.formName = '';
    this.formIcon = '';
    this.formIconPreviewDataUrl = null;
    this.errorMessage = '';
    this.showModal = true;
  }

  get filteredCategories(): CategoryListItemDTO[] {
    const term = (this.searchText ?? '').trim().toLowerCase();
    if (!term) return this.categories;
    return this.categories.filter((cat) => (cat.name ?? '').toLowerCase().includes(term));
  }

  get totalCategories(): number {
    return this.categories.length;
  }

  /** Ces infos ne sont pas encore garanties côté API, on garde une valeur sûre. */
  get totalProducts(): number {
    return this.categories.reduce((sum, cat) => sum + this.getCategoryProductCount(cat), 0);
  }

  get activeProducts(): number {
    return this.categories.reduce((sum, cat) => sum + this.getCategoryActiveProductCount(cat), 0);
  }

  openEdit(cat: CategoryListItemDTO): void {
    this.isEdit = true;
    this.editingId = cat.id;
    this.formIconPreviewDataUrl = null;
    this.formName = cat.name ?? '';
    this.formIcon = (cat.icon ?? '').trim();
    this.errorMessage = '';
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.errorMessage = '';
  }

  save(): void {
    this.errorMessage = '';
    const name = (this.formName ?? '').trim();
    if (!name) {
      this.errorMessage = 'Le nom est obligatoire.';
      return;
    }
    if (this.uploadingIcon || (this.formIconPreviewDataUrl && !(this.formIcon ?? '').trim())) {
      this.errorMessage = 'Veuillez attendre la fin de l\'envoi de l\'icône.';
      return;
    }
    this.loading = true;
    if (this.isEdit && this.editingId != null) {
      const body: { name?: string; icon?: string } = { name };
      if (this.formIcon !== undefined) body.icon = this.formIcon.trim() || undefined;
      this.adminService.updateCategory(this.editingId, body).subscribe({
        next: () => {
          this.loading = false;
          this.closeModal();
          this.loadCategories(true, () => this.showSuccessAlert('Catégorie modifiée avec succès'));
        },
        error: (err) => {
          this.loading = false;
          this.errorMessage = err?.error?.message || 'Erreur lors de la modification.';
        }
      });
    } else {
      const body: { name: string; icon?: string } = { name };
      if ((this.formIcon ?? '').trim()) body.icon = this.formIcon.trim();
      this.adminService.createCategory(body).subscribe({
        next: () => {
          this.loading = false;
          this.closeModal();
          this.loadCategories(true, () => this.showSuccessAlert('Catégorie créée avec succès'));
        },
        error: (err) => {
          this.loading = false;
          this.errorMessage = err?.error?.message || 'Erreur lors de la création.';
        }
      });
    }
  }

  iconDisplay(cat: CategoryListItemDTO): string {
    const icon = (cat.icon ?? '').trim();
    if (!icon) return '—';
    if (icon.startsWith('http') || icon.startsWith('/') || icon.includes('/')) {
      return '';
    }
    return icon;
  }

  /* l’image a échoué au chargement → on affiche le fallback (emoji / —). */
  /** Icône par défaut (fichier existant dans public/icones). */
  readonly defaultIconUrl = '/icones/catalogue.svg';
  iconLoadFailedIds = new Set<number>();

  /** True si la valeur ressemble à un chemin de fichier (pas un emoji, pas un chemin local file://). */
  private isIconFilePath(icon: string): boolean {
    const s = (icon ?? '').trim();
    if (!s) return false;
    if (s.startsWith('file://')) return false; // chemin local client → jamais envoyer au serveur
    if (s.startsWith('http') || s.startsWith('/')) return true;
    if (s.includes('/')) return true;
    const ext = s.split('.').pop()?.toLowerCase();
    return !!ext && ['svg', 'png', 'jpg', 'jpeg', 'gif', 'webp'].includes(ext);
  }

  /** URL pour afficher l’icône en <img> (null si emoji → afficher en <span>). */
  getIconUrl(cat: CategoryListItemDTO): string | null {
    if (this.iconLoadFailedIds.has(cat.id)) return this.defaultIconUrl;
    const icon = (cat.icon ?? '').trim();
    if (!icon || icon.startsWith('file://')) return null; // file:// = chemin local, pas servable par l’API
    if (!this.isIconFilePath(icon)) return null; // emoji → pas d’URL /files/
    if (icon.startsWith('http') || icon.startsWith('/')) return icon;
    const base = environment.imageServerUrl;
    return `${base}/files/${icon}`;
  }

  /** Pour compatibilité : URL ou défaut (utiliser getIconUrl + span emoji de préférence). */
  buildIconUrl(cat: CategoryListItemDTO): string {
    const url = this.getIconUrl(cat);
    if (url) return url;
    return this.defaultIconUrl;
  }

  onIconLoadError(cat: CategoryListItemDTO): void {
    this.iconLoadFailedIds.add(cat.id);
  }

  getCategoryProductCount(cat: CategoryListItemDTO): number {
    const raw = (cat as any)?.productCount;
    return typeof raw === 'number' && raw >= 0 ? raw : 0;
  }

  getCategoryActiveProductCount(cat: CategoryListItemDTO): number {
    const raw = (cat as any)?.activeProductCount;
    if (typeof raw === 'number' && raw >= 0) return raw;
    return this.getCategoryProductCount(cat);
  }

  addProduct(cat: CategoryListItemDTO): void {
    this.router.navigate(['/admin/add-produit'], {
      queryParams: { categoryId: cat.id }
    });
  }

  deleteCategory(cat: CategoryListItemDTO): void {
    if (!cat?.id) return;

    Swal.fire({
      title: 'Supprimer cette catégorie ?',
      html: `
        <div style="text-align:left">
          <p style="margin:0 0 8px 0">
            Vous êtes sur le point de supprimer <b>${(cat.name ?? 'cette catégorie')}</b>.
          </p>
          <p style="margin:0;color:#6B7280">
            Attention : les produits associés à cette catégorie seront aussi supprimés.
          </p>
        </div>
      `,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Oui, supprimer',
      cancelButtonText: 'Annuler',
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-3xl p-6',
        title: 'text-2xl font-medium text-gray-900',
        htmlContainer: 'text-base text-gray-600',
        confirmButton: 'bg-[#EF4444] hover:bg-[#DC2626] text-white px-8 py-3 rounded-lg font-medium text-base shadow-none border-none',
        cancelButton: 'bg-[#F3F4F6] hover:bg-gray-200 text-gray-700 px-8 py-3 rounded-lg font-medium text-base shadow-none border-none',
        actions: 'flex justify-center w-full gap-2',
        icon: 'border-none'
      },
      backdrop: 'rgba(0,0,0,0.2)',
      width: '580px'
    }).then((result) => {
      if (!result.isConfirmed) return;

      this.adminService.deleteCategory(cat.id).subscribe({
        next: () => {
          this.loadCategories(true, () => this.showSuccessAlert('Catégorie supprimée avec succès'));
        },
        error: (err) => {
          const msg = err?.error?.message || err?.error || 'Erreur lors de la suppression.';
          Swal.fire({ title: 'Erreur', text: msg, icon: 'error', confirmButtonText: 'OK' });
        }
      });
    });
  }

  /** Aperçu formulaire : URL seulement si chemin fichier (sinon afficher l’emoji en span). */
  formIconPreviewUrl(): string | null {
    const icon = (this.formIcon ?? '').trim();
    if (!icon || icon.startsWith('file://')) return null;
    if (!this.isIconFilePath(icon)) return null;
    if (icon.startsWith('http') || icon.startsWith('/')) return icon;
    const base = environment.imageServerUrl;
    return `${base}/files/${icon}`;
  }

  onIconFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.errorMessage = '';
    const reader = new FileReader();
    reader.onload = (e) => {
      this.formIconPreviewDataUrl = e.target?.result as string ?? null;
    };
    reader.readAsDataURL(file);
    this.uploadingIcon = true;
    this.adminService.uploadCategoryIcon(file).subscribe({
      next: (res) => {
        this.formIcon = res.path ?? '';
        this.uploadingIcon = false;
        input.value = '';
      },
      error: (err) => {
        this.uploadingIcon = false;
        this.errorMessage = err?.error?.message || err?.error || 'Erreur lors du téléversement de l’icône.';
        input.value = '';
      }
    });
  }

  private showSuccessAlert(message: string): void {
    Swal.fire({
      iconHtml: '<img src="/icones/message success.svg" style="width: 95px; height: 95px; margin: 0 auto;" />',
      title: message,
      showConfirmButton: false,
      timer: 1500,
      buttonsStyling: false,
      customClass: {
        popup: 'rounded-3xl p-6',
        title: 'text-xl font-medium text-gray-900',
        icon: 'border-none'
      },
      backdrop: 'rgba(0,0,0,0.2)',
      width: '580px',
      showClass: { popup: 'animate__animated animate__fadeIn animate__faster' },
      hideClass: { popup: 'animate__animated animate__fadeOut animate__faster' }
    });
  }
}
