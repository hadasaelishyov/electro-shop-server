package com.example.demo.dto;

import com.example.demo.entities.*;
import lombok.*;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderDto toDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setShippingAddress(order.getShippingAddress());
        dto.setShippingCity(order.getShippingCity());
        dto.setShippingZipCode(order.getShippingZipCode());
        dto.setShippingCountry(order.getShippingCountry());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUser(toDto(order.getUser()));
        dto.setOrderItems(order.getOrderItems().stream()
                .map(this::toDto)
                .collect(Collectors.toList()));
        return dto;
    }

    public OrderItemDto toDto(OrderItem item) {
        OrderItemDto dto = new OrderItemDto();
        dto.setId(item.getId());
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setTotalAmount(item.getTotalPrice());
        dto.setProduct(toDto(item.getProduct()));
        return dto;
    }

    public ProductDto toDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setActive(product.isActive());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setBrand(product.getBrand());
        dto.setModel(product.getModel());
        dto.setImages(product.getImages().stream()
                .map(this::toDto)
                .collect(Collectors.toList()));
        dto.setSpecifications(product.getSpecifications().stream()
                .map(this::toDto)
                .collect(Collectors.toList()));
        dto.setQuantity(product.getQuantity());
        dto.setCategoryId(product.getCategory().getId());
        return dto;
    }

    public ProductImageDto toDto(ProductImage image) {
        ProductImageDto dto = new ProductImageDto();
        dto.setImageUrl(image.getImageUrl());
        dto.setId(image.getId());
        dto.setMain(image.isMain());
        return dto;
    }
    public ProductSpecificationDto toDto(ProductSpecification specification) {
        ProductSpecificationDto dto = new ProductSpecificationDto();
        dto.setSpecName(specification.getSpecName());
        dto.setSpecValue(specification.getSpecValue());
        return dto;
    }

    public UserDto toDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        return dto;
    }

    public OrderPaymentDto toDto(Payment payment) {
        if (payment == null) return null;
        OrderPaymentDto dto = new OrderPaymentDto();
        dto.setId(payment.getId());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setPaymentStatus(payment.getPaymentStatus());
        dto.setTransactionId(payment.getTransactionId());
        return dto;
    }
}
