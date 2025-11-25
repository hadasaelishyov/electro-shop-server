package com.example.demo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
@Data

public class OrderDto {
    private Long id;
    private String shippingAddress;
    private String shippingCity;
    private String shippingZipCode;
    private String shippingCountry;
    private double totalAmount;
    private LocalDateTime createdAt;
    private UserDto user;
    private List<OrderItemDto> orderItems;
}

