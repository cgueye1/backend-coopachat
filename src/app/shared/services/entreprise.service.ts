import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

/** Données commandes par mois (évolution des commandes). */
export interface CommandesParMoisDTO {
    mois: string;
    nbCommandes: number;
}

/** KPIs du tableau de bord entreprise (GET /api/entreprise/dashboard/kpis). */
export interface CompanyDashboardKpisDTO {
    totalEmployees: number;
    activeEmployees: number;
    inactiveEmployees: number;
    ordersThisMonth: number;
    activeEmployeesRatio: string;
    evolutionCommandes: CommandesParMoisDTO[];
}

@Injectable({
    providedIn: 'root'
})
export class EntrepriseService {
  
    private apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) { }

    /** Récupère les KPIs pour le dashboard de l'entreprise. */
    getDashboardKpis(): Observable<CompanyDashboardKpisDTO> {
        return this.http.get<CompanyDashboardKpisDTO>(`${this.apiUrl}/entreprise/dashboard/kpis`);
    }

    /** 
     * Récupère la liste des salariés de l'entreprise. 
     * (Prêt pour la page "Mes salariés")
     */
    getEmployees(
        page: number,
        size: number,
        search?: string,
        isActive?: boolean
    ): Observable<any> {
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());

        if (search?.trim()) params = params.set('search', search.trim());
        if (isActive !== undefined && isActive !== null) params = params.set('isActive', String(isActive));

        return this.http.get(`${this.apiUrl}/entreprise/employees`, { params });
    }
}
