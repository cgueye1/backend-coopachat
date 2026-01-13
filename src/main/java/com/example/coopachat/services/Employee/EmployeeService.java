package com.example.coopachat.services.Employee;

public interface EmployeeService {


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