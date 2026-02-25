import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HeaderComponent } from '../../../core/layouts/header/header.component';
import { MainLayoutComponent } from '../../../core/layouts/main-layout/main-layout.component';
import { CompanyModalComponent, CompanyFormData } from '../../../shared/components/company-modal/company-modal.component';
import { CommercialService } from '../../../shared/services/commercial.service';
import Swal from 'sweetalert2';

// Interface pour les donnees entreprises
interface Prospect {
  id: string;
  companyCode?: string;
  entreprise: string;
  secteur: string;
  note?: string;
  localisation: string;
  contact: {
    nom: string;
    email?: string;
    telephone: string;
  };
  statut: 'Actif' | 'Inactif';
  date: string;
  initials: string;
}

// Interface pour les cartes statistiques
interface MetricCard {
  title: string;
  value: string;
  icon: string;
}

@Component({
  selector: 'app-prospection',
  standalone: true,
  imports: [CommonModule, FormsModule, MainLayoutComponent, CompanyModalComponent, HeaderComponent],
  templateUrl: `./propection.component.html`,
  styles: [`
    /* Custom styles for better visual consistency */
    .table-row:hover {
      background-color: #f9fafb;
    }

    /* Focus styles for accessibility */
    input:focus, select:focus, button:focus {
      outline: 2px solid transparent;
      outline-offset: 2px;
    }

    /* Smooth transitions */
    * {
      transition-property: background-color, border-color, color, fill, stroke;
      transition-timing-function: cubic-bezier(0.4, 0, 0.2, 1);
      transition-duration: 200ms;
    }

    /* Scrollbar styles (applied to elements with overflow-x-auto) */
    .overflow-x-auto {
      scrollbar-width: thin;
      scrollbar-color: #cbd5e1 #f8fafc;
      position: relative;
    }

    .overflow-x-auto::-webkit-scrollbar {
      height: 8px;
    }

    .overflow-x-auto::-webkit-scrollbar-track {
      background: #f8fafc;
      border-radius: 4px;
    }

    .overflow-x-auto::-webkit-scrollbar-thumb {
      background: #cbd5e1;
      border-radius: 4px;
    }

    .overflow-x-auto::-webkit-scrollbar-thumb:hover {
      background: #94a3b8;
    }

    @media (max-width: 1024px) {
      .grid-cols-12 { grid-template-columns: repeat(12, minmax(0, 1fr)); }
    }

    @media (max-width: 640px) {
      button, .px-4, .py-2 {
        min-height: 40px;
      }
    }

    /* Ensure cards and table cells wrap text properly */
    .whitespace-nowrap { white-space: nowrap; }
  `]
})
export class ProspectionComponent implements OnInit {
  // ==================================================
  // SECTION 1 : VARIABLES GLOBALES ET INITIALISATION
  // ==================================================

  // Texte tape dans la recherche
  searchTerm = '';

  // Statut choisi dans le filtre
  selectedStatus = '';

  // Secteur choisi dans le filtre
  selectedSector = '';

  // Page actuelle dans la pagination
  currentPage = 1;

  // Nombre d'elements par page
  itemsPerPage = 6;

  // Ouvre ou ferme le menu des statuts
  showStatusDropdown = false;

  // Ouvre ou ferme le menu des secteurs
  showSectorDropdown = false;

  // Ouvre ou ferme la modale des details
  showProspectModal = false;

  // Prospect selectionné pour les details
  selectedProspect: Prospect | null = null;
  private successPopupTimeoutId: ReturnType<typeof setTimeout> | null = null;
  private submitSequence = 0;

  // Ouvre ou ferme la modale de creation / modification
  isCompanyModalOpen = false;

  // Id de l'entreprise en modification
  editingCompanyId: string | null = null;

  // Donnees pre-remplies du formulaire
  editingCompanyData: CompanyFormData | null = null;

  // Liste des statuts visibles dans le filtre
  uniqueStatuses = ['Tous les statuts', 'Actif', 'Inactif'];

  // Liste des secteurs visibles dans le filtre
  uniqueSectors = [
    'Tous les secteurs',
    'Technologie',
    'Finance',
    'Santé',
    'Éducation',
    'Commerce de détail',
    'Industrie manufacturière',
    'BTP / Construction',
    'Transport',
    'Hôtellerie / Restauration',
    'Énergie',
    'Télécommunications',
    'Agriculture',
    'Agroalimentaire',
    'Pharmaceutique',
    'Automobile',
    'Textile',
    'Conseil',
    'Immobilier',
    'Médias',
    'Secteur public',
    'Association / ONG',
    'Autre'
  ];

  // Données des cartes statistiques
  metricsData: MetricCard[] = [];

  // Liste principale des prospects
  prospects: Prospect[] = [];

  // Liste filtree (affichage)
  filteredProspects: Prospect[] = [];

  // Total renvoye par l'API
  totalElements = 0;

  // Service API injecte
  constructor(private commercialService: CommercialService) {}


  // ==================================================
  // SECTION 2 : METHODES PRINCIPALES (CRUD + ACTIONS)
  // ==================================================

  /** ==================================================
   *  METHODE : OUVRIR LA MODALE DE CREATION
   *  ================================================== */
  /**
   * But : Ouvrir le formulaire vide pour creer une entreprise
   * Resultat attendu : La modale s'affiche
   * Etapes :
   * 1. Vider l'id en cours
   * 2. Vider les donnees du formulaire
   * 3. Ouvrir la modale
   */
  openCreateModal(): void {
    // Effacer l'id de modification
    this.editingCompanyId = null;

    // Effacer les donnees pre-remplies
    this.editingCompanyData = null;

    // Ouvrir la modale
    this.isCompanyModalOpen = true;
  }


  /** ==================================================
   *  METHODE : FERMER LA MODALE DE CREATION / MODIF
   *  ================================================== */
  /**
   * But : Fermer la modale et nettoyer les donnees
   * Resultat attendu : La modale se ferme proprement
   * Etapes :
   * 1. Fermer la modale
   * 2. Vider l'id
   * 3. Vider les donnees
   */
  onCloseCompanyModal(): void {
    // Fermer la modale
    this.isCompanyModalOpen = false;

    // Vider l'id en cours
    this.editingCompanyId = null;

    // Vider les donnees pre-remplies
    this.editingCompanyData = null;
  }


  /** ==================================================
   *  METHODE : CHARGER LA LISTE DES ENTREPRISES
   *  ================================================== */
  /**
   * But : Recuperer la liste depuis l'API
   * Resultat attendu : Le tableau est mis a jour
   * Etapes :
   * 1. Appeler l'API avec les filtres
   * 2. Mapper la reponse
   * 3. Mettre a jour la pagination
   */
  loadCompanies(): void {
    // Appel API avec les filtres
    this.commercialService
      .getCompanies(
        this.currentPage - 1,
        this.itemsPerPage,
        this.searchTerm,
        this.mapSectorLabelToEnum(this.selectedSector),
        this.getIsActiveFilter()
      )
      .subscribe({
        next: (response) => {
          // Recuperer la liste depuis la reponse
          const companies = response?.content ?? [];

          // Convertir chaque element en Prospect
          this.prospects = companies.map((company: any) => this.mapCompanyToProspect(company));

          // Copier dans la liste filtree
          this.filteredProspects = [...this.prospects];

          // Mettre a jour le total
          this.totalElements = response?.totalElements ?? this.prospects.length;
        },
        error: (error) => {
          // Afficher l'erreur en console
          console.error('Erreur lors du chargement des entreprises:', error);
        }
      });
  }


  /** ==================================================
   *  METHODE : MODIFIER UNE ENTREPRISE
   *  ================================================== */
  /**
   * But : Recuperer les details pour pre-remplir le formulaire
   * Resultat attendu : La modale s'ouvre avec les données
   * Etapes :
   * 1. Appeler l'API details
   * 2. Transformer la reponse
   * 3. Ouvrir la modale
   */
  editProspect(id: string): void {
    // Appeler l'API pour les details
    this.commercialService.getCompanyDetails(id).subscribe({
      next: (details) => {
        // Stocker l'id en cours
        this.editingCompanyId = id;

        // Preparer les donnees du formulaire
        this.editingCompanyData = this.mapCompanyDetailsToFormData(details);

        // Ouvrir la modale
        this.isCompanyModalOpen = true;
      },
      error: (error) => {
        // Afficher l'erreur en console
        console.error('Erreur lors du chargement pour modification:', error);
      }
    });
  }


  /** ==================================================
   *  METHODE : VOIR LES DETAILS D'UNE ENTREPRISE
   *  ================================================== */
  /**
   * But : Afficher la fiche détaillée dans une modale
   * Resultat attendu : La modale de details s'ouvre
   * Etapes :
   * 1. Appeler l'API details
   * 2. Mapper la reponse
   * 3. Ouvrir la modale
   */
  viewDetails(prospect: Prospect): void {
    // Appeler l'API des details
    this.commercialService.getCompanyDetails(prospect.id).subscribe({
      next: (details) => {
        // Mapper les details
        this.selectedProspect = this.mapCompanyDetailsToProspect(details);

        // Ouvrir la modale
        this.showProspectModal = true;
      },
      error: (error) => {
        // Afficher l'erreur en console
        console.error('Erreur lors du chargement des détails:', error);

        // Fallback : afficher les infos deja connues
        this.selectedProspect = prospect;

        // Ouvrir quand meme la modale
        this.showProspectModal = true;
      }
    });
  }


  /** ==================================================
   *  METHODE : FERMER LA MODALE DE DETAILS
   *  ================================================== */
  /**
   * But : Fermer la modale et vider la selection
   * Resultat attendu : La modale se ferme proprement
   * Etapes :
   * 1. Fermer la modale
   * 2. Vider la selection
   */
  closeProspectModal(): void {
    // Fermer la modale
    this.showProspectModal = false;

    // Vider la selection
    this.selectedProspect = null;
  }

  /** ==================================================
   *  METHODE : BOUTON "MODIFIER" DANS LA MODALE DETAILS
   *  ================================================== */
  /**
   * But : Ouvrir la modale d'edition depuis la fiche details
   * Resultat attendu : Le formulaire s'ouvre avec les donnees
   */
  modifierProspect(): void {
    if (!this.selectedProspect) return;

    // Fermer la modale de details
    this.closeProspectModal();

    // Ouvrir la modale d'edition
    this.editProspect(this.selectedProspect.id);
  }

  /** ==================================================
   *  METHODE : BOUTON "ANNULER" DANS LA MODALE DETAILS
   *  ================================================== */
  /**
   * But : Fermer la modale sans action
   */
  annulerProspect(): void {
    this.closeProspectModal();
  }


  /** ==================================================
   *  METHODE : CHANGER LE STATUT D'UNE ENTREPRISE
   *  ================================================== */
  /**
   * But : Activer ou desactiver une entreprise
   * Resultat attendu : Le statut change dans la liste
   * Etapes :
   * 1. Calculer le nouveau statut
   * 2. Appeler l'API
   * 3. Mettre a jour l'affichage
   */
  toggleProspectStatus(prospect: Prospect): void {
    // Calculer le futur statut (nextIsActive devient un booléen (true ou false) qui indique le statut que le prospect aura après le toggle)
    const nextIsActive = prospect.statut !== 'Actif';

    // Appeler l'API de mise a jour
    this.commercialService.updateCompanyStatus(prospect.id, nextIsActive).subscribe({
      next: () => {
        // Mettre a jour le statut local
        prospect.statut = nextIsActive ? 'Actif' : 'Inactif';

        // Recharger la liste pour refléter les changements
        this.loadCompanies();
      },
      error: (error) => {
        // Afficher l'erreur en console
        console.error('Erreur lors de la mise à jour du statut:', error);
      }
    });
  }


  /** ==================================================
   *  METHODE : CREER / MODIFIER UNE ENTREPRISE
   *  ================================================== */
  /**
   * But : Envoyer le formulaire au backend
   * Resultat attendu : La liste est rafraichie
   * Etapes :
   * 1. Construire le payload
   * 2. Choisir create ou update
   * 3. Afficher un message
   */
  onCompanySubmit(data: CompanyFormData): void {
    // Numero d'envoi pour eviter les popups en double
    const submitId = ++this.submitSequence;
    const isEditMode = !!this.editingCompanyId;

    // Fermer un popup deja ouvert
    Swal.close();

    // Annuler un ancien popup succes encore en attente
    if (this.successPopupTimeoutId) {
      clearTimeout(this.successPopupTimeoutId);
      this.successPopupTimeoutId = null;
    }

    // Stopper si des champs obligatoires sont vides
    // Reprendre les valeurs existantes si certains champs arrivent vides
    const fallbackData = this.editingCompanyData;
    const resolveValue = (value?: string, fallback?: string): string => {
      const trimmed = value?.trim();
      if (trimmed) return trimmed;
      return fallback?.trim() ?? '';
    };

    const resolvedNom = resolveValue(data.nom, fallbackData?.nom);
    const resolvedLocalisation = resolveValue(data.localisation, fallbackData?.localisation);
    const resolvedContact = resolveValue(data.contact, fallbackData?.contact);
    const resolvedTelephone = resolveValue(data.telephone, fallbackData?.telephone);
    const resolvedStatut = resolveValue(data.statut, fallbackData?.statut);
    const resolvedSecteur = resolveValue(data.secteur, fallbackData?.secteur);
    const resolvedEmail = resolveValue(data.email, fallbackData?.email);
    const resolvedNote = resolveValue(data.note, fallbackData?.note);

    // Construire le payload a envoyer
    const payload = {
      name: resolvedNom,
      sector: resolvedSecteur || undefined,
      location: resolvedLocalisation,
      contactName: resolvedContact,
      contactEmail: resolvedEmail || undefined,
      contactPhone: resolvedTelephone,
      status: resolvedStatut,
      note: resolvedNote
    };

    // Choisir la bonne requête (editingCompanyId sert à savoir si on est en mode ajout ou modification :)
    const request$ = this.editingCompanyId
      ? this.commercialService.updateCompany(this.editingCompanyId, payload)
      : this.commercialService.createCompany(payload);

    // Exécuter la requete
    request$.subscribe({
      next: () => {
        // Verifier si on est en mode modification
        const isEdit = !!this.editingCompanyId;// convertit editingCompanyId en boolean (true si non null, false si null)

        // Fermer la modale
        this.isCompanyModalOpen = false;

        // Nettoyer l'etat
        this.editingCompanyId = null;
        this.editingCompanyData = null;

        // Recharger la liste
        this.loadCompanies();

        // Afficher un message de succes apres un petit delai
        this.successPopupTimeoutId = setTimeout(() => {
          // Ne pas afficher si un autre envoi est arrive apres
          if (submitId !== this.submitSequence) {
            return;
          }
          Swal.fire({
            icon: 'success',
            iconHtml: '<img src="/icones/message success.svg" style="width: 95px; height: 95px; margin: 0 auto;" />',
            title: isEdit ? 'Entreprise modifiée avec succès' : 'Entreprise créée avec succès',
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
          this.successPopupTimeoutId = null;
        }, 200);
      },
      error: (error) => {
        // Annuler un popup succes en attente si la requete echoue
        if (this.successPopupTimeoutId) {
          clearTimeout(this.successPopupTimeoutId);
          this.successPopupTimeoutId = null;
        }
        // Fermer un popup existant avant d'afficher l'erreur
        Swal.close();

        // Recuperer le message d'erreur
        const rawMessage = error?.error?.message || 'Erreur lors de la création';

        // Message simple si doublon
        const message = rawMessage.includes('Duplicata')
          ? 'Cet email de contact existe déjà. Utilise un autre email.'
          : rawMessage;

        // Afficher un message d'erreur
        Swal.fire({
          icon: 'error',
          title: isEditMode ? 'Modification échouée' : 'Création échouée',
          text: message,
          confirmButtonText: 'OK'
        });
      }
    });
  }


  // ==================================================
  // SECTION 3 : METHODES UTILITAIRES (HELPERS)
  // ==================================================

  /** ==================================================
   *  METHODE : CALCULER LE TOTAL
   *  ================================================== */
  /**
   * But : Donner le total pour la pagination
   * Cette méthode sert a : totalPages, getDisplayStart, getDisplayEnd
   */
  get totalResults(): number {
    // Retourner le total correct ou la longueur filtrée
    return this.totalElements > 0 ? this.totalElements : this.filteredProspects.length;
  }


  /** ==================================================
   *  METHODE : CALCULER LE NOMBRE DE PAGES
   *  ================================================== */
  /**
   * But : Calculer le nombre total de pages
   * Cette methode sert a : pagination
   */
  get totalPages(): number {
    // Calculer et forcer au moins 1 page
    return Math.max(1, Math.ceil(this.totalResults / this.itemsPerPage));
  }


  /** ==================================================
   *  METHODE : PAGE COURANTE
   *  ================================================== */
  /**
   * But : Retourner la page a afficher
   * Cette methode sert a : affichage du tableau
   */
  getCurrentPageProspects(): Prospect[] {
    // Les données sont déjà paginées par le backend
    return this.filteredProspects;
  }


  /** ==================================================
   *  METHODE : TRACK BY
   *  ================================================== */
  /**
   * But : Optimiser l'affichage Angular
   * Cette methode sert a : *ngFor du tableau
   */
  trackByProspect(index: number, prospect: Prospect): string {
    // Retourner l'id unique
    return prospect.id;
  }


  /** ==================================================
   *  METHODE : CLASS DU STATUT
   *  ================================================== */
  /**
   * But : Donner la couleur du statut
   * Cette méthode sert a : affichage du tableau
   */
  getStatusClass(status: string): string {
    // Retourner la classe selon le statut
    return status === 'Actif'
      ? 'bg-[#0A97480F] text-[#0A9748]'
      : 'bg-red-50 text-[#FF0909]';
  }


  /** ==================================================
   *  METHODE : POINT DU STATUT
   *  ================================================== */
  /**
   * But : Donner la couleur du point de statut
   * Cette methode sert a : affichage du tableau
   */
  getStatusDotClass(status: string): string {
    // Retourner la classe selon le statut
    return status === 'Actif' ? 'bg-[#0A9748]' : 'bg-[#FF0909]';
  }


  /** ==================================================
   *  METHODE : PREMIER ELEMENT A AFFICHER
   *  ================================================== */
  /**
   * But : Calculer le debut de la page
   * Cette methode sert a : affichage de la pagination
   */
  getDisplayStart(): number {
    // Si aucun resultat, retourner 0
    if (this.totalResults === 0) return 0;

    // Calculer l'index de debut
    return (this.currentPage - 1) * this.itemsPerPage + 1;
  }


  /** ==================================================
   *  METHODE : DERNIER ELEMENT A AFFICHER
   *  ================================================== */
  /**
   * But : Calculer la fin de la page
   * Cette methode sert a : affichage de la pagination
   */
  getDisplayEnd(): number {
    // Calculer l'index de fin
    return Math.min(this.currentPage * this.itemsPerPage, this.totalResults);
  }


  /** ==================================================
   *  METHODE : LISTE DES PAGES
   *  ================================================== */
  /**
   * But : Construire les numéros de pages visibles
   * Cette méthode sert a : pagination
   */
  getPageNumbers(): number[] {
    // Créer un tableau vide
    const pages = [];

    // Nombre max de pages visibles
    const maxVisiblePages = 5;

    // Calculer la page de depart
    let startPage = Math.max(1, this.currentPage - Math.floor(maxVisiblePages / 2));

    // Calculer la page de fin
    let endPage = Math.min(this.totalPages, startPage + maxVisiblePages - 1);

    // Ajuster si on manque de pages
    if (endPage - startPage + 1 < maxVisiblePages) {
      startPage = Math.max(1, endPage - maxVisiblePages + 1);
    }

    // Remplir la liste
    for (let i = startPage; i <= endPage; i++) {
      pages.push(i);
    }

    // Retourner les pages
    return pages;
  }


  /** ==================================================
   *  METHODE : FILTRE ACTIF / INACTIF
   *  ================================================== */
  /**
   * But : Convertir le filtre en boolean
   * Cette methode sert a : loadCompanies
   */
  private getIsActiveFilter(): boolean | undefined {
    // Actif = true
    if (this.selectedStatus === 'Actif') return true;

    // Inactif = false
    if (this.selectedStatus === 'Inactif') return false;

    // Aucun filtre
    return undefined;
  }


  /** ==================================================
   *  METHODE : MAPPER UN ELEMENT API
   *  ================================================== */
  /**
   * But : Convertir la reponse API en Prospect
   * Cette methode sert a : loadCompanies
   */
  private mapCompanyToProspect(company: any): Prospect {
    // Recupérer le nom
    const name = company?.name ?? '';

    // Calculer les initiales
    const initials = name
      .split(' ')
      .filter((word: string) => word)
      .map((word: string) => word[0])
      .join('')
      .substring(0, 2)
      .toUpperCase();

    // Normaliser le statut
    const statusLabel = this.normalizeStatus(company?.status);

    // Retourner l'objet Prospect
    return {
      id: company?.id?.toString() ?? initials,
      entreprise: name,
      secteur: company?.sector ?? 'Non défini',
      localisation: company?.location ?? '',
      contact: {
        nom: company?.contactName ?? '',
        email: company?.contactEmail ?? '',
        telephone: company?.contactPhone ?? ''
      },
      statut: statusLabel,
      date: this.formatCreatedAt(company?.createdAt),
      initials
    };
  }


  /** ==================================================
   *  METHODE : NORMALISER LE STATUT
   *  ================================================== */
  /**
   * But : Mettre le statut au format Actif/Inactif
   * Cette methode sert a : mapCompanyToProspect, mapCompanyDetailsToProspect
   */
  private normalizeStatus(status: string | undefined): 'Actif' | 'Inactif' {
    // Si vide, on considere Actif
    if (!status) return 'Actif';

    // Mettre en minuscule
    const normalized = status.toLowerCase();

    // Detecter les valeurs inactives
    if (normalized.includes('inactif') || normalized === 'inactive' || normalized === 'false') {
      return 'Inactif';
    }

    // Sinon, Actif
    return 'Actif';
  }


  /** ==================================================
   *  METHODE : FORMATTER LA DATE
   *  ================================================== */
  /**
   * But : Garder seulement la date sans l'heure
   * Cette methode sert a : mapCompanyToProspect, mapCompanyDetailsToProspect
   */
  private formatCreatedAt(createdAt: string | undefined): string {
    // Si vide, retourner chaine vide
    if (!createdAt) return '';

    // Couper la date
    const parts = createdAt.split(' ');
    return parts[0] ?? createdAt;
  }


  /** ==================================================
   *  METHODE : DETAILS -> PROSPECT
   *  ================================================== */
  /**
   * But : Convertir les details API en Prospect
   * Cette methode sert a : viewDetails
   */
  private mapCompanyDetailsToProspect(details: any): Prospect {
    // Recuperer le nom
    const name = details?.name ?? '';

    // Calculer les initiales
    const initials = name
      .split(' ')
      .filter((word: string) => word)
      .map((word: string) => word[0])
      .join('')
      .substring(0, 2)
      .toUpperCase();

    // Retourner l'objet Prospect
    return {
      id: details?.id?.toString() ?? initials,
      companyCode: details?.companyCode ?? '',
      entreprise: name,
      secteur: details?.sector ?? 'Non défini',
      note: details?.note ?? '',
      localisation: details?.location ?? '',
      contact: {
        nom: details?.contactName ?? '',
        email: details?.contactEmail ?? '',
        telephone: details?.contactPhone ?? ''
      },
      statut: this.normalizeStatus(details?.status),
      date: this.formatCreatedAt(details?.createdAt),
      initials
    };
  }


  /** ==================================================
   *  METHODE : DETAILS -> FORMULAIRE
   *  ================================================== */
  /**
   * But : Pre-remplir le formulaire d'edition
   * Cette methode sert a : editProspect
   */
  private mapCompanyDetailsToFormData(details: any): CompanyFormData {
    // Retourner les donnees du formulaire
    const statusLabel = this.normalizeStatusForForm(details?.status);
    return {
      nom: details?.name ?? '',
      secteur: details?.sector ?? 'Autre',
      localisation: details?.location ?? '',
      statut: statusLabel,
      contact: details?.contactName ?? '',
      email: details?.contactEmail ?? '',
      telephone: details?.contactPhone ?? '',
      note: details?.note ?? ''
    };
  }


  // ==================================================
  // SECTION 4 : GESTION DES EVENEMENTS
  // ==================================================

  /** ==================================================
   *  METHODE : OUVRIR / FERMER LE FILTRE STATUT
   *  ================================================== */
  /**
   * But : Afficher ou cacher le menu statut
   * Resultat attendu : Le menu apparait ou disparait
   */
  toggleStatusDropdown(): void {
    // Inverser l'etat
    this.showStatusDropdown = !this.showStatusDropdown;

    // Fermer l'autre menu
    this.showSectorDropdown = false;
  }


  /** ==================================================
   *  METHODE : OUVRIR / FERMER LE FILTRE SECTEUR
   *  ================================================== */
  /**
   * But : Afficher ou cacher le menu secteur
   * Resultat attendu : Le menu apparait ou disparait
   */
  toggleSectorDropdown(): void {
    // Inverser l'etat
    this.showSectorDropdown = !this.showSectorDropdown;

    // Fermer l'autre menu
    this.showStatusDropdown = false;
  }


  /** ==================================================
   *  METHODE : CHOISIR UN STATUT
   *  ================================================== */
  /**
   * But : Appliquer le filtre statut
   * Resultat attendu : La liste est rechargée
   */
  selectStatus(status: string): void {
    // Sauvegarder le statut
    this.selectedStatus = status === 'Tous les statuts' ? '' : status;

    // Fermer le menu
    this.showStatusDropdown = false;

    // Revenir à la première page
    this.currentPage = 1;

    // Recharger la liste
    this.loadCompanies();
  }


  /** ==================================================
   *  METHODE : CHOISIR UN SECTEUR
   *  ================================================== */
  /**
   * But : Appliquer le filtre secteur
   * Resultat attendu : La liste est rechargée
   */
  selectSector(sector: string): void {
    // Sauvegarder le secteur
    this.selectedSector = sector === 'Tous les secteurs' ? '' : sector;

    // Fermer le menu
    this.showSectorDropdown = false;

    // Revenir à la première page
    this.currentPage = 1;

    // Recharger la liste
    this.loadCompanies();
  }


  /** ==================================================
   *  METHODE : RECHERCHE AU CLAVIER
   *  ================================================== */
  /**
   * But : Lancer la recherche a chaque frappe
   * Resultat attendu : La liste est rechargee
   */
  onSearch(): void {
    // Revenir à la première page
    this.currentPage = 1;

    // Recharger la liste
    this.loadCompanies();
  }

  /** ==================================================
   *  METHODE : CONVERTIR LE SECTEUR (FILTRE)
   *  ================================================== */
  /**
   * But : Envoyer le code enum attendu par l'API pour le filtre
   * Cette methode sert a : loadCompanies (query param sector)
   */
  private mapSectorLabelToEnum(label: string): string | undefined {
    if (!label || label === 'Tous les secteurs') return undefined;
    const mapping: Record<string, string> = {
      'Technologie': 'TECHNOLOGY',
      'Finance': 'FINANCE',
      'Santé': 'HEALTHCARE',
      'Éducation': 'EDUCATION',
      'Commerce de détail': 'RETAIL',
      'Industrie manufacturière': 'MANUFACTURING',
      'BTP / Construction': 'CONSTRUCTION',
      'Transport': 'TRANSPORTATION',
      'Hôtellerie / Restauration': 'HOSPITALITY',
      'Énergie': 'ENERGY',
      'Télécommunications': 'TELECOMMUNICATIONS',
      'Agriculture': 'AGRICULTURE',
      'Agroalimentaire': 'FOOD_AND_BEVERAGE',
      'Pharmaceutique': 'PHARMACEUTICAL',
      'Automobile': 'AUTOMOTIVE',
      'Textile': 'TEXTILE',
      'Conseil': 'CONSULTING',
      'Immobilier': 'REAL_ESTATE',
      'Médias': 'MEDIA',
      'Secteur public': 'GOVERNMENT',
      'Association / ONG': 'NON_PROFIT',
      'Autre': 'OTHER'
    };

    return mapping[label];
  }


  /** ==================================================
   *  METHODE : NORMALISER LE STATUT POUR LE FORMULAIRE
   *  ================================================== */
  /**
   * But : Adapter la valeur du backend aux options du select
   * Cette methode sert a : mapCompanyDetailsToFormData
   */
  private normalizeStatusForForm(rawStatus: string | undefined): string {
    // Liste des statuts attendus par le formulaire
    const allowed = [
      'En attente',
      'Relancée',
      'Intéressée',
      'Refusée',
      'Partenaire signé'
    ];

    // Si la valeur est deja correcte, on la garde
    if (rawStatus && allowed.includes(rawStatus)) {
      return rawStatus;
    }

    // Si le backend envoie des codes, on les convertit
    const mapping: Record<string, string> = {
      PENDING: 'En attente',
      RELAUNCHED: 'Relancée',
      INTERESTED: 'Intéressée',
      REFUSED: 'Refusée',
      PARTNER_SIGNED: 'Partenaire signé'
    };

    // Retourner la version compatible ou un statut par defaut
    return mapping[rawStatus ?? ''] || 'En attente';
  }


  /** ==================================================
   *  METHODE : PAGE PRECEDENTE
   *  ================================================== */
  /**
   * But : Reculer dans la pagination
   * Resultat attendu : La page diminue
   */
  previousPage(): void {
    // Verifier la limite
    if (this.currentPage > 1) {
      // Changer la page
      this.currentPage--;

      // Recharger la liste
      this.loadCompanies();
    }
  }


  /** ==================================================
   *  METHODE : PAGE SUIVANTE
   *  ================================================== */
  /**
   * But : Avancer dans la pagination
   * Resultat attendu : La page augmente
   */
  nextPage(): void {
    // Verifier la limite
    if (this.currentPage < this.totalPages) {
      // Changer la page
      this.currentPage++;

      // Recharger la liste
      this.loadCompanies();
    }
  }


  /** ==================================================
   *  METHODE : ALLER A UNE PAGE
   *  ================================================== */
  /**
   * But : Aller a une page precise
   * Resultat attendu : La page change
   */
  goToPage(page: number): void {
    // Vérifier la limite
    if (page >= 1 && page <= this.totalPages) {
      // Changer la page
      this.currentPage = page;

      // Recharger la liste
      this.loadCompanies();
    }
  }

  /** ==================================================
   *  METHODE : INITIALISER LE COMPOSANT
   *  ================================================== */
  ngOnInit(): void {
    // Charger la liste au demarrage
    this.loadCompanies();
    // Charger les statistiques au demarrage
    this.loadCompanyStats();
  }

  /** ==================================================
   *  METHODE : CHARGER LES STATISTIQUES
   *  ================================================== */
  /**
   * But : Recuperer les compteurs (total, actif, inactif)
   * Resultat attendu : Les cartes du haut se remplissent
   */
  loadCompanyStats(): void {
    this.commercialService.getCompanyStats().subscribe({
      next: (stats) => {
        this.metricsData = [
          {
            title: 'Total entreprises',
            value: String(stats?.totalCompanies ?? 0),
            icon: '/icones/entreprise.svg'
          },
          {
            title: 'Entreprises actives',
            value: String(stats?.activeCompanies ?? 0),
            icon: '/icones/actif.svg'
          },
          {
            title: 'Entreprises inactives',
            value: String(stats?.inactiveCompanies ?? 0),
            icon: '/icones/inactif.svg'
          }
        ];
      },
      error: (error) => {
        console.error('Erreur lors du chargement des statistiques:', error);
        this.metricsData = [];
      }
    });
  }
}

