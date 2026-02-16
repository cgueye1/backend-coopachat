package com.example.coopachat.dtos.geocoding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceDetailsResult {
   private String formattedAddress;//Adresse formatée
    private BigDecimal latitude;//Latitude
    private BigDecimal longitude;//Longitude
    private String placeId;//ID du lieu

}
