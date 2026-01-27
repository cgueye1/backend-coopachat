package com.example.coopachat.repositories;

import com.example.coopachat.entities.Address;
import com.example.coopachat.entities.Employee;
import com.example.coopachat.enums.DeliveryMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository <Address,Long> {

    //Vérifier s'il existe déjà une adresse de ce type pour un utilisateur
    boolean existsByEmployeeAndDeliveryMode (Employee employee, DeliveryMode deliveryMode);

    //Retourner le nombre d'adresse de livraison du salarié
    Long countByEmployee (Employee employee);

    //Vérifier s'il existe déjà une adresse de ce type pour un utilisateur en excluant l'adresse actuelle
    boolean existsByEmployeeAndDeliveryModeAndIdNot(Employee employee, DeliveryMode deliveryMode, Long id);

    //Retourner les adresses de l'employé
    List <Address> findByEmployee(Employee employee);
}
