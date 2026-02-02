package com.example.coopachat.dtos.DeliveryDriver;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailableDriverDTO {
    private String fullName; // user.getFirstName() + " " + user.getLastName()
}