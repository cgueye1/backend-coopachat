import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { 
  SupplierListResponseDTO, 
  SupplierDetailsDTO, 
  CreateSupplierDTO, 
  UpdateSupplierDTO, 
  UpdateSupplierStatusDTO,
  SupplierStatsDTO
} from '../models/supplier.model';

@Injectable({
  providedIn: 'root'
})
export class SupplierService {
  private apiUrl = `${environment.apiUrl}/admin/suppliers`;

  constructor(private http: HttpClient) {}

  /**
   * Liste paginée des fournisseurs avec recherche optionnelle
   */
  getSuppliers(params: { page: number; size: number; search?: string; categoryId?: number; status?: boolean }): Observable<SupplierListResponseDTO> {
    let httpParams = new HttpParams()
      .set('page', params.page.toString())
      .set('size', params.size.toString());
    
    if (params.search && params.search.trim() !== '') {
      httpParams = httpParams.set('search', params.search.trim());
    }
    if (params.categoryId) {
      httpParams = httpParams.set('categoryId', params.categoryId.toString());
    }
    if (params.status !== undefined) {
      httpParams = httpParams.set('status', params.status.toString());
    }

    return this.http.get<SupplierListResponseDTO>(`${this.apiUrl}/paged`, { params: httpParams });
  }

  /**
   * Détails d'un fournisseur
   */
  getSupplierById(id: number): Observable<SupplierDetailsDTO> {
    return this.http.get<SupplierDetailsDTO>(`${this.apiUrl}/${id}`);
  }

  /**
   * Création d'un fournisseur
   */
  createSupplier(dto: CreateSupplierDTO): Observable<string> {
    return this.http.post(this.apiUrl, dto, { responseType: 'text' });
  }

  /**
   * Mise à jour d'un fournisseur
   */
  updateSupplier(id: number, dto: UpdateSupplierDTO): Observable<string> {
    return this.http.put(`${this.apiUrl}/${id}`, dto, { responseType: 'text' });
  }

  /**
   * Activation/Désactivation d'un fournisseur
   */
  updateStatus(id: number, dto: UpdateSupplierStatusDTO): Observable<string> {
    return this.http.patch(`${this.apiUrl}/${id}/status`, dto, { responseType: 'text' });
  }

  /**
   * Statistiques des fournisseurs
   */
  getStats(): Observable<SupplierStatsDTO> {
    return this.http.get<SupplierStatsDTO>(`${this.apiUrl}/stats`);
  }

  /**
   * Export Excel des fournisseurs (À implémenter côté backend si besoin, ou simuler)
   */
  exportSuppliers(search?: string): Observable<Blob> {
    // Note: This endpoint doesn't exist yet in the backend, but we keep the structure
    let httpParams = new HttpParams();
    if (search && search.trim() !== '') {
      httpParams = httpParams.set('search', search.trim());
    }
    return this.http.get(`${this.apiUrl}/export`, {
      params: httpParams,
      responseType: 'blob'
    });
  }
}
