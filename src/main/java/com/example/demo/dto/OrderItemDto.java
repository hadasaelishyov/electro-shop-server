package com.example.demo.dto;

import lombok.Data;

@Data

public class OrderItemDto {
    private Long id;
    private int quantity;
    private double unitPrice;
    private double totalAmount;
    private ProductDto product;
}
