import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges, HostListener, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { ModalComponent } from '../../ui/modal/modal.component';
import { FormFieldComponent } from '../../ui/form-field/form-field.component';
import { CommercialService } from '../../services/commercial.service';

// Interface pour les données du formulaire
export interface CompanyFormData {
  nom: string;
  secteur: string;
  localisation: string;
  statut: string;
  contact: string;
  email?: string;
  telephone: string;
  note: string;
}

/** Données émises à la soumission (formulaire + fichier logo optionnel + demande de suppression logo) */
export interface CompanySubmitPayload {
  formData: CompanyFormData;
  logo?: File;
  deleteLogo?: boolean;
}

@Component({
  selector: 'app-company-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    ModalComponent,
    FormFieldComponent
  ],
  templateUrl: 'company-modal.component.html',
})
export class CompanyModalComponent implements OnInit, OnChanges, AfterViewChecked {
  @Input() isOpen = false;
  @Input() isSubmitting = false;
  @Input() title = 'Ajouter une nouvelle entreprise';
  @Input() initialData: CompanyFormData | null = null;
  @Input() logoPreviewUrl: string | null = null; // URL pour afficher le logo existant (mode édition)
  /** Secteurs chargés depuis l'API (référentiel). Si vide, le modal charge la liste via l'API. */
  @Input() sectorOptions: { id: number; name: string }[] = [];
  @Output() close = new EventEmitter<void>();
  @Output() submit = new EventEmitter<CompanySubmitPayload>();

  /** Secteurs chargés par le modal si le parent n'en a pas fourni (fallback). */
  sectorsLoadedByModal: { id: number; name: string }[] = [];

  selectedLogoFile: File | null = null;
  selectedLogoPreviewUrl: string | null = null;
  deleteLogoRequested = false; // true si l'utilisateur a cliqué Supprimer pour retirer le logo existant

  companyForm!: FormGroup;
  showSectorDropdown = false; // Dropdown des secteurs d'activité
  /** Liste effective des secteurs (input du parent ou chargée par le modal). */
  get effectiveSectorOptions(): { id: number; name: string }[] {
    return (this.sectorOptions?.length ? this.sectorOptions : this.sectorsLoadedByModal);
  }
  /** Noms des secteurs pour le dropdown. */
  get secteurOptions(): string[] {
    return this.effectiveSectorOptions.map(s => s.name);
  }
  // Liste filtrée des secteurs d'activité
  filteredSectors: string[] = [];
  private lastOpenState = false;

  @ViewChild('formContainer') formContainerRef?: ElementRef<HTMLElement>;

  constructor(
    private fb: FormBuilder,
    private commercialService: CommercialService
  ) {}

  // Fonction pour mettre le focus sur le premier champ du formulaire lorsque la modale s'ouvre
  ngAfterViewChecked(): void {
    if (this.isOpen && !this.lastOpenState && this.formContainerRef?.nativeElement) {
      this.lastOpenState = true;
      setTimeout(() => {
        const first = this.formContainerRef?.nativeElement?.querySelector<HTMLInputElement | HTMLTextAreaElement>('input:not([type="hidden"]), textarea');
        if (first) {
          first.focus();
          if (first.setSelectionRange) {
            first.setSelectionRange(0, 0);
          }
        }
      }, 150);
    }
    if (!this.isOpen) this.lastOpenState = false;
  }

  // Écouteur pour fermer le dropdown des secteurs d'activité lors d'un clic sur le document
  @HostListener('document:click')
  onDocumentClick(): void {
    this.showSectorDropdown = false;
  }

  onSectorInput(): void {
    const q = (this.companyForm.get('secteur')?.value || '').trim().toLowerCase();
    const options = this.secteurOptions;
    const filtered = q ? options.filter(s => s.toLowerCase().includes(q)) : options;
    // Toujours afficher la liste : si aucun filtre ne correspond, montrer tous les secteurs
    this.filteredSectors = filtered.length > 0 ? filtered : [...options];
    this.showSectorDropdown = true;
  }

  selectSector(secteur: string): void {
    this.companyForm.get('secteur')?.setValue(secteur);
    this.showSectorDropdown = false;
    this.filteredSectors = [...this.secteurOptions];
  }

  // Fonction pour initialiser le formulaire
  ngOnInit() {
    this.initForm();
  }

  // Fonction pour mettre à jour le formulaire lors des changements
  ngOnChanges(changes: SimpleChanges) {
    if (changes['initialData'] && this.companyForm) {
      if (this.initialData) {
        this.companyForm.patchValue(this.initialData);
      } else {
        this.companyForm.reset();
        this.companyForm.patchValue({ statut: 'En attente' });
        this.clearLogoSelection();
      }
    }
    if (changes['logoPreviewUrl']) {
      if (!this.logoPreviewUrl) this.clearLogoSelection();
    }
    // Charger la liste des secteurs si le modal s'ouvre et qu'aucun secteur n'a été fourni
    if (changes['isOpen'] && this.isOpen && (!this.sectorOptions?.length)) {
      this.commercialService.getCompanySectors().subscribe({
        next: (list) => {
          this.sectorsLoadedByModal = list.map(s => ({ id: s.id, name: s.name }));
          this.onSectorInput();
        },
        error: () => { this.sectorsLoadedByModal = []; }
      });
    }
  }

  // Fonction pour initialiser le formulaire
  private initForm() {
    this.companyForm = this.fb.group({
      nom: ['', [Validators.required, Validators.minLength(2)]],
      secteur: [''],
      localisation: ['', [Validators.required]],
      statut: ['En attente'],
      contact: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      telephone: ['', [Validators.required, Validators.pattern(/^[+]?[0-9\s\-\(\)]+$/)]],
      note: ['']
    });
  }

  // Fonction pour obtenir les erreurs de validation d'un champ
  getFieldError(fieldName: string): string {
    const field = this.companyForm.get(fieldName);
    if (field?.invalid && field?.touched) {
      if (field.errors?.['required']) {
        return 'Ce champ est requis';
      }
      if (field.errors?.['minlength']) {
        return `Minimum ${field.errors?.['minlength'].requiredLength} caractères`;
      }
      if (field.errors?.['email'] && fieldName === 'email') {
        return 'Format email invalide';
      }
      if (field.errors?.['pattern'] && fieldName === 'telephone') {
        return 'Format de téléphone invalide';
      }
    }
    return '';
  }

  onClose() {
    this.companyForm.reset();
    this.companyForm.patchValue({ statut: 'En attente' });
    this.clearLogoSelection();
    this.close.emit();
  }

  onLogoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    const ext = file.name.split('.').pop()?.toLowerCase();
    if (ext !== 'jpg' && ext !== 'jpeg' && ext !== 'png') {
      return; // Formats acceptés: JPG, PNG
    }
    if (file.size > 5 * 1024 * 1024) return; // Max 5 Mo
    this.selectedLogoFile = file;
    this.deleteLogoRequested = false; // Nouveau fichier sélectionné, annule la demande de suppression
    const reader = new FileReader();
    reader.onload = () => { this.selectedLogoPreviewUrl = reader.result as string; };
    reader.readAsDataURL(file);
    input.value = '';
  }

  clearLogoSelection(): void {
    this.selectedLogoFile = null;
    this.selectedLogoPreviewUrl = null;
    this.deleteLogoRequested = false;
  }

  /** Appelé quand l'utilisateur clique sur Supprimer : efface la sélection et demande la suppression côté backend */
  onDeleteLogoClick(): void {
    this.selectedLogoFile = null;
    this.selectedLogoPreviewUrl = null;
    this.deleteLogoRequested = true;
  }

  onSubmit() {
    if (this.companyForm.valid) {
      this.submit.emit({
        formData: this.companyForm.value,
        logo: this.selectedLogoFile ?? undefined,
        deleteLogo: this.deleteLogoRequested
      });
    } else {
      // Marquer tous les champs comme touchés pour afficher les erreurs
      Object.keys(this.companyForm.controls).forEach(key => {
        this.companyForm.get(key)?.markAsTouched();
      });
    }
  }
}