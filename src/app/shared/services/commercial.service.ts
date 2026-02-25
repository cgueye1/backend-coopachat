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

// Structure des statistiques renvoyees par /commercial/employees/stats
export interface EmployeeStats {
    totalEmployees: number;
    activeEmployees: number;
    pendingEmployees: number;
}

@Injectable({
    providedIn: 'root'
})
export class CommercialService {
    // ==================================================
    // SECTION 1 : VARIABLES GLOBALES ET INITIALISATION
    // ==================================================

    // URL de base de l'API (definie dans environment.ts)
    private apiUrl = environment.apiUrl;

    // Constructeur : injection du client HTTP
    constructor(private http: HttpClient) { }

    // ==================================================
    // SECTION 2 : METHODES PRINCIPALES (CRUD + ACTIONS)
    // ==================================================

    /** ==================================================
     *  METHODE : LISTER LES ENTREPRISES
     *  ================================================== */
    /**
     * But : Recuperer la liste paginee avec filtres
     * Resultat attendu : Une reponse API avec contenu + pagination
     * Etapes :
     * 1. Construire les parametres
     * 2. Ajouter les filtres si presents
     * 3. Appeler l'API GET
     */
    getCompanies(
        page: number,
        size: number,
        search?: string,
        sector?: string,
        isActive?: boolean
    ): Observable<any> {
        // Preparer les parametres de requete
        let params = new HttpParams()
            .set('page', page.toString())
            .set('size', size.toString());

        // Ajouter la recherche si elle existe
        if (search && search.trim()) {
            params = params.set('search', search.trim());
        }

        // Ajouter le secteur si selectionne
        if (sector) {
            params = params.set('sector', sector);
        }

        // Ajouter le statut actif/inactif si selectionne
        if (isActive !== undefined && isActive !== null) {
            params = params.set('isActive', String(isActive));
        }

        // Appeler l'API et retourner la reponse
        return this.http.get(`${this.apiUrl}/commercial/companies`, { params });
    }

    /** ==================================================
     *  METHODE : LIRE LES STATISTIQUES ENTREPRISES
     *  ================================================== */
    /**
     * But : Recuperer les compteurs pour les cartes du haut
     * Resultat attendu : total / actif / inactif
     */
    getCompanyStats(): Observable<CompanyStats> {
        // Appeler l'API de statistiques
        return this.http.get<CompanyStats>(`${this.apiUrl}/commercial/companies/stats`);
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
    createCompany(payload: {
        name: string;
        sector?: string;
        location: string;
        contactName: string;
        contactEmail?: string;
        contactPhone: string;
        status: string;
        note?: string;
    }): Observable<any> {
        // Appeler l'API de creation
        return this.http.post(
            `${this.apiUrl}/commercial/companies`,
            payload,
            {
                responseType: 'text' as 'json'
            }
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
        sector?: string;
        location: string;
        contactName: string;
        contactEmail?: string;
        contactPhone: string;
        status: string;
        note?: string;
    }): Observable<any> {
        // Appeler l'API de mise a jour
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
}

