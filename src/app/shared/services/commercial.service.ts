import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

// Structure des statistiques renvoyees par /commercial/companies/stats
export interface CompanyStats {
    totalCompanies: number;
    activeCompanies: number;
    inactiveCompanies: number;
}

// Structure des statistiques prospections (GET /commercial/prospects/stats)
export interface ProspectStats {
    total: number;
    enAttente: number;
    interesses: number;
    relancer: number;
    signes: number;
}

// Structure des statistiques renvoyees par /commercial/employees/stats
export interface EmployeeStats {
    totalEmployees: number;
    activeEmployees: number;
    pendingEmployees: number;
}

/** Un jour du graphique « Tendance des coupons utilisés » (7 derniers jours). */
export interface CouponUsageParJourDTO {
    date: string;
    nbUtilisations: number;
}

/** Données ventes par mois (évolution des ventes). */
export interface VentesParMoisDTO {
    mois: string;
    montant: number;
}

/** Données commandes par mois (évolution des commandes). */
export interface CommandesParMoisDTO {
    mois: string;
    nbCommandes: number;
}

/** KPIs du tableau de bord commercial (GET /api/commercial/dashboard/kpis). */
export interface CommercialDashboardKpisDTO {
    totalSalaries: number;
    nouveauxSalariesCeMois: number;
    commandesCeMois: number;
    evolutionCommandesPct: number | null;
    ventesCeMois: number;
    evolutionVentesPct: number | null;
    promotionsActives: number;
    evolutionVentes?: VentesParMoisDTO[];
    evolutionCommandes?: CommandesParMoisDTO[];
}

@Injectable({
    providedIn: 'root'
})
export class CommercialService {
  
    // SECTION 1 : VARIABLES GLOBALES ET INITIALISATION

    // URL de base de l'API (definie dans environment.ts)
    private apiUrl = environment.apiUrl;

    // Constructeur : injection du client HTTP
    constructor(private http: HttpClient) { }

    // SECTION 2 : METHODES PRINCIPALES (CRUD + ACTIONS)

    /** Liste des secteurs d'activité (référentiel, lecture seule). */
    getCompanySectors(): Observable<{ id: number; name: string; description?: string }[]> {
        return this.http.get<{ id: number; name: string; description?: string }[]>(`${this.apiUrl}/commercial/company-sectors`);
    }

    /** Entreprises partenaires uniquement (status = Partenaire signé). */
    getCompanies(
        page: number,
        size: number,
        search?: string,
        sectorId?: number,
        isActive?: boolean
    ): Observable<any> {
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());
        if (search?.trim()) params = params.set('search', search.trim());
        if (sectorId != null) params = params.set('sectorId', sectorId.toString());
        if (isActive !== undefined && isActive !== null) params = params.set('isActive', String(isActive));
        return this.http.get(`${this.apiUrl}/commercial/companies`, { params });
    }

    /** Prospects uniquement (status != Partenaire signé). */
    getProspects(
        page: number,
        size: number,
        search?: string,
        sectorId?: number,
        prospectionStatus?: string
    ): Observable<any> {
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());
        if (search?.trim()) params = params.set('search', search.trim());
        if (sectorId != null) params = params.set('sectorId', sectorId.toString());
        if (prospectionStatus?.trim()) params = params.set('prospectionStatus', prospectionStatus.trim());
        return this.http.get(`${this.apiUrl}/commercial/prospects`, { params });
    }

    /** Derniers prospects (entreprises non partenaires). limit défaut 3. */
    getLastProspects(limit: number = 3): Observable<any[]> {
        const params = new HttpParams().set('limit', limit.toString());
        return this.http.get<any[]>(`${this.apiUrl}/commercial/companies/last-prospects`, { params });
    }

    /** ==================================================
     *  METHODE : LIRE LES STATISTIQUES ENTREPRISES
     *  ================================================== */
    /**
     * But : Recuperer les compteurs pour les cartes du haut
     * Resultat attendu : total / actif / inactif
     */
    getCompanyStats(): Observable<CompanyStats> {
        return this.http.get<CompanyStats>(`${this.apiUrl}/commercial/companies/stats`);
    }

    /** Stats prospections (total, enAttente, interesses, relancer, signes). */
    getProspectStats(): Observable<ProspectStats> {
        return this.http.get<ProspectStats>(`${this.apiUrl}/commercial/prospects/stats`);
    }

    /** Stats entreprises partenaires (totalCompanies, activeCompanies, inactiveCompanies). */
    getPartnerStats(): Observable<CompanyStats> {
        return this.http.get<CompanyStats>(`${this.apiUrl}/commercial/partners/stats`);
    }

    /** GET /api/commercial/dashboard/kpis — KPIs du tableau de bord (salariés, commandes, ventes, promotions). */
    getDashboardKpis(): Observable<CommercialDashboardKpisDTO> {
        return this.http.get<CommercialDashboardKpisDTO>(`${this.apiUrl}/commercial/dashboard/kpis`);
    }

    /** GET /api/commercial/dashboard/coupons-utilises-par-jour — 7 derniers jours pour le graphique « Tendance des coupons utilisés ». */
    getCouponsUtilisesParJour(): Observable<CouponUsageParJourDTO[]> {
        return this.http.get<CouponUsageParJourDTO[]>(`${this.apiUrl}/commercial/dashboard/coupons-utilises-par-jour`);
    }

    /** ==================================================
     *  METHODE : CREER UNE ENTREPRISE
     *  ================================================== */
    /**
     * But : Envoyer un nouveau prospect au backend
     * Resultat attendu : L'entreprise est creee
     * Etapes :
     * 1. Recevoir les donnees
     * 2. Appeler l'API POST
     */
    /** Création via multipart/form-data : champs, logo optionnel en file. sectorId = ID du référentiel (GET /company-sectors). */
    createCompany(payload: {
        name: string;
        sectorId?: number;
        location: string;
        contactName: string;
        contactEmail?: string;
        contactPhone: string;
        status: string;
        note?: string;
        logo?: File;
    }): Observable<string> {
        const formData = new FormData();
        formData.append('name', payload.name);
        formData.append('location', payload.location);
        formData.append('contactName', payload.contactName);
        formData.append('contactPhone', payload.contactPhone);
        formData.append('status', payload.status);
        if (payload.sectorId != null) formData.append('sectorId', payload.sectorId.toString());
        if (payload.contactEmail) formData.append('contactEmail', payload.contactEmail);
        if (payload.note) formData.append('note', payload.note);
        if (payload.logo) formData.append('logo', payload.logo, payload.logo.name);
        return this.http.post(
            `${this.apiUrl}/commercial/companies`,
            formData,
            { responseType: 'text' }
        );
    }

    /** ==================================================
     *  METHODE : METTRE A JOUR UNE ENTREPRISE
     *  ================================================== */
    /**
     * But : Modifier les informations d'une entreprise
     * Resultat attendu : L'entreprise est mise a jour
     * Etapes :
     * 1. Recevoir l'id
     * 2. Envoyer les donnees
     * 3. Appeler l'API PUT
     */
    updateCompany(companyId: string, payload: {
        name: string;
        sectorId?: number;
        location: string;
        contactName: string;
        contactEmail?: string;
        contactPhone: string;
        status: string;
        note?: string;
    }): Observable<any> {
        return this.http.put(
            `${this.apiUrl}/commercial/companies/${companyId}`,
            payload,
            {
                responseType: 'text' as 'json'
            }
        );
    }

    /** ==================================================
     *  METHODE : LIRE LES DETAILS D'UNE ENTREPRISE
     *  ================================================== */
    /**
     * But : Recuperer les details complets
     * Resultat attendu : Un objet detaille
     */
    getCompanyDetails(companyId: string): Observable<any> {
        // Appeler l'API des details
        return this.http.get(`${this.apiUrl}/commercial/companies/${companyId}`);
    }

    /** ==================================================
     *  METHODE : ACTIVER / DESACTIVER UNE ENTREPRISE
     *  ================================================== */
    /**
     * But : Changer le statut actif/inactif
     * Resultat attendu : Le statut est mis a jour
     */
    updateCompanyStatus(companyId: string, isActive: boolean): Observable<any> {
        // Appeler l'API PATCH de statut
        return this.http.patch(
            `${this.apiUrl}/commercial/companies/${companyId}/status`,
            { isActive },
            { responseType: 'text' as 'json' }
        );
    }

    /** Téléverser le logo d'une entreprise (JPG, PNG, max 5 Mo). */
    uploadCompanyLogo(companyId: string, file: File): Observable<string> {
        const formData = new FormData();
        formData.append('file', file);
        return this.http.post(
            `${this.apiUrl}/commercial/companies/${companyId}/logo`,
            formData,
            { responseType: 'text' }
        );
    }

    /** Supprimer le logo d'une entreprise. */
    deleteCompanyLogo(companyId: string): Observable<string> {
        return this.http.delete(
            `${this.apiUrl}/commercial/companies/${companyId}/logo`,
            { responseType: 'text' }
        );
    }

    /** ==================================================
     *  METHODE : LISTER LES EMPLOYES
     *  ================================================== */
    /**
     * But : Recuperer la liste paginee avec filtres
     * Resultat attendu : Une reponse API avec contenu + pagination
     */
    getEmployees(
        page: number,
        size: number,
        search?: string,
        companyId?: string,
        isActive?: boolean
    ): Observable<any> {
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());

        if (search && search.trim()) {
            params = params.set('search', search.trim());
        }

        if (companyId) {
            params = params.set('companyId', companyId);
        }

        if (isActive !== undefined && isActive !== null) {
            params = params.set('isActive', String(isActive));
        }

        return this.http.get(`${this.apiUrl}/commercial/employees`, { params });
    }

    /** ==================================================
     *  METHODE : LIRE LES STATISTIQUES EMPLOYES
     *  ================================================== */
    /**
     * But : Recuperer les compteurs pour les cartes du haut
     * Resultat attendu : total / actif / en attente
     */
    getEmployeeStats(): Observable<EmployeeStats> {
        return this.http.get<EmployeeStats>(`${this.apiUrl}/commercial/employees/stats`);
    }

    /** ==================================================
     *  METHODE : CREER UN EMPLOYE
     *  ================================================== */
    /**
     * But : Envoyer un employe au backend
     * Resultat attendu : L'employe est cree
     */
    createEmployee(payload: {
        firstName: string;
        lastName: string;
        email: string;
        phone: string;
        address: string;
        companyId: string;
    }): Observable<any> {
        return this.http.post(
            `${this.apiUrl}/commercial/employees`,
            payload,
            { responseType: 'text' as 'json' }
        );
    }

    /** ==================================================
     *  METHODE : METTRE A JOUR UN EMPLOYE
     *  ================================================== */
    /**
     * But : Modifier les informations d'un employe
     * Resultat attendu : L'employe est mis a jour
     */
    updateEmployee(employeeId: string, payload: {
        firstName?: string;
        lastName?: string;
        email?: string;
        phone?: string;
        address?: string;
        companyId?: string;
    }): Observable<any> {
        return this.http.put(
            `${this.apiUrl}/commercial/employees/${employeeId}`,
            payload,
            { responseType: 'text' as 'json' }
        );
    }

    /** ==================================================
     *  METHODE : LIRE LES DETAILS D'UN EMPLOYE
     *  ================================================== */
    /**
     * But : Recuperer les details complets
     * Resultat attendu : Un objet detaille
     */
    getEmployeeDetails(employeeId: string): Observable<any> {
        return this.http.get(`${this.apiUrl}/commercial/employees/${employeeId}`);
    }

    /** ==================================================
     *  METHODE : ACTIVER / DESACTIVER UN EMPLOYE
     *  ================================================== */
    /**
     * But : Changer le statut actif/inactif
     * Resultat attendu : Le statut est mis a jour
     */
    updateEmployeeStatus(employeeId: string, isActive: boolean): Observable<any> {
        return this.http.patch(
            `${this.apiUrl}/commercial/employees/${employeeId}/status`,
            { isActive },
            { responseType: 'text' as 'json' }
        );
    }

    // ==================================================
    // COUPONS / PROMOTIONS
    // ==================================================

    static readonly DISCOUNT_TYPE = { PERCENTAGE: 'PERCENTAGE', FIXED_AMOUNT: 'FIXED_AMOUNT' } as const;
    static readonly COUPON_STATUS = { PLANNED: 'PLANNED', ACTIVE: 'ACTIVE', EXPIRED: 'EXPIRED', DISABLED: 'DISABLED' } as const;

    getCoupons(
        page: number,
        size: number,
        search?: string,
        status?: string,
        isActive?: boolean
    ): Observable<CouponListResponse> {
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());
        if (search && search.trim()) params = params.set('search', search.trim());
        if (status) params = params.set('status', status);
        if (isActive !== undefined && isActive !== null) params = params.set('isActive', String(isActive));
        return this.http.get<CouponListResponse>(`${this.apiUrl}/commercial/coupons`, { params });
    }

    /** Statistiques des coupons panier (CART_TOTAL) : nombre actifs et total d'utilisations. */
    getCartTotalCouponStats(): Observable<CartTotalCouponStats> {
        return this.http.get<CartTotalCouponStats>(`${this.apiUrl}/commercial/coupons/cart-total-stats`);
    }

    /** Produits actifs (id, name) pour le modal création coupon. */
    getActiveProductsForCoupon(): Observable<{ id: number; name: string }[]> {
        return this.http.get<{ id: number; name: string }[]>(`${this.apiUrl}/commercial/coupons/products`);
    }

    /** Catégories (id, name) pour le modal création coupon. */
    getCategoriesForCoupon(): Observable<{ id: number; name: string }[]> {
        return this.http.get<{ id: number; name: string }[]>(`${this.apiUrl}/commercial/coupons/categories`);
    }

    getCouponById(id: number): Observable<CouponDetails> {
        return this.http.get<CouponDetails>(`${this.apiUrl}/commercial/coupons/${id}`);
    }

    createCoupon(payload: CreateCouponPayload): Observable<string> {
        return this.http.post(
            `${this.apiUrl}/commercial/coupons`,
            payload,
            { responseType: 'text' }
        );
    }

    updateCouponStatus(couponId: number, isActive: boolean): Observable<string> {
        return this.http.patch(
            `${this.apiUrl}/commercial/coupons/${couponId}/status`,
            { isActive },
            { responseType: 'text' }
        );
    }

    deleteCoupon(couponId: number): Observable<string> {
        return this.http.delete(
            `${this.apiUrl}/commercial/coupons/${couponId}`,
            { responseType: 'text' }
        );
    }

    /** Produits pour création de promotion (optionnel : filtrer par catégorie). */
    getProductsForPromotion(categoryId?: number): Observable<{ id: number; name: string }[]> {
        const params = categoryId != null ? { params: { categoryId: categoryId.toString() } } : {};
        return this.http.get<{ id: number; name: string }[]>(`${this.apiUrl}/commercial/promotions/products`, params);
    }

    /** Créer une promotion (nom, dates, liste produit + réduction %). */
    createPromotion(payload: CreatePromotionPayload): Observable<string> {
        return this.http.post(
            `${this.apiUrl}/commercial/promotions`,
            payload,
            { responseType: 'text' }
        );
    }

    /** Liste paginée des promotions (produit) avec recherche et filtre par statut. */
    getPromotions(
        page: number,
        size: number,
        search?: string,
        status?: string
    ): Observable<PromotionListResponse> {
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());
        if (search?.trim()) params = params.set('search', search.trim());
        if (status) params = params.set('status', status);
        return this.http.get<PromotionListResponse>(`${this.apiUrl}/commercial/promotions`, { params });
    }

    /** Statistiques des promotions (total, actives, planifiées, expirées, désactivées, produits concernés). */
    getPromotionStats(): Observable<PromotionStats> {
        return this.http.get<PromotionStats>(`${this.apiUrl}/commercial/promotions/stats`);
    }

    /** Détails d'une promotion (nom, dates, statut, produits avec réduction). */
    getPromotionById(id: number): Observable<PromotionDetails> {
        return this.http.get<PromotionDetails>(`${this.apiUrl}/commercial/promotions/${id}`);
    }

    /** Activer ou désactiver une promotion (comme pour les coupons). */
    updatePromotionStatus(id: number, isActive: boolean): Observable<string> {
        return this.http.patch(
            `${this.apiUrl}/commercial/promotions/${id}/status`,
            { isActive },
            { responseType: 'text' }
        );
    }
}

// --- Types pour les promotions (produit)
export interface PromotionListResponse {
    content: PromotionListItem[];
    totalElements: number;
    totalPages: number;
    currentPage: number;
    pageSize: number;
    hasNext: boolean;
    hasPrevious: boolean;
}

export interface PromotionListItem {
    id: number;
    name: string;
    status: string;
    isActive: boolean;
    startDate: string;
    endDate: string;
    productCount: number;
}

export interface PromotionDetails {
    id: number;
    name: string;
    status: string;
    isActive: boolean;
    startDate: string;
    endDate: string;
    products: { productId: number; productName: string; image?: string; discountValue: number }[];
}

export interface PromotionStats {
    totalPromotions: number;
    promotionsActives: number;
    promotionsPlanifiees: number;
    promotionsExpirees: number;
    promotionsDesactivees: number;
    totalProduitsConcernes: number;
}

/** Payload création promotion : name, startDate, endDate, productItems (productId, discountValue en %). */
export interface CreatePromotionPayload {
    name: string;
    startDate: string;
    endDate: string;
    productItems: { productId: number; discountValue: number }[];
}

// --- Stats coupons panier (CART_TOTAL)
export interface CartTotalCouponStats {
    activeCouponsCount: number;
    totalUsages: number;
    totalGenerated: number;
}

// --- Types pour les coupons
export interface CouponListResponse {
    content: CouponListItem[];
    totalElements: number;
    totalPages: number;
    currentPage: number;
    pageSize: number;
    hasNext: boolean;
    hasPrevious: boolean;
}

export interface CouponListItem {
    id: number;
    code: string;
    name: string;
    discountType: 'PERCENTAGE' | 'FIXED_AMOUNT';
    value: number;
    status: string;
    validFrom: string;
    validTo: string;
    usageCount?: number;
    totalGenerated?: number;
}

export interface CouponDetails {
    id: number;
    code: string;
    name: string;
    discountType: 'PERCENTAGE' | 'FIXED_AMOUNT';
    value: number;
    status: string;
    isActive: boolean;
    validFrom: string;
    validTo: string;
    usageCount?: number;
    totalGenerated?: number;
}

export interface CreateCouponPayload {
    code: string;
    name: string;
    discountType: 'PERCENTAGE' | 'FIXED_AMOUNT';
    value: number;
    status: string;
    startDate: string;
    endDate: string;
}

