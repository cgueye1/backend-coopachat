package com.example.coopachat.services;

import com.example.coopachat.dtos.CreateCompanyDTO;
import com.example.coopachat.dtos.CreateEmployeeDTO;

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
     * Crée un nouvel employé et envoie une invitation par email
     *
     * @param createEmployeeDTO Les informations de l'employé à créer
     * @throws RuntimeException si l'entreprise n'existe pas ou si une erreur survient
     */
    void createEmployee(CreateEmployeeDTO createEmployeeDTO);


}

