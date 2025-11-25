package com.example.demo.services;

import com.example.demo.entities.Order;
import com.example.demo.entities.OrderItem;
import com.example.demo.entities.Product;
import com.example.demo.exceptions.InvalidOrderStateException;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.OrderItemRepo;
import com.example.demo.repositories.OrderRepo;
import com.example.demo.repositories.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing order items in the e-commerce system.
 * Handles order item creation, updates, and management.
 */
@Service
public class OrderItemService {
    @Autowired
    private OrderItemRepo orderItemRepo;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private ProductRepo productRepo;

    /**
     * Get all order items (admin function)
     */
    public List<OrderItem> getAll() {
        return orderItemRepo.findAll();
    }

    /**
     * Get order item by ID
     */
    public Optional<OrderItem> getById(Long id) {
        return orderItemRepo.findById(id);
    }

    /**
     * Get order items by order ID
     */
    public List<OrderItem> getByOrderId(Long orderId) {
        return orderItemRepo.findByOrderId(orderId);
    }

    /**
     * Get order items by product ID
     */
    public List<OrderItem> getByProductId(Long productId) {
        return orderItemRepo.findByProductId(productId);
    }

    /**
     * Get top selling products based on order items
     */
    public List<Object[]> getTopSellingProducts(int limit) {
        return orderItemRepo.findTopSellingProducts(limit);
    }

    /**
     * Add a new order item with validation
     */
    @Transactional
    public OrderItem add(OrderItem item) {
        // Validate the order item
        if (item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (item.getOrder() == null) {
            throw new IllegalArgumentException("Order cannot be null");
        }

        if (item.getProduct() == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }

        // Check if order exists
        Order order = orderRepo.findById(item.getOrder().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + item.getOrder().getId()));


        // Check if product exists
        Product product = productRepo.findById(item.getProduct().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + item.getProduct().getId()));

        // Set unit price from product if not set
        if (item.getUnitPrice() <= 0) {
            item.setUnitPrice(product.getPrice());
        }

        OrderItem savedItem = orderItemRepo.save(item);

        // Recalculate order total
        order.calculateTotalAmount();
        orderRepo.save(order);

        return savedItem;
    }

    /**
     * Update an existing order item with validation
     */
    @Transactional
    public OrderItem update(Long id, OrderItem updatedItem) {
        OrderItem existingItem = orderItemRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found with id: " + id));

        Order order = existingItem.getOrder();


        // Validate quantity
        if (updatedItem.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        // Update fields
        existingItem.setQuantity(updatedItem.getQuantity());

        if (updatedItem.getUnitPrice() > 0) {
            existingItem.setUnitPrice(updatedItem.getUnitPrice());
        }

        OrderItem savedItem = orderItemRepo.save(existingItem);

        // Recalculate order total
        order.calculateTotalAmount();
        orderRepo.save(order);

        return savedItem;
    }

    /**
     * Update order item quantity
     */
    @Transactional
    public OrderItem updateQuantity(Long id, int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        OrderItem existingItem = orderItemRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found with id: " + id));

        Order order = existingItem.getOrder();



        // Update quantity
        existingItem.setQuantity(newQuantity);
        OrderItem savedItem = orderItemRepo.save(existingItem);

        // Recalculate order total
        order.calculateTotalAmount();
        orderRepo.save(order);

        return savedItem;
    }

    /**
     * Delete an order item
     */
    @Transactional
    public void delete(Long id) {
        OrderItem item = orderItemRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order item not found with id: " + id));

        Order order = item.getOrder();



        orderItemRepo.deleteById(id);

        // Recalculate order total
        order.calculateTotalAmount();
        orderRepo.save(order);
    }

    /**
     * Get total sales by product
     */
    public List<Object[]> getTotalSalesByProduct() {
        return orderItemRepo.calculateTotalSalesByProduct();
    }

    /**
     * Get items sold in a specific date range
     */
    public List<OrderItem> getItemsSoldInDateRange(java.time.LocalDate startDate, java.time.LocalDate endDate) {
        return orderItemRepo.findByOrderOrderDateBetween(startDate, endDate);
    }
}