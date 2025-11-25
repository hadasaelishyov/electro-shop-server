package com.example.demo.dto;

import lombok.Data;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

@Data

public class ProductDto {
    private Long id;
    private String name;
    private String description;
    private double price;
    private String brand;
    private String model;
    private List<ProductImageDto> images;
    private int quantity;
    private boolean active = true;
    private Long categoryId;
    private List<ProductSpecificationDto> specifications;

}
