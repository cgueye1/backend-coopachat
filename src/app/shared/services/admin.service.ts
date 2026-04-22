import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';


/**
 * Un élément "Paiements par statut" renvoyé par l'API (ex. Payé: 10, Échoué: 1).
 */
export interface PaymentStatusItemDTO {
  statusLabel: string;
  count: number;
}

/**
 * Réponse de GET /api/admin/dashboard/stats (données globales, sans filtre de période).
 * Les 3 KPIs + la liste pour le graphique "Paiements par statut".
 */
export interface AdminDashboardStatsDTO {
  commandesEnAttente: number;
  paiementsEchoues: number;
  reclamationsOuvertes: number;
  paiementsParStatut: PaymentStatusItemDTO[];
}

/** Un jour du graphique "Tendance des coupons utilisés" (7 derniers jours). */
export interface CouponUsageParJourDTO {
  date: string;
  nbUtilisations: number;
}

/** Une alerte du tableau de bord admin (GET /api/admin/alerts). */
export interface AlertItemDTO {
  type: string;
  message: string;
  detail: string;
  module: string;
  date: string;
}

export interface AdminAlertsDTO {
  alerts: AlertItemDTO[];
}

/**
 * Un jour du graphique « Livraisons » (7 derniers jours) : prévu à la date, livré à la date prévue, retard.
 * GET /admin/dashboard/livraisons-par-jour — et GET .../commandes-vs-livraisons (même JSON, alias).
 */
export interface LivraisonParJourDTO {
  date: string;
  nbPrevues: number;
  nbLivreesALaDate: number;
  nbRetard: number;
}

/** Un rôle du graphique "Utilisateurs par rôle" (API GET /admin/users/stats/by-role). */
export interface UserStatsByRoleItemDTO {
  role: string;
  roleLabel: string;
  count: number;
  percentage: number;
}

// --- Page Gestion des utilisateurs (GET/POST /admin/users, stats, etc.) ---

export interface UserListItemDTO {
  id: number;
  reference: string;
  firstName: string;
  lastName: string;
  email: string;
  roleLabel: string;
  profilePhotoUrl?: string | null;
  createdAt: string;
  isActive: boolean;
}

export interface UserListResponseDTO {
  content: UserListItemDTO[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
  pageSize: number;
  hasNext: boolean;
  hasPrevious: boolean;
}

export interface UserStatsDTO {
  totalUsers: number;
  activeUsers: number;
  inactiveUsers: number;
}

export interface UserStatsByStatusItemDTO {
  label: string;
  count: number;
  percentage: number;
}

export interface UserDetailsDTO {
  id: number;
  refUser: string;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber?: string | null;
  role: string;
  roleLabel: string;
  companyCommercial?: string | null;
  companyName?: string;
  isActive: boolean;
  profilePhotoUrl?: string | null;
  createdAt: string;
}

/** Création / modification utilisateur (POST /users, PUT /users/{id}). */
export interface SaveUserDTO {
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  role: string;
  companyCommercial?: string | null;
}

/** Toggle statut (PATCH /users/{id}/status). */
export interface UpdateUserStatusDTO {
  isActive: boolean;
}

/** Réponse GET /admin/dashboard/stocks-etat-global pour le donut "Stocks - État global". */
export interface StockEtatGlobalDTO {
  normal: number;
  sousSeuil: number;
  critique: number;
}

/** Réponse GET /admin/dashboard/statut-tournees — effectifs par statut de tournée (clés ASSIGNEE, EN_COURS, …). */
export interface StatutTourneesDTO {
  parStatut: Record<string, number>;
}

// --- Référentiels / Configuration ---

export interface ReferenceItemDTO {
  id?: number;
  name: string;
  description: string;
}

export interface DeliveryOptionDTO {
  id?: number;
  name: string;
  description: string;
  isActive: boolean;
}

export interface FeeDTO {
  id?: number;
  name: string;
  description: string;
  amount: number;
  isActive: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = environment.apiUrl; 

  constructor(private http: HttpClient) {}

  /**
   * Récupère les stats du tableau de bord admin (3 KPIs + paiements par statut).
   * @param periode "TODAY" (aujourd'hui) ou "THIS_MONTH" (mois en cours)
   */
  getDashboardStats(): Observable<AdminDashboardStatsDTO> {
    return this.http.get<AdminDashboardStatsDTO>(`${this.apiUrl}/admin/dashboard/stats`);
  }

  /**
   * Alias historique : même réponse que getLivraisonsParJour() (nbPrevues, nbLivreesALaDate, nbRetard).
   */
  getCommandesVsLivraisons(): Observable<LivraisonParJourDTO[]> {
    return this.http.get<LivraisonParJourDTO[]>(`${this.apiUrl}/admin/dashboard/commandes-vs-livraisons`);
  }

  /**
   * 7 derniers jours : date (dd/MM), nbPrevues, nbLivreesALaDate, nbRetard.
   * Graphique « Livraisons » (empilé : livrées à la date, prévu non livré, retard).
   */
  getLivraisonsParJour(): Observable<LivraisonParJourDTO[]> {
    return this.http.get<LivraisonParJourDTO[]>(`${this.apiUrl}/admin/dashboard/livraisons-par-jour`);
  }

  /**
   * Récupère les effectifs par rôle pour le graphique "Utilisateurs par rôle".
   * GET /admin/users/stats/by-role → [{ roleLabel: "Salariés", count: 40, ... }, ...]
   */
  getUsersStatsByRole(): Observable<UserStatsByRoleItemDTO[]> {
    return this.http.get<UserStatsByRoleItemDTO[]>(`${this.apiUrl}/admin/users/stats/by-role`);
  }

  /** Liste paginée des utilisateurs (filtres optionnels). */
  getUsers(params: { page: number; size: number; search?: string; role?: string; status?: boolean }): Observable<UserListResponseDTO> {
    let httpParams = new HttpParams()
      .set('page', params.page.toString())
      .set('size', params.size.toString());
    if (params.search != null && params.search.trim() !== '') {
      httpParams = httpParams.set('search', params.search.trim());
    }
    if (params.role != null && params.role !== '') {
      httpParams = httpParams.set('role', params.role);
    }
    if (params.status != null) {
      httpParams = httpParams.set('status', params.status.toString());
    }
    return this.http.get<UserListResponseDTO>(`${this.apiUrl}/admin/users`, { params: httpParams });
  }

  /** Export Excel des utilisateurs (mêmes filtres que la liste : search, role, status). */
  exportUsers(search?: string, role?: string, status?: boolean): Observable<Blob> {
    let httpParams = new HttpParams();
    if (search != null && search.trim() !== '') {
      httpParams = httpParams.set('search', search.trim());
    }
    if (role != null && role !== '') {
      httpParams = httpParams.set('role', role);
    }
    if (status !== undefined && status !== null) {
      httpParams = httpParams.set('status', String(status));
    }
    return this.http.get(`${this.apiUrl}/admin/users/export`, {
      params: httpParams,
      responseType: 'blob'
    });
  }

  getUsersStats(): Observable<UserStatsDTO> {
    return this.http.get<UserStatsDTO>(`${this.apiUrl}/admin/users/stats`);
  }

  getUsersStatsByStatus(): Observable<UserStatsByStatusItemDTO[]> {
    return this.http.get<UserStatsByStatusItemDTO[]>(`${this.apiUrl}/admin/users/stats/by-status`);
  }

  getUserById(id: number): Observable<UserDetailsDTO> {
    return this.http.get<UserDetailsDTO>(`${this.apiUrl}/admin/users/${id}`);
  }

  updateUserStatus(id: number, dto: UpdateUserStatusDTO): Observable<string> {
    return this.http.patch(`${this.apiUrl}/admin/users/${id}/status`, dto, { responseType: 'text' });
  }

  /**
   * Met à jour un utilisateur via multipart/form-data (request params + photo optionnelle).
   * Tous les champs sont optionnels ; si profilePhoto est fourni, il remplace la photo actuelle.
   */
  updateUser(id: number, params: {
    firstName?: string;
    lastName?: string;
    email?: string;
    phoneNumber?: string;
    role?: string;
    companyCommercial?: string;
    profilePhoto?: File;
  }): Observable<string> {
    const form = new FormData();
    if (params.firstName != null) form.append('firstName', params.firstName);
    if (params.lastName != null) form.append('lastName', params.lastName);
    if (params.email != null) form.append('email', params.email);
    if (params.phoneNumber != null) form.append('phoneNumber', params.phoneNumber);
    if (params.role != null) form.append('role', params.role);
    if (params.companyCommercial != null && params.companyCommercial !== '') {
      form.append('companyCommercial', params.companyCommercial);
    }
    if (params.profilePhoto) {
      form.append('profilePhoto', params.profilePhoto, params.profilePhoto.name);
    }
    return this.http.put(`${this.apiUrl}/admin/users/${id}`, form, { responseType: 'text' });
  }

  /**
   * Crée un utilisateur via multipart/form-data (request params + photo optionnelle).
   * Paramètres : firstName, lastName, email, phoneNumber, role, companyCommercial (optionnel), profilePhoto (optionnel).
   */
  createUser(params: {
    firstName: string;
    lastName: string;
    email: string;
    phoneNumber: string;
    role: string;
    companyCommercial?: string;
    profilePhoto?: File;
  }): Observable<string> {
    const form = new FormData();
    form.append('firstName', params.firstName);
    form.append('lastName', params.lastName);
    form.append('email', params.email);
    form.append('phoneNumber', params.phoneNumber);
    form.append('role', params.role);
    if (params.companyCommercial != null && params.companyCommercial !== '') {
      form.append('companyCommercial', params.companyCommercial);
    }
    if (params.profilePhoto) {
      form.append('profilePhoto', params.profilePhoto, params.profilePhoto.name);
    }
    return this.http.post(`${this.apiUrl}/admin/users`, form, { responseType: 'text' });
  }

  /** Met à jour la photo de profil d'un utilisateur (PUT /admin/users/{id}/profile-photo). */
  updateUserProfilePhoto(userId: number, file: File): Observable<string> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.put(`${this.apiUrl}/admin/users/${userId}/profile-photo`, form, { responseType: 'text' });
  }

  /**
   * Supprime la photo de profil (POST /admin/users/{id}/profile-photo/remove).
   * POST évite les proxys qui ne relaient pas DELETE (erreur « Request method 'DELETE' is not supported »).
   */
  deleteUserProfilePhoto(userId: number): Observable<string> {
    return this.http.post(
      `${this.apiUrl}/admin/users/${userId}/profile-photo/remove`,
      {},
      { responseType: 'text' }
    );
  }

  /** Retourne l'URL complète pour afficher une photo de profil (ex. /api/files/profiles/xxx.jpg). */
  getProfilePhotoUrl(profilePhotoUrl: string | null | undefined): string | null {
    if (!profilePhotoUrl?.trim()) return null;
    const base = profilePhotoUrl.startsWith('http') ? '' : (environment.imageServerUrl ?? '').replace(/\/$/, '');
    return base ? `${base}/files/${profilePhotoUrl}` : `/files/${profilePhotoUrl}`;
  }

  /**
   * Récupère les effectifs Normal, Sous seuil, Critique pour le donut "Stocks - État global".
   * GET /admin/dashboard/stocks-etat-global
   */
  getStockEtatGlobal(): Observable<StockEtatGlobalDTO> {
    return this.http.get<StockEtatGlobalDTO>(`${this.apiUrl}/admin/dashboard/stocks-etat-global`);
  }

  /** Effectifs des tournées par statut — donut « Statut des livraisons ». GET /admin/dashboard/statut-tournees */
  getStatutTournees(): Observable<StatutTourneesDTO> {
    return this.http.get<StatutTourneesDTO>(`${this.apiUrl}/admin/dashboard/statut-tournees`);
  }

  /**
   * 7 derniers jours : date (dd/MM), nbUtilisations (commandes avec coupon). GET /admin/dashboard/coupons-utilises-par-jour
   */
  getCouponsUtilisesParJour(): Observable<CouponUsageParJourDTO[]> {
    return this.http.get<CouponUsageParJourDTO[]>(`${this.apiUrl}/admin/dashboard/coupons-utilises-par-jour`);
  }

  /** GET /api/admin/alerts — alertes (livraisons en retard, stocks critiques). module = LIVRAISONS | STOCKS pour la navigation. */
  getAlerts(): Observable<AdminAlertsDTO> {
    return this.http.get<AdminAlertsDTO>(`${this.apiUrl}/admin/alerts`);
  }

  // --- Catégories (id + nom + icon) ---
  /** @param noCache si true, ajoute un paramètre pour éviter le cache navigateur (après création/modification). */
  getCategories(noCache?: boolean): Observable<CategoryListItemDTO[]> {
    const options = noCache ? { params: { _: Date.now().toString() } } : {};
    return this.http.get<CategoryListItemDTO[]>(`${this.apiUrl}/admin/categories`, options);
  }

  getCategoryById(id: number): Observable<CategoryListItemDTO> {
    return this.http.get<CategoryListItemDTO>(`${this.apiUrl}/admin/categories/${id}`);
  }

  createCategory(body: { name: string; icon?: string }): Observable<string> {
    return this.http.post(`${this.apiUrl}/admin/categories`, body, { responseType: 'text' });
  }

  updateCategory(id: number, body: { name?: string; icon?: string }): Observable<string> {
    return this.http.put(`${this.apiUrl}/admin/categories/${id}`, body, { responseType: 'text' });
  }

  deleteCategory(id: number): Observable<string> {
    return this.http.delete(`${this.apiUrl}/admin/categories/${id}`, { responseType: 'text' });
  }

  /** Upload une icône (SVG, PNG, etc.) pour une catégorie. Retourne le chemin à mettre dans le champ icon. */
  uploadCategoryIcon(file: File): Observable<{ path: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ path: string }>(`${this.apiUrl}/admin/categories/upload-icon`, formData);
  }

  // --- Gestion des Référentiels / Configuration ---

  // Options de livraison
  getAllDeliveryOptions(): Observable<DeliveryOptionDTO[]> {
    return this.http.get<DeliveryOptionDTO[]>(`${this.apiUrl}/admin/delivery-options`);
  }
  createDeliveryOption(dto: DeliveryOptionDTO): Observable<string> {
    return this.http.post(`${this.apiUrl}/admin/delivery-options`, dto, { responseType: 'text' });
  }

  // Frais
  getAllFees(): Observable<FeeDTO[]> {
    return this.http.get<FeeDTO[]>(`${this.apiUrl}/admin/fees`);
  }
  createFee(dto: FeeDTO): Observable<string> {
    return this.http.post(`${this.apiUrl}/admin/fees`, dto, { responseType: 'text' });
  }

  // Types de réclamation
  getAllClaimProblemTypes(): Observable<ReferenceItemDTO[]> {
    return this.http.get<ReferenceItemDTO[]>(`${this.apiUrl}/admin/claim-problem-types`);
  }
  createClaimProblemType(dto: ReferenceItemDTO): Observable<string> {
    return this.http.post(`${this.apiUrl}/admin/claim-problem-types`, dto, { responseType: 'text' });
  }
  updateClaimProblemType(id: number, dto: ReferenceItemDTO): Observable<string> {
    return this.http.put(`${this.apiUrl}/admin/claim-problem-types/${id}`, dto, { responseType: 'text' });
  }
  deleteClaimProblemType(id: number): Observable<string> {
    return this.http.delete(`${this.apiUrl}/admin/claim-problem-types/${id}`, { responseType: 'text' });
  }

  // Raisons livreur (Motifs incidents livreur)
  getAllDeliveryIssueReasons(): Observable<ReferenceItemDTO[]> {
    return this.http.get<ReferenceItemDTO[]>(`${this.apiUrl}/admin/delivery-issue-reasons`);
  }
  createDeliveryIssueReason(dto: ReferenceItemDTO): Observable<string> {
    return this.http.post(`${this.apiUrl}/admin/delivery-issue-reasons`, dto, { responseType: 'text' });
  }
  updateDeliveryIssueReason(id: number, dto: ReferenceItemDTO): Observable<string> {
    return this.http.put(`${this.apiUrl}/admin/delivery-issue-reasons/${id}`, dto, { responseType: 'text' });
  }
  deleteDeliveryIssueReason(id: number): Observable<string> {
    return this.http.delete(`${this.apiUrl}/admin/delivery-issue-reasons/${id}`, { responseType: 'text' });
  }

  // Raisons salarié (Motifs incidents salarié)
  getAllEmployeeDeliveryIssueReasons(): Observable<ReferenceItemDTO[]> {
    return this.http.get<ReferenceItemDTO[]>(`${this.apiUrl}/admin/employee-delivery-issue-reasons`);
  }
  createEmployeeDeliveryIssueReason(dto: ReferenceItemDTO): Observable<string> {
    return this.http.post(`${this.apiUrl}/admin/employee-delivery-issue-reasons`, dto, { responseType: 'text' });
  }
  updateEmployeeDeliveryIssueReason(id: number, dto: ReferenceItemDTO): Observable<string> {
    return this.http.put(`${this.apiUrl}/admin/employee-delivery-issue-reasons/${id}`, dto, { responseType: 'text' });
  }
  deleteEmployeeDeliveryIssueReason(id: number): Observable<string> {
    return this.http.delete(`${this.apiUrl}/admin/employee-delivery-issue-reasons/${id}`, { responseType: 'text' });
  }

  // Secteurs d'activité
  getAllCompanySectors(): Observable<ReferenceItemDTO[]> {
    return this.http.get<ReferenceItemDTO[]>(`${this.apiUrl}/admin/company-sectors`);
  }
  createCompanySector(dto: ReferenceItemDTO): Observable<string> {
    return this.http.post(`${this.apiUrl}/admin/company-sectors`, dto, { responseType: 'text' });
  }
  updateCompanySector(id: number, dto: ReferenceItemDTO): Observable<string> {
    return this.http.put(`${this.apiUrl}/admin/company-sectors/${id}`, dto, { responseType: 'text' });
  }
  deleteCompanySector(id: number): Observable<string> {
    return this.http.delete(`${this.apiUrl}/admin/company-sectors/${id}`, { responseType: 'text' });
  }
}

export interface CategoryListItemDTO {
  id: number;
  name: string;
  icon?: string | null;
}
