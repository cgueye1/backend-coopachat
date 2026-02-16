package com.example.coopachat.services.geocoding;

import com.example.coopachat.entities.Coordinates;

public interface GeocodingService {
    //Retourner les coordonnées d'une adresse
    Coordinates  getCoordinatesFromAddress (String address);
}
