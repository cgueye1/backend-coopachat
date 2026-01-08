package com.example.coopachat.services;

import com.example.coopachat.dtos.RegisterDriverRequestDTO;

/**
 * Interface pour le service de gestion des actions du Responsable Logistique
 */
public interface LogisticsManagerService {

    /**
     * Crée un nouveau livreur et envoie une invitation par email
     *
     * @param driverDTO Les informations du livreur à créer
     * @throws RuntimeException si l'email ou le téléphone existe déjà ou si une erreur survient
     */
    void createDriver(RegisterDriverRequestDTO driverDTO);
}


