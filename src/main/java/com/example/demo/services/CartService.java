package com.example.demo.services;

import com.example.demo.entities.Cart;
import com.example.demo.entities.CartItem;
import com.example.demo.entities.Product;
import com.example.demo.entities.User;
import com.example.demo.exceptions.InsufficientInventoryException;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.CartItemRepo;
import com.example.demo.repositories.CartRepo;
import com.example.demo.repositories.ProductRepo;
import com.example.demo.repositories.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing shopping carts in the e-commerce system.
 * Handles cart creation, updates, and management.
 */
@Service
public class CartService {
    @Autowired
    private CartRepo cartRepo;

    @Autowired
    private CartItemRepo cartItemRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ProductRepo productRepo;

    /**
     * Get all carts (admin function)
     */
    public List<Cart> getAll() {
        return cartRepo.findAll();
    }

    /**
     * Get cart by ID
     */
    public Optional<Cart> getById(Long id) {
        return cartRepo.findById(id);
    }

    /**
     * Get carts by user email
     */
    public List<Cart> getByUserEmail(String email) {
        return cartRepo.findByUserEmail(email);
    }

    /**
     * Get active cart for user by email
     */
    public Optional<Cart> getActiveCartByUserEmail(String email) {
        return cartRepo.findByUserEmailAndActiveTrue(email);
    }

    /**
     * Get active cart for user by ID
     */
    public Optional<Cart> getActiveCartByUserId(Long userId) {
        return cartRepo.findByUserIdAndActiveTrue(userId);
    }

    /**
     * Create a new cart for user or get existing active cart
     */
    @Transactional
    public Cart getOrCreateActiveCart(String email) {
        Optional<Cart> existingCart = getActiveCartByUserEmail(email);
        if (existingCart.isPresent()) {
            return existingCart.get();
        }

        return createCartForUser(email);
    }

    /**
     * Create a new cart for user
     */
    @Transactional
    public Cart createCartForUser(String email) {
        User user = userRepo.findByEmail(email);
        if (user == null) {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }

        Cart cart = new Cart(user);
        return cartRepo.save(cart);
    }

    /**
     * Add a product to cart with quantity validation
     */
    @Transactional
    public Cart addProductToCart(Long cartId, Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        Cart cart = cartRepo.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with id: " + cartId));

        if (!cart.isActive()) {
            throw new IllegalStateException("Cannot add products to inactive cart");
        }

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (!product.isActive()) {
            throw new IllegalStateException("Cannot add inactive product to cart");
        }

        // Validate inventory
        if (product.getQuantity() < quantity) {
            throw new InsufficientInventoryException(
                    "Not enough inventory for product: " + product.getName() +
                            ". Available: " + product.getQuantity() +
                            ", Requested: " + quantity);
        }

        // Check if product already exists in cart
        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        CartItem cartItem;
        if (existingItem.isPresent()) {
            // Update existing item
            cartItem = existingItem.get();
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItem.setUpdatedAt(LocalDateTime.now());
        } else {
            // Create new cart item
            cartItem = new CartItem(cart, product, quantity);
            cart.getCartItems().add(cartItem);
        }

        // Update cart timestamp
        cart.setUpdatedAt(LocalDateTime.now());

        // Save cart (this will cascade save the cart items due to CascadeType.ALL)
        cartRepo.save(cart);

        return cart;
    }
    /**
     * Update product quantity in cart
     */
    @Transactional
    public Cart updateCartItemQuantity(Long cartId, Long productId, int newQuantity) {
        if (newQuantity <= 0) {
            removeProductFromCart(cartId, productId);
        }

        Cart cart = cartRepo.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with id: " + cartId));

        if (!cart.isActive()) {
            throw new IllegalStateException("Cannot update inactive cart");
        }

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        // Validate inventory
        if (product.getQuantity() < newQuantity) {
            throw new InsufficientInventoryException(
                    "Not enough inventory for product: " + product.getName() +
                            ". Available: " + product.getQuantity() +
                            ", Requested: " + newQuantity);
        }

        // Find cart item
        CartItem cartItem = cart.getCartItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product not found in cart"));

        // Update quantity
        cartItem.setQuantity(newQuantity);
        cartItem.setUpdatedAt(LocalDateTime.now());

        // Update cart
        cart.setUpdatedAt(LocalDateTime.now());
        cartItemRepo.save(cartItem);
        return cartRepo.save(cart);

    }

    /**
     * Remove a product from cart
     */
    @Transactional
    public void removeProductFromCart(Long cartId, Long productId) {
        Cart cart = cartRepo.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with id: " + cartId));

        if (!cart.isActive()) {
            throw new IllegalStateException("Cannot modify inactive cart");
        }

        // Find cart item
        CartItem cartItemToRemove = null;
        for (CartItem item : cart.getCartItems()) {
            if (item.getProduct().getId().equals(productId)) {
                cartItemToRemove = item;
                break;
            }
        }

        if (cartItemToRemove != null) {
            cart.getCartItems().remove(cartItemToRemove);
            cartItemRepo.deleteById(cartItemToRemove.getId());

            // Update cart
            cart.setUpdatedAt(LocalDateTime.now());
            cartRepo.save(cart);
        }
    }

    /**
     * Clear all items from cart
     */
    @Transactional
    public void clearCart(Long cartId) {
        Cart cart = cartRepo.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with id: " + cartId));

        if (!cart.isActive()) {
            throw new IllegalStateException("Cannot modify inactive cart");
        }

        // Remove all cart items
        cart.getCartItems().clear();
        cartItemRepo.deleteAll(cart.getCartItems());

        // Update cart
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepo.save(cart);
    }

    /**
     * Merge anonymous cart with user cart after login
     */
    @Transactional
    public Cart mergeAnonymousCartWithUserCart(Long anonymousCartId, String userEmail) {
        Cart anonymousCart = cartRepo.findById(anonymousCartId)
                .orElseThrow(() -> new ResourceNotFoundException("Anonymous cart not found with id: " + anonymousCartId));

        // Get or create user cart
        Cart userCart = getOrCreateActiveCart(userEmail);

        // Merge items from anonymous cart to user cart
        for (CartItem item : anonymousCart.getCartItems()) {
            Product product = item.getProduct();
            userCart.addProduct(product, item.getQuantity());
        }

        // Save user cart
        userCart.setUpdatedAt(LocalDateTime.now());
        cartRepo.save(userCart);

        // Deactivate anonymous cart
        anonymousCart.setActive(false);
        cartRepo.save(anonymousCart);

        return userCart;
    }

    /**
     * Get abandoned carts (for marketing/analysis)
     */
    public List<Cart> getAbandonedCarts(LocalDateTime cutoffTime) {
        return cartRepo.findByActiveTrueAndUpdatedAtBefore(cutoffTime);
    }

    /**
     * Get count of active carts (for dashboard)
     */
    public Long countActiveCarts() {
        return cartRepo.countByActiveTrue();
    }

    /**
     * Add a new cart
     */
    public Cart add(Cart cart) {
        if (cart.getCreatedAt() == null) {
            cart.setCreatedAt(LocalDateTime.now());
        }
        cart.setUpdatedAt(LocalDateTime.now());
        return cartRepo.save(cart);
    }

    /**
     * Update an existing cart
     */
    public Cart update(Long id, Cart updatedCart) {
        if (!cartRepo.existsById(id)) {
            throw new ResourceNotFoundException("Cart not found with id: " + id);
        }

        updatedCart.setId(id);
        updatedCart.setUpdatedAt(LocalDateTime.now());
        return cartRepo.save(updatedCart);
    }

    /**
     * Delete a cart
     */
    public void delete(Long id) {
        if (!cartRepo.existsById(id)) {
            throw new ResourceNotFoundException("Cart not found with id: " + id);
        }

        cartRepo.deleteById(id);
    }
}