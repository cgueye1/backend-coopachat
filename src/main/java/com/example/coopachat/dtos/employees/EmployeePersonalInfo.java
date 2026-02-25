package com.example.coopachat.dtos.employees;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO pour l'affichage des infos personnelles de l'employé
 */
@Data
@AllArgsConstructor
public class EmployeePersonalInfo{


    private String firstName;
    private String lastName;
    private String phone;
 }