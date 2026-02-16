package com.example.coopachat.services.geocoding;

import com.example.coopachat.dtos.geocoding.PlaceDetailsResult;

public interface PlacesService {
    //Récupère les détails d'un lieu
    PlaceDetailsResult getPlaceDetails(String placeId);//placeId est l'ID du lieu
}