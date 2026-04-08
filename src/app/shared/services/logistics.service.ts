import { Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class LogisticsService {
    private apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) { }

    // Liste des fournisseurs (pour dropdown)
    getSuppliers(): Observable<any> {
        return this.http.get(`${this.apiUrl}/logistics/suppliers`);
    }

    /**
     * Liste paginée des commandes salariés (gestion des commandes RL).
     * @param page 0-based
     * @param size nombre par page
     * @param search recherche par référence ou nom
     * @param status EN_ATTENTE | VALIDEE | LIVREE | ANNULEE | etc. (optionnel)
     */
    getEmployeeOrders(
        page: number,
        size: number,
        search?: string,
        status?: string
    ): Observable<{
        content: Array<{
            id: number;
            orderNumber: string;
            employeeName: string;
            orderDate?: string;
            statusDate?: string;
            products: string[];
            deliveryFrequency: string | null;
            status: string;
            failureReason?: string | null;
        }>;
        totalElements: number;
        totalPages: number;
        currentPage: number;
        pageSize: number;
        hasNext: boolean;
        hasPrevious: boolean;
    }> {
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());
        if (search && search.trim()) {
            params = params.set('search', search.trim());
        }
        if (status) {
            params = params.set('status', status);
        }
        return this.http.get<{
            content: Array<{
                id: number;
                orderNumber: string;
                employeeName: string;
                orderDate?: string;
                statusDate?: string;
                products: string[];
                deliveryFrequency: string | null;
                status: string;
                failureReason?: string | null;
            }>;
            totalElements: number;
            totalPages: number;
            currentPage: number;
            pageSize: number;
            hasNext: boolean;
            hasPrevious: boolean;
        }>(`${this.apiUrl}/logistics/employee-orders`, { params });
    }

    /** Export Excel des commandes salariés (mêmes filtres que la liste : search, status). */
    exportEmployeeOrders(search?: string, status?: string): Observable<Blob> {
        let params = new HttpParams();
        if (search?.trim()) {
            params = params.set('search', search.trim());
        }
        if (status) {
            params = params.set('status', status);
        }
        return this.http.get(`${this.apiUrl}/logistics/employee-orders/export`, {
            params,
            responseType: 'blob'
        });
    }

    /** Replanifier une commande en échec (RL). Passe en EN_ATTENTE, notifie le salarié. */
    replanOrder(orderId: number): Observable<string> {
        return this.http.patch(`${this.apiUrl}/logistics/employee-orders/${orderId}/replan`, {}, { responseType: 'text' });
    }

    /** Annuler définitivement une commande après échec (RL). ANNULEE + réintégration stock, notifie le salarié. */
    cancelOrderAfterFailure(orderId: number): Observable<string> {
        return this.http.patch(`${this.apiUrl}/logistics/employee-orders/${orderId}/cancel-after-failure`, {}, { responseType: 'text' });
    }

    /**
     * Statistiques pour la page Gestion des commandes : total (hors annulées), EN ATTENTE, EN RETARD, EN COURS, VALIDÉES, LIVRÉES ce mois.
     */
    getEmployeeOrderStats(): Observable<EmployeeOrderStatsKpis> {
        return this.http.get<EmployeeOrderStatsKpis>(`${this.apiUrl}/logistics/employee-orders/stats`);
    }

    /**
     * Détails d'une commande salarié (avec liste des produits : image, nom, catégorie, stock).
     * GET /logistics/employee-order/{id}
     * Accept: application/json pour éviter 406 Not Acceptable.
     */
    getEmployeeOrderDetails(orderId: number): Observable<EmployeeOrderDetails> {
        const headers = new HttpHeaders({ 'Accept': 'application/json' });
        return this.http.get<EmployeeOrderDetails>(`${this.apiUrl}/logistics/employee-order/${orderId}`, { headers });
    }

    // Statistiques des commandes fournisseurs
    getSupplierOrderStats(): Observable<any> {
        return this.http.get(`${this.apiUrl}/logistics/supplier-orders/stats`);
    }

    // Lister les commandes fournisseurs
    getSupplierOrders(
        page: number,
        size: number,
        search?: string,
        supplierId?: number,
        status?: string
    ): Observable<any> {
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());

        if (search && search.trim()) {
            params = params.set('search', search.trim());
        }
        if (supplierId) {
            params = params.set('supplierId', supplierId.toString());
        }
        if (status) {
            params = params.set('status', status);
        }

        return this.http.get(`${this.apiUrl}/logistics/supplier-orders`, { params });
    }

    // Détails d'une commande fournisseur
    getSupplierOrderDetails(orderId: string): Observable<any> {
        return this.http.get(`${this.apiUrl}/logistics/supplier-orders/${orderId}`);
    }

    // Créer une commande fournisseur
    createSupplierOrder(payload: {
        supplierId: number;
        items: { productId: number; quantite: number }[];
        expectedDate?: string;
        notes?: string;
    }): Observable<any> {
        return this.http.post(
            `${this.apiUrl}/logistics/supplier-orders`,
            payload,
            { responseType: 'text' as 'json' }
        );
    }

    // Modifier une commande fournisseur
    updateSupplierOrder(orderId: string, payload: {
        expectedDate?: string;
        notes?: string;
        items?: { productId: number; quantite: number }[];
    }): Observable<any> {
        return this.http.put(
            `${this.apiUrl}/logistics/supplier-orders/${orderId}`,
            payload,
            { responseType: 'text' as 'json' }
        );
    }

    // Modifier le statut d'une commande fournisseur
    updateSupplierOrderStatus(orderId: string, status: string): Observable<any> {
        return this.http.patch(
            `${this.apiUrl}/logistics/suppliers-orders/${orderId}/status`,
            { status },
            { responseType: 'text' as 'json' }
        );
    }

    // Exporter la liste des commandes fournisseurs
    exportSupplierOrders(
        search?: string,
        supplierId?: number,
        status?: string
    ): Observable<Blob> {
        let params = new HttpParams();
        if (search && search.trim()) {
            params = params.set('search', search.trim());
        }
        if (supplierId) {
            params = params.set('supplierId', supplierId.toString());
        }
        if (status) {
            params = params.set('status', status);
        }

        return this.http.get(`${this.apiUrl}/logistics/supplier-orders/export`, {
            params,
            responseType: 'blob'
        });
    }

    // Lister les stocks (pour récupérer les produits disponibles)
    getStockList(
        page: number,
        size: number,
        search?: string,
        categoryId?: number,
        status?: boolean
    ): Observable<any> {
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());

        if (search && search.trim()) {
            params = params.set('search', search.trim());
        }
        if (categoryId) {
            params = params.set('categoryId', categoryId.toString());
        }
        if (status !== undefined) {
            params = params.set('status', String(status));
        }

        return this.http.get(`${this.apiUrl}/logistics/stocks`, { params });
    }

    // Statistiques du suivi des stocks
    getStockStats(): Observable<any> {
        return this.http.get(`${this.apiUrl}/logistics/stocks/stats`);
    }

    // Liste des alertes de stock
    getStockAlerts(
        page: number,
        size: number,
        search?: string,
        categoryId?: number
    ): Observable<any> {
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());

        if (search && search.trim()) {
            params = params.set('search', search.trim());
        }
        if (categoryId) {
            params = params.set('categoryId', categoryId.toString());
        }

        return this.http.get(`${this.apiUrl}/logistics/stocks/alerts`, { params });
    }

    // Export du suivi des stocks
    exportStockList(
        search?: string,
        categoryId?: number,
        status?: boolean
    ): Observable<Blob> {
        let params = new HttpParams();
        if (search && search.trim()) {
            params = params.set('search', search.trim());
        }
        if (categoryId) {
            params = params.set('categoryId', categoryId.toString());
        }
        if (status !== undefined) {
            params = params.set('status', String(status));
        }
        return this.http.get(`${this.apiUrl}/logistics/stocks/export`, {
            params,
            responseType: 'blob'
        });
    }

    // Export des alertes de stock
    exportStockAlerts(
        search?: string,
        categoryId?: number
    ): Observable<Blob> {
        let params = new HttpParams();
        if (search && search.trim()) {
            params = params.set('search', search.trim());
        }
        if (categoryId) {
            params = params.set('categoryId', categoryId.toString());
        }
        return this.http.get(`${this.apiUrl}/logistics/stocks/alerts/export`, {
            params,
            responseType: 'blob'
        });
    }

    // Entrée de stock
    increaseStock(productId: string, quantity: number): Observable<any> {
        const params = new HttpParams().set('quantity', quantity.toString());
        return this.http.post(
            `${this.apiUrl}/logistics/stocks/${productId}/in`,
            null,
            { params, responseType: 'text' as 'json' }
        );
    }

    // Sortie de stock
    decreaseStock(productId: string, quantity: number): Observable<any> {
        const params = new HttpParams().set('quantity', quantity.toString());
        return this.http.post(
            `${this.apiUrl}/logistics/stocks/${productId}/out`,
            null,
            { params, responseType: 'text' as 'json' }
        );
    }

    // Modifier le seuil minimum
    updateMinThreshold(productId: string, minThreshold: number): Observable<any> {
        const params = new HttpParams().set('minThreshold', minThreshold.toString());
        return this.http.patch(
            `${this.apiUrl}/logistics/stocks/${productId}/threshold`,
            null,
            { params, responseType: 'text' as 'json' }
        );
    }

    // Modifier le seuil minimum par pourcentage
    updateMinThresholdByPercent(productId: string, percent: number): Observable<any> {
        const params = new HttpParams().set('percent', percent.toString());
        return this.http.patch(
            `${this.apiUrl}/logistics/stocks/${productId}/threshold/percent`,
            null,
            { params, responseType: 'text' as 'json' }
        );
    }

    // --- Tournées de livraison ---

    /** Commandes éligibles pour une date (liste plate). Date au format dd-MM-yyyy */
    getEligibleOrders(deliveryDate: string): Observable<EligibleOrder[]> {
        const params = new HttpParams().set('deliveryDate', deliveryDate);
        return this.http.get<EligibleOrder[]>(`${this.apiUrl}/logistics/delivery-tours/eligible-orders`, { params });
    }

    /** Commandes éligibles groupées par proximité. Date au format dd-MM-yyyy */
    getGroupedEligibleOrders(deliveryDate: string, lotSize: number): Observable<EligibleOrderLot[]> {
        const params = new HttpParams()
            .set('deliveryDate', deliveryDate)
            .set('lotSize', lotSize.toString());
        return this.http.get<EligibleOrderLot[]>(`${this.apiUrl}/logistics/delivery-tours/eligible-orders/grouped`, { params });
    }

    /** Nombre de commandes éligibles pour la date (même règles que grouped). Date au format dd-MM-yyyy */
    getEligibleOrdersCount(deliveryDate: string): Observable<number> {
        const params = new HttpParams().set('deliveryDate', deliveryDate);
        return this.http.get<number>(`${this.apiUrl}/logistics/delivery-tours/eligible-orders/count`, { params });
    }

    /**
     * Chauffeurs actifs. Avec deliveryDate (dd-MM-yyyy), exclut ceux ayant déjà une tournée assignée ou en cours ce jour.
     * excludeTourId : à la modification, ignorer cette tournée pour que le livreur actuel reste listé.
     */
    getAvailableDrivers(deliveryDateDdMmYyyy?: string, excludeTourId?: number): Observable<AvailableDriver[]> {
        let params = new HttpParams();
        if (deliveryDateDdMmYyyy?.trim()) {
            params = params.set('deliveryDate', deliveryDateDdMmYyyy.trim());
        }
        if (excludeTourId != null && excludeTourId !== undefined) {
            params = params.set('excludeTourId', String(excludeTourId));
        }
        return this.http.get<AvailableDriver[]>(`${this.apiUrl}/logistics/delivery-tours/available-drivers`, {
            params
        });
    }

    /**
     * Calendrier de planification (mois) : jours du mois + totalOverdueGlobal (retards sur toutes les dates, pas seulement le mois affiché).
     */
    getPlanningCalendar(year: number, month: number): Observable<DeliveryPlanningCalendarResponse> {
        let params = new HttpParams()
            .set('year', year.toString())
            .set('month', month.toString());
        return this.http.get<DeliveryPlanningCalendarResponse>(
            `${this.apiUrl}/logistics/delivery-tours/planning-calendar`,
            { params }
        );
    }

    /** Créer une tournée de livraison */
    createDeliveryTour(payload: CreateDeliveryTourPayload): Observable<string> {
        return this.http.post(
            `${this.apiUrl}/logistics/delivery-tours`,
            payload,
            { responseType: 'text' }
        ) as Observable<string>;
    }

    /**
     * Liste paginée des tournées de livraison (N° Tour | Date | Chauffeur | Véhicule | Nb Cmd | Statut | Actions).
     * @param page 0-based
     * @param size nombre par page
     * @param tourNumber filtre optionnel par numéro de tour
     * @param status ASSIGNEE | EN_COURS | TERMINEE | ANNULEE (optionnel)
     */
    getDeliveryTours(
        page: number,//
        size: number,//
        tourNumber?: string,
        status?: string
    ): Observable<DeliveryTourListResponse> {
        //on définit les paramètres de la requête
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());
        if (tourNumber && tourNumber.trim()) {
            params = params.set('tourNumber', tourNumber.trim());
        }
        if (status) {
            params = params.set('status', status);
        }
        return this.http.get<DeliveryTourListResponse>(`${this.apiUrl}/logistics/delivery-tours`, { params });
    }

    /** Export Excel des tournées (tourNumber, status — alignés sur la liste). */
    exportDeliveryTours(tourNumber?: string, status?: string): Observable<Blob> {
        let params = new HttpParams();
        if (tourNumber?.trim()) {
            params = params.set('tourNumber', tourNumber.trim());
        }
        if (status) {
            params = params.set('status', status);
        }
        return this.http.get(`${this.apiUrl}/logistics/delivery-tours/export`, {
            params,
            responseType: 'blob'
        });
    }

    /** Statistiques des tournées par statut (Assignées, En cours, Terminées, Annulées). */
    getDeliveryTourStats(): Observable<DeliveryTourStats> {
        return this.http.get<DeliveryTourStats>(`${this.apiUrl}/logistics/delivery-tours/stats`);
    }

    /** Détails d'une tournée (GET /logistics/delivery-tours/{tourId}). */
    getDeliveryTourDetails(tourId: number): Observable<DeliveryTourDetails> {
        return this.http.get<DeliveryTourDetails>(`${this.apiUrl}/logistics/delivery-tours/${tourId}`);
    }

    /** Mettre à jour une tournée (PATCH). Date, livreur, véhicule, notes, commandes à garder — tournée ASSIGNEE uniquement. */
    updateDeliveryTour(tourId: number, body: {
        vehicleInfo?: string;
        notes?: string | null;
        orderIds?: number[];
        deliveryDate?: string;
        driverId?: number;
    }): Observable<string> {
        return this.http.patch(
            `${this.apiUrl}/logistics/delivery-tours/${tourId}`,
            body,
            { responseType: 'text' }
        ) as Observable<string>;
    }

    /** Retirer une commande d'une tournée (DELETE /logistics/delivery-tours/{tourId}/orders/{orderId}). */
    removeOrderFromTour(tourId: number, orderId: number): Observable<string> {
        return this.http.delete(
            `${this.apiUrl}/logistics/delivery-tours/${tourId}/orders/${orderId}`,
            { responseType: 'text' }
        ) as Observable<string>;
    }

    /** Annuler une tournée (POST /logistics/delivery-tours/{tourId}/cancel). Motif obligatoire. */
    cancelDeliveryTour(tourId: number, body: { reason: string }): Observable<string> {
        return this.http.post(
            `${this.apiUrl}/logistics/delivery-tours/${tourId}/cancel`,
            body,
            { responseType: 'text' }
        ) as Observable<string>;
    }

    // ========== GESTION DES RETOURS (RÉCLAMATIONS) ==========

    /** Statistiques des retours : total, validés, rejetés, réintégrés, montant remboursé. */
    getClaimStats(): Observable<ClaimStats> {
        return this.http.get<ClaimStats>(`${this.apiUrl}/logistics/claims/stats`);
    }

    /** Liste paginée des réclamations (retours). search = référence, client ou nom de produit, status = EN_ATTENTE | VALIDE | REJETE */
    getClaims(
        page: number,
        size: number,
        search?: string,
        status?: string
    ): Observable<ClaimListResponse> {
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());
        if (search && search.trim()) {
            params = params.set('search', search.trim());
        }
        if (status) {
            params = params.set('status', status);
        }
        return this.http.get<ClaimListResponse>(`${this.apiUrl}/logistics/claims`, { params });
    }

    /** Export Excel des réclamations (retours), mêmes filtres que la liste : search, status (EN_ATTENTE | VALIDE | REJETE). */
    exportClaims(search?: string, status?: string): Observable<Blob> {
        let params = new HttpParams();
        if (search?.trim()) {
            params = params.set('search', search.trim());
        }
        if (status) {
            params = params.set('status', status);
        }
        return this.http.get(`${this.apiUrl}/logistics/claims/export`, {
            params,
            responseType: 'blob'
        });
    }

    /** Détails d'une réclamation (retour). */
    getClaimById(id: number): Observable<ClaimDetail> {
        return this.http.get<ClaimDetail>(`${this.apiUrl}/logistics/claims/${id}`);
    }

    /** Valider une réclamation : réintégration au stock ou remboursement. */
    validateClaim(id: number, payload: ValidateClaimPayload): Observable<string> {
        return this.http.post(
            `${this.apiUrl}/logistics/claims/${id}/validate`,
            payload,
            { responseType: 'text' }
        ) as Observable<string>;
    }

    /** Rejeter une réclamation (motif obligatoire). */
    rejectClaim(id: number, payload: RejectClaimPayload): Observable<string> {
        return this.http.post(
            `${this.apiUrl}/logistics/claims/${id}/reject`,
            payload,
            { responseType: 'text' }
        ) as Observable<string>;
    }

    // ----------- Tableau de bord RL -----------

    /** KPIs RL : en attente / en retard alignés sur la planification (salarié actif, sans tournée) ; tournées actives ; livrées ce mois. */
    getDashboardKpis(): Observable<RLDashboardKpis> {
        return this.http.get<RLDashboardKpis>(`${this.apiUrl}/logistics/dashboard/kpis`);
    }

    /** Graphique Statut tournées : { ASSIGNEE: n, EN_COURS: n, TERMINEE: n, ANNULEE: n }. */
    getStatutTournees(): Observable<StatutTournees> {
        return this.http.get<StatutTournees>(`${this.apiUrl}/logistics/dashboard/statut-tournees`);
    }

    /** Graphique Commandes par jour (7 derniers jours) : date (dd/MM), nbCommandes. */
    getCommandesParJour(): Observable<CommandesParJourItem[]> {
        return this.http.get<CommandesParJourItem[]>(`${this.apiUrl}/logistics/dashboard/commandes-par-jour`);
    }

    /** Graphique Taux de retours (%) : 7 derniers jours, date (dd/MM) et tauxPercent (réclamations/commandes × 100). */
    getTauxRetoursParJour(): Observable<TauxRetoursParJourItem[]> {
        return this.http.get<TauxRetoursParJourItem[]>(`${this.apiUrl}/logistics/dashboard/taux-retours-par-jour`);
    }

    /** Top 5 produits les plus commandés (en % d'utilisation) pour le graphique « Produits les plus fréquents » (Gestion des commandes). */
    getTop5ProductsUsage(): Observable<{ productName: string; usagePercent: number }[]> {
        return this.http.get<{ productName: string; usagePercent: number }[]>(`${this.apiUrl}/logistics/dashboard/top5-products-usage`);
    }

    /** Graphique Livraisons par jour (7 derniers jours) : date, nbPrevues, nbLivreesALaDate, nbRetard. */
    getLivraisonsParJour(): Observable<LivraisonParJourItem[]> {
        return this.http.get<LivraisonParJourItem[]>(`${this.apiUrl}/logistics/dashboard/livraisons-par-jour`);
    }

    /**
     * Tableau de bord RL — 7 derniers jours, même sémantique que getLivraisonsParJour()
     * (date prévue, livrées à la date, retard).
     */
    getCommandesVsLivraisons(): Observable<LivraisonParJourItem[]> {
        return this.http.get<LivraisonParJourItem[]>(`${this.apiUrl}/logistics/dashboard/commandes-vs-livraisons`);
    }

    /** Donut Stocks - État global : effectifs normal, sousSeuil, critique (GET /logistics/dashboard/stock-etat-global). */
    getStockEtatGlobal(): Observable<StockEtatGlobalDTO> {
        return this.http.get<StockEtatGlobalDTO>(`${this.apiUrl}/logistics/dashboard/stock-etat-global`);
    }
}

/** KPIs page Gestion des commandes (GET /logistics/employee-orders/stats). totalCommandes = tous statuts sauf annulé. */
export interface EmployeeOrderStatsKpis {
    totalCommandes: number;
    enAttente: number;
    enRetard: number;
    enCours: number;
    validees: number;
    livreesCeMois: number;
}

export interface RLDashboardKpis {
    commandesEnAttente: number;
    commandesEnRetard: number;
    tourneesActives: number;
    livreesCeMois: number;
}

export interface StatutTournees {
    parStatut: Record<string, number>;
}

export interface CommandesParJourItem {
    date: string;
    nbCommandes: number;
}

export interface TauxRetoursParJourItem {
    date: string;
    tauxPercent: number;
}

export interface LivraisonParJourItem {
    date: string;
    nbPrevues: number;
    nbLivreesALaDate: number;
    nbRetard: number;
}

export interface StockEtatGlobalDTO {
    normal: number;
    sousSeuil: number;
    critique: number;
}

// --- Types pour les retours (claims) ---
export interface ClaimStats {
    total: number;
    validatedCount: number;
    rejectedCount: number;
    reintegratedCount: number;
    totalRefundAmount: number;
}

export interface ClaimListItem {
    claimId: number;
    orderNumber: string;
    employeeName: string;
    productName: string;
    productImage: string | null;
    quantity: number;
    problemTypeLabel: string;
    status: string;
    createdAt: string;
    decisionLabel: string | null;
    refundAmount: number | null;
}

export interface ClaimListResponse {
    content: ClaimListItem[];
    totalElements: number;
    totalPages: number;
    currentPage: number;
    pageSize: number;
    hasNext: boolean;
    hasPrevious: boolean;
}

export interface ClaimDetail {
    claimId: number;
    orderNumber: string;
    status: string;
    createdAt: string;
    employeeName: string;
    employeePhone: string | null;
    productId: number;
    productName: string;
    productImage: string | null;
    quantityOrdered: number;
    subtotalProduct: number;
    problemTypeLabel: string;
    comment: string | null;
    photoUrls: string[];
    decisionTypeLabel: string | null;
    refundAmount: number | null;
    rejectionReason: string | null;
}

export interface ValidateClaimPayload {
    decisionType: 'REINTEGRATION' | 'REMBOURSEMENT';
    quantityToReintegrate?: number;
    refundAmount?: number;
}

export interface RejectClaimPayload {
    rejectionReason: string;
}

export interface EligibleOrder {
    orderId: number;
    orderNumber: string;
    customerName: string;
    formattedAddress: string;
    /** Jours préférés (ex: ["LUNDI", "MARDI"]) — informatif pour le RL */
    preferredDays?: string[] | null;
    /** Créneau préféré (ex: "Matin (8h-12h)") */
    preferredTimeSlot?: string | null;
    /** Mode livraison (ex: "Domicile", "Bureau") */
    preferredDeliveryMode?: string | null;
    /** true si la tournée correspond aux préférences */
    matchesPreferences?: boolean | null;
    /** false si le salarié n'a pas renseigné de préférences */
    hasPreferences?: boolean | null;
    /** Nombre de jours de retard (date de livraison prévue dépassée). null si pas en retard. */
    daysOverdue?: number | null;
}

export interface EligibleOrderLot {
    lotIndex: number;
    orderCount: number;
    orders: EligibleOrder[];
    zoneName?: string;  // libellé de zone pour affichage (ex. "Lot 1", "Plateau", ...)
}

export interface AvailableDriver {
    driverId: number;
    fullName: string;
}

export interface CreateDeliveryTourPayload {
    deliveryDate: string;   // yyyy-MM-dd
    driverId: number;
    vehiclePlate: string;
    vehicleType: string;
    orderIds: number[];
    notes?: string;
}

/** Un élément de la liste des tournées (aligné sur le backend DeliveryTourListDTO). */
export interface DeliveryTourListItem {
    id: number;
    tourNumber: string;
    deliveryDate: string;
    driverName: string;
    vehicle: string;
    orderCount: number;
    status: string;  // ASSIGNEE | EN_COURS | TERMINEE | ANNULEE
}

export interface DeliveryPlanningCalendarDay {
    /** Date au format dd/MM/yyyy */
    date: string;
    pendingOrders: number;
    plannedOrders: number;
    /** Parmi les pendingOrders, celles dont la date est passée (date < aujourd'hui). */
    overdueOrders: number;
}

/** Réponse API planning-calendar : grille du mois + total retards global. */
export interface DeliveryPlanningCalendarResponse {
    days: DeliveryPlanningCalendarDay[];
    totalOverdueGlobal: number;
}

/** Une commande dans la liste des commandes d'une tournée. */
export interface OrderInTour {
    orderId: number;
    orderNumber: string;
    employeeName: string;
    employeeFirstName?: string;
    employeeLastName?: string;
    /** Entreprise du salarié */
    companyName?: string;
    deliveryAddress: string;
    /** Montant total commande */
    totalAmount?: number;
    /** Code statut (EN_COURS, LIVREE, ECHEC_LIVRAISON, …) */
    orderStatus?: string;
    orderStatusLabel?: string;
    /** Libellé du statut du paiement (Impayé, Payé, …) — détail tournée */
    paymentStatusLabel?: string;
}

/** Détails d'une tournée (GET /logistics/delivery-tours/{tourId}). */
export interface DeliveryTourDetails {
    id: number;
    tourNumber?: string;
    deliveryDate?: string;
    /** Id livreur (liste déroulante édition) */
    driverId?: number;
    driverName?: string;
    /** Téléphone du livreur */
    driverPhone?: string;
    vehicle?: string;
    vehicleType?: string;
    vehiclePlate?: string;
    orderCount?: number;
    status?: string;
    orders?: OrderInTour[];
    /** Récap : commandes livrées */
    deliveredOrderCount?: number;
    /** Récap : échecs livraison */
    failedOrderCount?: number;
    /** Récap : montant total des commandes */
    totalTourAmount?: number;
    /** Note / commentaire (affiché en modification si présent). */
    notes?: string;
    /** Motif d'annulation (DeliveryTour.cancellationReason) si statut ANNULEE. */
    cancellationReason?: string;
}

/** Réponse GET /api/logistics/employee-order/{id} (détails commande + produits avec image) */
export interface EmployeeOrderDetails {
    orderNumber: string;
    validationDate: string;
    employeeName: string;
    status: string;
    /** Acteur ayant posé le statut courant (historique). */
    currentStatusChangedByName?: string | null;
    /** Date/heure du passage au statut courant (ISO). */
    currentStatusChangedAt?: string | null;
    /** Libellé du rôle de l'acteur. */
    currentStatusChangedByRole?: string | null;
    /** Nom complet du livreur ayant livré la commande (si disponible). */
    driverName?: string;
    /** Téléphone du livreur (Users.phone). */
    driverPhone?: string | null;
    /** Motif d'échec de livraison (si statut échec). */
    failureReason?: string | null;
    /** Statut avant annulation (historique * → ANNULEE), si disponible. */
    previousStatusLabel?: string | null;
    listProducts: Array<{
        image: string;
        name: string;
        categoryName: string;
        currentStock?: number;
        /** Quantité commandée pour ce produit */
        quantity?: number;
    }>;
}

/** Réponse GET /api/logistics/delivery-tours/stats */
export interface DeliveryTourStats {
    totalTours: number;
    assignedTours: number;
    inProgressTours: number;
    completedTours: number;
    cancelledTours: number;
}

/** Réponse paginée GET /api/logistics/delivery-tours */
export interface DeliveryTourListResponse {
    content: DeliveryTourListItem[];
    totalElements: number;
    totalPages: number;
    currentPage: number;
    pageSize: number;
    hasNext: boolean;
    hasPrevious: boolean;
}

