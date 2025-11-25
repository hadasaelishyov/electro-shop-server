package com.example.demo.entities;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Data
@ToString

@Entity
@Table(name = "orders")
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")

public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private LocalDate orderDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private double totalAmount;

    // Shipping details
    private String shippingAddress;
    private String shippingCity;
    private String shippingZipCode;
    private String shippingCountry;


    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)

    private List<OrderItem> orderItems = new ArrayList<>();


    public Order(User user, LocalDate orderDate) {
        this.user = user;
        this.orderDate = orderDate;
        this.totalAmount = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // Initialize shipping address from user if available
        if (user != null && user.getAddress() != null) {
            this.shippingAddress = user.getAddress();
        }
    }



    // Method to calculate total amount
    public void calculateTotalAmount() {
        this.totalAmount = orderItems.stream()
                .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                .sum();
    }

    // Method to create order from cart
    public static Order createFromCart(Cart cart, String shippingAddress) {
        if (cart == null || cart.getUser() == null) {
            throw new IllegalArgumentException("Cart or user cannot be null");
        }

        Order order = new Order(cart.getUser(), LocalDate.now());
        order.setShippingAddress(shippingAddress);

        // Convert cart items to order items
        for (CartItem cartItem : cart.getCartItems()) {
            OrderItem orderItem = new OrderItem(
                    order,
                    cartItem.getProduct(),
                    cartItem.getQuantity(),
                    cartItem.getUnitPrice()
            );
            order.getOrderItems().add(orderItem);
        }

        order.calculateTotalAmount();
        return order;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}