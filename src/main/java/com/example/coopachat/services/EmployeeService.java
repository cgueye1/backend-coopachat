package com.example.coopachat.services;

import com.example.coopachat.dtos.CreateEmployeeDTO;


public interface EmployeeService {

    /**
     * Ajoute un nouveau salarié à une entreprise
     * Crée un utilisateur avec le rôle EMPLOYEE, l'associe à une entreprise,
     * génère un token d'invitation et envoie un email d'activation au salarié
     *
     * @param employee Les informations du salarié à créer
     * @throws RuntimeException si l'email ou le téléphone existe déjà, si l'entreprise n'existe pas ...

     */
    void addEmployee(CreateEmployeeDTO employee);

    /**
     * Active le compte d'un salarié et crée son mot de passe via le token d'invitation
     *
     * @param token Le token d'invitation reçu par email
     * @param newPassword Le nouveau mot de passe à définir
     * @param confirmPassword La confirmation du nouveau mot de passe
     * @throws RuntimeException si le token est invalide, expiré, si les mots de passe ne correspondent pas, ou si l'utilisateur n'existe pas
     */
    void activateEmployeeAccount(String token, String newPassword, String confirmPassword);
}
