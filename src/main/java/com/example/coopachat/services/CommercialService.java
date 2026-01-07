package com.example.coopachat.services;

import com.example.coopachat.dtos.CreateCompanyDTO;
import com.example.coopachat.dtos.CreateEmployeeDTO;
import com.example.coopachat.dtos.CompanyListResponseDTO;

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
     * Crée un nouvel employé et envoie une invitation par email
     *
     * @param createEmployeeDTO Les informations de l'employé à créer
     * @throws RuntimeException si l'entreprise n'existe pas ou si une erreur survient
     */
    void createEmployee(CreateEmployeeDTO createEmployeeDTO);

    


}

