package com.example.coopachat.services.commercial;

import com.example.coopachat.dtos.companies.*;
import com.example.coopachat.dtos.coupons.CartTotalCouponStatsDTO;
import com.example.coopachat.dtos.coupons.CouponDetailsDTO;
import com.example.coopachat.dtos.coupons.CouponListResponseDTO;
import com.example.coopachat.dtos.coupons.CreateCouponDTO;
import com.example.coopachat.dtos.coupons.UpdateCouponStatusDTO;
import com.example.coopachat.dtos.employees.*;
import com.example.coopachat.enums.CompanySector;
import com.example.coopachat.enums.CouponScope;
import com.example.coopachat.enums.CouponStatus;

/**
 * Interface pour le service de gestion des actions du commercial
 */
public interface CommercialService {

    /**
     * Crée une nouvelle entreprise associée au commercial connecté
     *
     * @param createCompanyDTO Les informations de l'entreprise à créer
     * @throws RuntimeException si le commercial n'existe pas ou si une erreur survient
     */
    void createCompany(CreateCompanyDTO createCompanyDTO);

    /**
     * Récupère la liste paginée de toutes les entreprises créées par le commercial connecté
     * avec possibilité de recherche et filtres
     *
     * @param page Numéro de la page (0-indexed, par défaut 0)
     * @param size Taille de la page (par défaut 6)
     * @param search Terme de recherche pour le nom de l'entreprise (optionnel, recherche partielle insensible à la casse)
     * @param sector Filtre par secteur d'activité (optionnel)
     * @param isActive Filtre par statut actif/inactif (optionnel, true pour actives, false pour inactives)
     * @return Réponse paginée contenant la liste des entreprises et les métadonnées de pagination
     * @throws RuntimeException si le commercial n'existe pas ou si une erreur survient
     */
    CompanyListResponseDTO getAllCompanies(int page, int size, String search, CompanySector sector, Boolean isActive);

    /**
     * Récupère les détails d'une entreprise spécifique par son ID
     *
     * @param id L'identifiant de l'entreprise
     * @return Les détails complets de l'entreprise
     * @throws RuntimeException si l'entreprise n'existe pas, n'appartient pas au commercial connecté, ou si une erreur survient
     */
    CompanyDetailsDTO getCompanyById(Long id);

    /**
     * Met à jour une entreprise existante
     *
     * @param id L'identifiant de l'entreprise à modifier
     * @param updateCompanyDTO Les nouvelles informations de l'entreprise
     * @throws RuntimeException si l'entreprise n'existe pas, n'appartient pas au commercial connecté, ou si une erreur survient
     */
    void updateCompany(Long id, UpdateCompanyDTO updateCompanyDTO);

    /**
     * Active ou désactive une entreprise
     *
     * @param id L'identifiant de l'entreprise
     * @param updateCompanyStatusDTO Le nouveau statut actif/inactif
     * @throws RuntimeException si l'entreprise n'existe pas, n'appartient pas au commercial connecté, ou si une erreur survient
     */
    void updateCompanyStatus(Long id, UpdateCompanyStatusDTO updateCompanyStatusDTO);

    /**
     * Téléverse le logo d'une entreprise (JPG, PNG, max 5 Mo).
     *
     * @param id L'identifiant de l'entreprise
     * @param file Fichier image
     */
    void uploadCompanyLogo(Long id, org.springframework.web.multipart.MultipartFile file);

    /**
     * Récupère les statistiques des entreprises du commercial connecté
     *
     * @return Les statistiques (total, actives, inactives)
     * @throws RuntimeException si le commercial n'existe pas ou si une erreur survient
     */
    CompanyStatsDTO getCompanyStats();

    /**
     * Crée un nouvel employé et envoie une invitation par email
     *
     * @param createEmployeeDTO Les informations de l'employé à créer
     * @throws RuntimeException si l'entreprise n'existe pas ou si une erreur survient
     */
    void createEmployee(CreateEmployeeDTO createEmployeeDTO);

    /**
     * Récupère la liste paginée de tous les employés créés par le commercial connecté
     * avec possibilité de recherche et filtres
     *
     * @param page Numéro de la page (0-indexed, par défaut 0)
     * @param size Taille de la page (par défaut 6)
     * @param search Terme de recherche pour le prénom ou nom de l'employé (optionnel, recherche partielle insensible à la casse)
     * @param companyId Filtre par entreprise (optionnel, ID de l'entreprise)
     * @param isActive Filtre par statut actif/inactif (optionnel, true pour actifs, false pour inactifs)
     * @return Réponse paginée contenant la liste des employés et les métadonnées de pagination
     * @throws RuntimeException si le commercial n'existe pas ou si une erreur survient
     */
    EmployeeListResponseDTO getAllEmployees(int page, int size, String search, Long companyId, Boolean isActive);

    /**
     * Récupère les statistiques des employés du commercial connecté
     *
     * @return Les statistiques (total, actifs, en attente d'activation)
     * @throws RuntimeException si le commercial n'existe pas ou si une erreur survient
     */
    EmployeeStatsDTO getEmployeeStats();

    /**
     * Récupère les détails d'un employé spécifique par son ID
     *
     * @param id L'identifiant de l'employé
     * @return Les détails complets de l'employé
     * @throws RuntimeException si l'employé n'existe pas, n'appartient pas au commercial connecté, ou si une erreur survient
     */
    EmployeeDetailsDTO getEmployeeById(Long id);

    /**
     * Met à jour un employé existant
     *
     * @param id L'identifiant de l'employé à modifier
     * @param updateEmployeeDTO Les nouvelles informations de l'employé
     * @throws RuntimeException si l'employé n'existe pas, n'appartient pas au commercial connecté, si l'email ou le téléphone existe déjà, ou si une erreur survient
     */
    void updateEmployee(Long id, UpdateEmployeeDTO updateEmployeeDTO);

    /**
     * Active ou désactive un employé
     *
     * @param id L'identifiant de l'employé
     * @param updateEmployeeStatusDTO Le nouveau statut actif/inactif
     * @throws RuntimeException si l'employé n'existe pas, n'appartient pas au commercial connecté, ou si une erreur survient
     */
    void updateEmployeeStatus(Long id, UpdateEmployeeStatusDTO updateEmployeeStatusDTO);

    /**
     * Crée un coupon et l'applique selon son scope.
     *
     * @param createCouponDTO Les informations du coupon à créer
     * @throws RuntimeException si le code/nom existe déjà, si les dates sont invalides,
     *                          ou si les produits/catégories requis sont absents.
     */
    void addCoupon(CreateCouponDTO createCouponDTO);

    /**
     * Active ou désactive un coupon.
     *
     * @param id L'identifiant du coupon
     * @param updateCouponStatusDTO Nouveau statut d'activation
     */
    void updateCouponStatus(Long id, UpdateCouponStatusDTO updateCouponStatusDTO);

    /**
     * Liste paginée des coupons avec recherche et filtres.
     *
     * @param page Numéro de la page (0-indexed, par défaut 0)
     * @param size Taille de la page (par défaut 6)
     * @param search Terme de recherche (code ou nom)
     * @param status Filtre par statut (optionnel)
     * @param scope Filtre par scope (optionnel)
     * @param isActive Filtre par activation manuelle (optionnel)
     * @return Réponse paginée contenant la liste des coupons
     */
    CouponListResponseDTO getAllCoupons(int page, int size, String search,
                                        CouponStatus status, CouponScope scope, Boolean isActive);

    /**
     * Récupère les détails d'un coupon avec ses produits liés.
     *
     * @param id L'identifiant du coupon
     * @return Détails du coupon
     */
    CouponDetailsDTO getCouponById(Long id);

    /**
     * Supprime un coupon après avoir délié les produits et catégories associés.
     *
     * @param id L'identifiant du coupon
     */
    void deleteCoupon(Long id);

    /**
     * Statistiques des coupons "panier" (scope CART_TOTAL uniquement) :
     * nombre de coupons actifs et nombre total d'utilisations.
     */
    CartTotalCouponStatsDTO getCartTotalCouponStats();

}

