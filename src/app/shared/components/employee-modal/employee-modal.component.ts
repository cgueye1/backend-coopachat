import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { ModalComponent } from '../../ui/modal/modal.component';
import { FormFieldComponent } from '../../ui/form-field/form-field.component';

export interface EmployeeFormData {
  prenom: string;
  nom: string;
  email: string;
  telephone: string;
  adresse: string;
  entreprise: string;
}

export interface Company {
  id: string;
  name: string;
}

@Component({
  selector: 'app-employee-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    ModalComponent,
    FormFieldComponent
  ],
  templateUrl: './employee-modal.component.html'
})
export class EmployeeModalComponent implements OnInit, OnChanges {
  // Ouvrir / fermer la modale
  @Input() isOpen = false;
  // Bloquer le bouton quand on envoie la requete
  @Input() isSubmitting = false;
  // Titre dynamique (creation / modification)
  @Input() title = "Inscrire un nouveau salarié";
  // Liste des entreprises pour le select
  @Input() companies: Company[] = [];
  // Donnees pour pre-remplir le formulaire en modification
  @Input() initialData: EmployeeFormData | null = null;
  // Fermeture demandee par le parent
  @Output() close = new EventEmitter<void>();
  // Envoi du formulaire vers le parent
  @Output() submit = new EventEmitter<EmployeeFormData>();

  employeeForm!: FormGroup;

  /** Autocomplete entreprise */
  companySearchTerm = '';
  showCompanyDropdown = false;

  constructor(private fb: FormBuilder) { }

  get filteredCompanies(): Company[] {
    const term = (this.companySearchTerm || '').trim().toLowerCase();
    return (this.companies || []).filter(c =>
      term === '' || (c.name || '').toLowerCase().includes(term)
    );
  }

  ngOnInit() {
    // Initialiser le formulaire
    this.initForm();
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['initialData'] && this.employeeForm) {
      if (this.initialData) {
        this.employeeForm.patchValue(this.initialData);
        this.syncCompanySearchFromForm();
      } else {
        this.employeeForm.reset();
        this.companySearchTerm = '';
      }
    }
    if (changes['companies'] && this.initialData?.entreprise) {
      this.syncCompanySearchFromForm();
    }
  }

  private syncCompanySearchFromForm(): void {
    const entrepriseId = this.employeeForm.get('entreprise')?.value;
    if (entrepriseId && this.companies?.length) {
      const company = this.companies.find(c => String(c.id) === String(entrepriseId));
      this.companySearchTerm = company?.name ?? '';
    } else {
      this.companySearchTerm = '';
    }
  }

  @HostListener('document:click')
  onDocumentClick(): void {
    this.showCompanyDropdown = false;
  }

  onCompanySearchInput(): void {
    this.showCompanyDropdown = true;
    const currentId = this.employeeForm.get('entreprise')?.value;
    if (currentId && this.companies?.length) {
      const selected = this.companies.find(c => String(c.id) === String(currentId));
      if (selected && this.companySearchTerm.trim() !== selected.name) {
        this.employeeForm.get('entreprise')?.setValue('');
      }
    }
  }

  selectCompany(company: Company): void {
    this.employeeForm.get('entreprise')?.setValue(company.id);
    this.companySearchTerm = company.name;
    this.showCompanyDropdown = false;
  }

  private initForm() {
    // Definir les champs et validations
    this.employeeForm = this.fb.group({
      prenom: ['', [Validators.required, Validators.minLength(2)]],
      nom: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      telephone: ['', [Validators.required, Validators.pattern(/^[+]?[0-9\s\-\(\)]+$/)]],
      adresse: ['', [Validators.required, Validators.minLength(5)]],
      entreprise: ['', [Validators.required]]
    });
  }

  getFieldError(fieldName: string): string {
    const field = this.employeeForm.get(fieldName);
    if (field?.invalid && field?.touched) {
      if (field.errors?.['required']) {
        return 'Ce champ est requis';
      }
      if (field.errors?.['minlength']) {
        return `Minimum ${field.errors?.['minlength'].requiredLength} caractères`;
      }
      if (field.errors?.['email']) {
        return 'Format d\'email invalide';
      }
      if (field.errors?.['pattern'] && fieldName === 'telephone') {
        return 'Format de téléphone invalide';
      }
    }
    return '';
  }

  onClose() {
    this.employeeForm.reset();
    this.companySearchTerm = '';
    this.showCompanyDropdown = false;
    this.close.emit();
  }

  onSubmit() {
    // Si le formulaire est valide, on envoie les donnees
    if (this.employeeForm.valid) {
      this.submit.emit(this.employeeForm.value);
    } else {
      // Marquer les champs pour afficher les erreurs
      Object.keys(this.employeeForm.controls).forEach(key => {
        this.employeeForm.get(key)?.markAsTouched();
      });
    }
  }
}