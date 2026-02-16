package com.example.coopachat.entities;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

//Classe pour les coordonnées géographiques
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Coordinates {
    private BigDecimal latitude;//Latitude
    private BigDecimal longitude;//Longitude
}
