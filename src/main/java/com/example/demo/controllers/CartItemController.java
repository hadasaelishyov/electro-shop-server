package com.example.demo.controllers;

import com.example.demo.entities.CartItem;
import com.example.demo.exceptions.InsufficientInventoryException;
import com.example.demo.services.CartItemService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cartItems")
@CrossOrigin(origins = "http://localhost:5173")
public class CartItemController {

    @Autowired
    private CartItemService cartItemService;

    @GetMapping
    public ResponseEntity<List<CartItem>> getAll() {
        return ResponseEntity.ok(cartItemService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CartItem> getById(@PathVariable Long id) {
        return cartItemService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/exists/{cartId}/{productId}")
    public ResponseEntity<Boolean> existsByCartIdAndProductId(
            @PathVariable Long cartId,
            @PathVariable Long productId) {
        boolean exists = cartItemService.existsByCartIdAndProductId(cartId, productId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/cart/{cartId}")
    public ResponseEntity<List<CartItem>> getByCartId(@PathVariable Long cartId) {
        return ResponseEntity.ok(cartItemService.getByCartId(cartId));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<CartItem>> getByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(cartItemService.getByProductId(productId));
    }

    @PostMapping
    public ResponseEntity<?> add(@RequestBody CartItem cartItem) {
        try {
            return ResponseEntity.ok(cartItemService.add(cartItem));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (InsufficientInventoryException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody CartItem cartItem) {
        try {
            return ResponseEntity.ok(cartItemService.update(id, cartItem));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException | InsufficientInventoryException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/quantity/{quantity}")
    public ResponseEntity<?> updateQuantity(@PathVariable Long id, @PathVariable int quantity) {
        try {
            return ResponseEntity.ok(cartItemService.updateQuantity(id, quantity));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException | InsufficientInventoryException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            cartItemService.delete(id);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/cart/{cartId}")
    public ResponseEntity<Void> deleteAllByCartId(@PathVariable Long cartId) {
        try {
            cartItemService.deleteAllByCartId(cartId);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}