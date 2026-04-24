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
  getSuppliers(params: { page: number; size: number; search?: string; sectorId?: number; status?: boolean }): Observable<SupplierListResponseDTO> {
    let httpParams = new HttpParams()
      .set('page', params.page.toString())
      .set('size', params.size.toString());
    
    if (params.search && params.search.trim() !== '') {
      httpParams = httpParams.set('search', params.search.trim());
    }
    if (params.sectorId) {
      httpParams = httpParams.set('sectorId', params.sectorId.toString());
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
  createSupplier(dto: CreateSupplierDTO): Observable<SupplierDetailsDTO> {
    return this.http.post<SupplierDetailsDTO>(this.apiUrl, dto);
  }

  /**
   * Mise à jour d'un fournisseur
   */
  updateSupplier(id: number, dto: UpdateSupplierDTO): Observable<SupplierDetailsDTO> {
    return this.http.put<SupplierDetailsDTO>(`${this.apiUrl}/${id}`, dto);
  }

  /**
   * Activation/Désactivation d'un fournisseur
   */
  updateStatus(id: number, dto: UpdateSupplierStatusDTO): Observable<void> {
    return this.http.patch<void>(`${this.apiUrl}/${id}/status`, dto);
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
