package com.example.coopachat.dtos;

import com.example.coopachat.enums.UserRole;

//DTO pour l'objet utilisateur
public class UserDto {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private UserRole role;
    private Boolean isActive;

}
