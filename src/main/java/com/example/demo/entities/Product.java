package com.example.demo.entities;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Data
@ToString
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")

@Entity
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name cannot be empty")
    private String name;

    private String description;

    @Positive(message = "Price must be greater than zero")
    private double price;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Positive(message = "Quantity must be greater than zero")
    private int quantity;

    private String brand;

    private String model;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private boolean active = true;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSpecification> specifications = new ArrayList<>();

    @OneToMany(mappedBy = "product")
    private List<Review> reviews = new ArrayList<>();

    // Method to add image
    public void addImage(String imageUrl) {
        ProductImage image = new ProductImage(this, imageUrl);
        images.add(image);
    }

    // Method to add specification
    public void addSpecification(String name, String value) {
        ProductSpecification spec = new ProductSpecification(this, name, value);
        specifications.add(spec);
    }

    public Product(String name, String description, double price, Category category,
                   int quantity, String brand, String model) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.quantity = quantity;
        this.brand = brand;
        this.model = model;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Legacy constructor for backward compatibility
    public Product(String name, String description, double price, String image,
                   int category_id, int quantity, String brand, String model) {
        this(name, description, price, new Category(), quantity, brand, model);
        if (image != null && !image.isEmpty()) {
            this.addImage(image);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}