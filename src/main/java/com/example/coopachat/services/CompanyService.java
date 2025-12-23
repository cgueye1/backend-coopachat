package com.example.coopachat.services;

import com.example.coopachat.dtos.CreateCompanyDTO;

/**
 * Interface pour le service de gestion des entreprises
 */
public interface CompanyService {

    /**
     * Crée une nouvelle entreprise associée au commercial connecté
     *
     * @param createCompanyDTO Les informations de l'entreprise à créer
     * @throws RuntimeException si le commercial n'existe pas ou si une erreur survient
     */
    void createCompany(CreateCompanyDTO createCompanyDTO);
}

