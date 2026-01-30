package com.example.coopachat.dtos.order;

import com.example.coopachat.dtos.products.ProductDetailsDTO;
import com.example.coopachat.dtos.products.ProductPreviewDTO;
import com.example.coopachat.entities.OrderItem;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;

//DTO pour afficher les détails d'un produit
@AllArgsConstructor
public class OrderItemDetailsDTO {

    private String orderNumber;
    private LocalDate validationDate;
    private String employeeName;
    private String status;
    private List<ProductPreviewDTO> listProducts;
}
