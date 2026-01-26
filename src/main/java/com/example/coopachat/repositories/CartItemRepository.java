package com.example.coopachat.repositories;

import com.example.coopachat.entities.CartItem;
import com.example.coopachat.entities.Product;
import com.example.coopachat.entities.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {


    Optional<CartItem> findByUserAndProduct(Users user, Product product);
    List<CartItem> findByUser (Users user);
}
