package com.example.demo.services;

import com.example.demo.entities.Cart;
import com.example.demo.entities.CartItem;
import com.example.demo.entities.Product;
import com.example.demo.exceptions.InsufficientInventoryException;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.CartItemRepo;
import com.example.demo.repositories.CartRepo;
import com.example.demo.repositories.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing cart items in the e-commerce system.
 * Handles cart item creation, updates, and management.
 */
@Service
public class CartItemService {
    @Autowired
    private CartItemRepo cartItemRepo;

    @Autowired
    private CartRepo cartRepo;

    @Autowired
    private ProductRepo productRepo;

    /**
     * Get all cart items (admin function)
     */
    public List<CartItem> getAll() {
        return cartItemRepo.findAll();
    }

    /**
     * Get cart item by ID
     */
    public Optional<CartItem> getById(Long id) {
        return cartItemRepo.findById(id);
    }

    public boolean existsByCartIdAndProductId(Long cartId, Long productId) {
        return cartItemRepo.existsByCartIdAndProductId(cartId, productId);
    }

    /**
     * Get cart items by cart ID
     */
    public List<CartItem> getByCartId(Long cartId) {
        return cartItemRepo.findByCartId(cartId);
    }

    /**
     * Get cart items by product ID
     */
    public List<CartItem> getByProductId(Long productId) {
        return cartItemRepo.findByProductId(productId);
    }

    /**
     * Add a new cart item with validation
     */
    @Transactional
    public CartItem add(CartItem item) {
        // Validate the cart item
        if (item.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (item.getCart() == null) {
            throw new IllegalArgumentException("Cart cannot be null");
        }

        if (item.getProduct() == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }

        // Check if cart exists
        Cart cart = cartRepo.findById(item.getCart().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with id: " + item.getCart().getId()));

        if (!cart.isActive()) {
            throw new IllegalStateException("Cannot add items to inactive cart");
        }

        // Check if product exists and has sufficient inventory
        Product product = productRepo.findById(item.getProduct().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + item.getProduct().getId()));

        if (!product.isActive()) {
            throw new IllegalStateException("Cannot add inactive product to cart");
        }

        if (product.getQuantity() < item.getQuantity()) {
            throw new InsufficientInventoryException(
                    "Not enough inventory for product: " + product.getName() +
                            ". Available: " + product.getQuantity() +
                            ", Requested: " + item.getQuantity());
        }

        // Set timestamps
        if (item.getCreatedAt() == null) {
            item.setCreatedAt(LocalDateTime.now());
        }
        item.setUpdatedAt(LocalDateTime.now());

        // Set unit price from product if not set
        if (item.getUnitPrice() <= 0) {
            item.setUnitPrice(product.getPrice());
        }

        return cartItemRepo.save(item);
    }

    /**
     * Update an existing cart item with validation
     */
    @Transactional
    public CartItem update(Long id, CartItem updatedItem) {
        CartItem existingItem = cartItemRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + id));

        // Validate quantity
        if (updatedItem.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        // Check inventory if quantity is being updated
        if (updatedItem.getQuantity() != existingItem.getQuantity()) {
            Product product = existingItem.getProduct();
            if (product.getQuantity() < updatedItem.getQuantity()) {
                throw new InsufficientInventoryException(
                        "Not enough inventory for product: " + product.getName() +
                                ". Available: " + product.getQuantity() +
                                ", Requested: " + updatedItem.getQuantity());
            }

            existingItem.setQuantity(updatedItem.getQuantity());
        }

        // Update other fields if provided
        if (updatedItem.getUnitPrice() > 0) {
            existingItem.setUnitPrice(updatedItem.getUnitPrice());
        }

        // Update timestamp
        existingItem.setUpdatedAt(LocalDateTime.now());

        return cartItemRepo.save(existingItem);
    }

    /**
     * Update cart item quantity with validation
     */
    @Transactional
    public CartItem updateQuantity(Long id, int newQuantity) {
        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        CartItem existingItem = cartItemRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + id));

        // Check inventory
        Product product = existingItem.getProduct();
        if (product.getQuantity() < newQuantity) {
            throw new InsufficientInventoryException(
                    "Not enough inventory for product: " + product.getName() +
                            ". Available: " + product.getQuantity() +
                            ", Requested: " + newQuantity);
        }

        // Update quantity and timestamp
        existingItem.setQuantity(newQuantity);
        existingItem.setUpdatedAt(LocalDateTime.now());

        // Also update the cart's updated timestamp
        Cart cart = existingItem.getCart();
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepo.save(cart);

        return cartItemRepo.save(existingItem);
    }

    /**
     * Delete a cart item
     */
    @Transactional
    public void delete(Long id) {
        CartItem item = cartItemRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + id));

        // Update the cart's updated timestamp
        Cart cart = item.getCart();
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepo.save(cart);

        cartItemRepo.deleteById(id);
    }

    /**
     * Delete all items from a cart
     */
    @Transactional
    public void deleteAllByCartId(Long cartId) {
        if (!cartRepo.existsById(cartId)) {
            throw new ResourceNotFoundException("Cart not found with id: " + cartId);
        }

        cartItemRepo.deleteByCartId(cartId);

        // Update the cart's updated timestamp
        Cart cart = cartRepo.findById(cartId).get();
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepo.save(cart);
    }
}