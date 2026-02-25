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
 * Réponse de GET /api/admin/dashboard/stats?periode=TODAY|THIS_MONTH
 * Les 3 KPIs + la liste pour le graphique "Paiements par statut".
 */
export interface AdminDashboardStatsDTO {
  commandesEnAttente: number;
  paiementsEchoues: number;
  reclamationsOuvertes: number;
  paiementsParStatut: PaymentStatusItemDTO[];
}

/**
 * Un jour du graphique "Commandes vs Livraisons" (7 derniers jours).
 * Livré = LIVREE, En attente = EN_ATTENTE.
 */
export interface CommandesVsLivraisonsDayDTO {
  date: string;
  commandesEnAttente: number;
  livraisons: number;
}

/**
 * Réponse de GET /api/admin/dashboard/commandes-vs-livraisons
 */
export interface CommandesVsLivraisonsDTO {
  derniersJours: CommandesVsLivraisonsDayDTO[];
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = environment.apiUrl;//URL de base

  constructor(private http: HttpClient) {}

  /**
   * Récupère les stats du tableau de bord admin (3 KPIs + paiements par statut).
   * @param periode "TODAY" (aujourd'hui) ou "THIS_MONTH" (mois en cours)
   */
  getDashboardStats(periode: 'TODAY' | 'THIS_MONTH' = 'THIS_MONTH'): Observable<AdminDashboardStatsDTO> {
    const params = new HttpParams().set('periode', periode);//le paramètre à passer 
    return this.http.get<AdminDashboardStatsDTO>(`${this.apiUrl}/admin/dashboard/stats`, { params });
  }
}
