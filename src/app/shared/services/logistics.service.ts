import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
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
            validationDate: string;
            products: string[];
            deliveryFrequency: string | null;
            status: string;
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
                validationDate: string;
                products: string[];
                deliveryFrequency: string | null;
                status: string;
            }>;
            totalElements: number;
            totalPages: number;
            currentPage: number;
            pageSize: number;
            hasNext: boolean;
            hasPrevious: boolean;
        }>(`${this.apiUrl}/logistics/employee-orders`, { params });
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

    /** Chauffeurs disponibles (pour planifier une tournée) */
    getAvailableDrivers(): Observable<AvailableDriver[]> {
        return this.http.get<AvailableDriver[]>(`${this.apiUrl}/logistics/delivery-tours/available-drivers`);
    }

    /** Créer une tournée de livraison */
    createDeliveryTour(payload: CreateDeliveryTourPayload): Observable<string> {
        return this.http.post(
            `${this.apiUrl}/logistics/delivery-tours`,
            payload,
            { responseType: 'text' }
        ) as Observable<string>;
    }
}

export interface EligibleOrder {
    orderId: number;
    orderNumber: string;
    customerName: string;
    formattedAddress: string;
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

