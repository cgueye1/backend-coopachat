import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Product {
    id: string;
    name: string;
    reference: string;
    category: string;
    price: string;
    stock: number;
    updatedAt: string;
    status: 'Actif' | 'Inactif';
    icon: string;
    description?: string;
}

@Injectable({
    providedIn: 'root'
})
export class ProductService {
    private apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) { }

    // Lister les produits (paginé + filtres)
    getProducts(
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
        if (status !== undefined && status !== null) {
            params = params.set('status', String(status));
        }

        return this.http.get(`${this.apiUrl}/admin/products`, { params });
    }

    // Statistiques catalogue
    getProductStats(): Observable<any> {
        return this.http.get(`${this.apiUrl}/admin/products/stats`);
    }

    // Lister les catégories
    getCategories(): Observable<any> {
        return this.http.get(`${this.apiUrl}/admin/categories`);
    }

    // Détails d'un produit
    getProductDetails(productId: string): Observable<any> {
        return this.http.get(`${this.apiUrl}/admin/products/${productId}`);
    }

    // Créer un produit (multipart/form-data)
    createProduct(formData: FormData): Observable<any> {
        return this.http.post(
            `${this.apiUrl}/admin/products`,
            formData,
            { responseType: 'text' as 'json' }
        );
    }

    // Modifier un produit (multipart/form-data)
    updateProduct(productId: string, formData: FormData): Observable<any> {
        return this.http.put(
            `${this.apiUrl}/admin/products/${productId}`,
            formData,
            { responseType: 'text' as 'json' }
        );
    }

    // Activer / désactiver un produit
    updateProductStatus(productId: string, status: boolean): Observable<any> {
        return this.http.patch(
            `${this.apiUrl}/admin/products/${productId}/status`,
            { status },
            { responseType: 'text' as 'json' }
        );
    }

    // Exporter la liste des produits
    exportProducts(
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
        if (status !== undefined && status !== null) {
            params = params.set('status', String(status));
        }

        return this.http.get(`${this.apiUrl}/admin/products/export`, {
            params,
            responseType: 'blob'
        });
    }
}
