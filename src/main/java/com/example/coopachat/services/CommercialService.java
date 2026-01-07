package com.example.coopachat.services;

import com.example.coopachat.dtos.CreateCompanyDTO;
import com.example.coopachat.dtos.CreateEmployeeDTO;
import com.example.coopachat.dtos.CompanyListResponseDTO;
import com.example.coopachat.dtos.CompanyDetailsDTO;
import com.example.coopachat.dtos.UpdateCompanyDTO;
import com.example.coopachat.dtos.UpdateCompanyStatusDTO;

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
     *
     * @param page Numéro de la page (0-indexed, par défaut 0)
     * @param size Taille de la page (par défaut 6)
     * @return Réponse paginée contenant la liste des entreprises et les métadonnées de pagination
     * @throws RuntimeException si le commercial n'existe pas ou si une erreur survient
     */
    CompanyListResponseDTO getAllCompanies(int page, int size);

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
     * Crée un nouvel employé et envoie une invitation par email
     *
     * @param createEmployeeDTO Les informations de l'employé à créer
     * @throws RuntimeException si l'entreprise n'existe pas ou si une erreur survient
     */
    void createEmployee(CreateEmployeeDTO createEmployeeDTO);

    


}

