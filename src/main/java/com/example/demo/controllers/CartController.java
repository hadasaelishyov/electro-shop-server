package com.example.demo.controllers;

import com.example.demo.entities.Cart;
import com.example.demo.entities.CartItem;
import com.example.demo.services.CartService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/carts")
@CrossOrigin(origins = "http://localhost:5173")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public ResponseEntity<List<Cart>> getAll() {
        return ResponseEntity.ok(cartService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cart> getById(@PathVariable Long id) {
        return cartService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{email}")
    public ResponseEntity<List<Cart>> getByUserEmail(@PathVariable String email) {
        return ResponseEntity.ok(cartService.getByUserEmail(email));
    }

    @GetMapping("/user/{email}/active")
    public ResponseEntity<Cart> getActiveCartByUserEmail(@PathVariable String email) {
        return cartService.getActiveCartByUserEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/id/{userId}/active")
    public ResponseEntity<Cart> getActiveCartByUserId(@PathVariable Long userId) {
        return cartService.getActiveCartByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{email}/get-or-create")
    public ResponseEntity<Cart> getOrCreateActiveCart(@PathVariable String email) {
        try {
            return ResponseEntity.ok(cartService.getOrCreateActiveCart(email));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/user/{email}")
    public ResponseEntity<Cart> createCartForUser(@PathVariable String email) {
        try {
            return ResponseEntity.ok(cartService.createCartForUser(email));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{cartId}/products/{productId}")
    public ResponseEntity<Cart> addProductToCart(
            @PathVariable Long cartId,
            @PathVariable Long productId,
            @RequestParam int quantity) {
        try {
            return ResponseEntity.ok(cartService.addProductToCart(cartId, productId, quantity));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{cartId}/products/{productId}")
    public ResponseEntity<Cart> updateCartItemQuantity(
            @PathVariable Long cartId,
            @PathVariable Long productId,
            @RequestParam int quantity) {
        try {
            return ResponseEntity.ok(cartService.updateCartItemQuantity(cartId, productId, quantity));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{cartId}/products/{productId}")
    public ResponseEntity<Void> removeProductFromCart(
            @PathVariable Long cartId,
            @PathVariable Long productId) {
        try {
            cartService.removeProductFromCart(cartId, productId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{cartId}/clear")
    public ResponseEntity<Void> clearCart(@PathVariable Long cartId) {
        try {
            cartService.clearCart(cartId);
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/merge/{anonymousCartId}/user/{userEmail}")
    public ResponseEntity<Cart> mergeAnonymousCartWithUserCart(
            @PathVariable Long anonymousCartId,
            @PathVariable String userEmail) {
        try {
            return ResponseEntity.ok(cartService.mergeAnonymousCartWithUserCart(anonymousCartId, userEmail));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/abandoned")
    public ResponseEntity<List<Cart>> getAbandonedCarts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime cutoffTime) {
        return ResponseEntity.ok(cartService.getAbandonedCarts(cutoffTime));
    }

    @GetMapping("/count/active")
    public ResponseEntity<Long> countActiveCarts() {
        return ResponseEntity.ok(cartService.countActiveCarts());
    }

    @PostMapping
    public ResponseEntity<Cart> create(@RequestBody Cart cart) {
        try {
            return ResponseEntity.ok(cartService.add(cart));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Cart> update(@PathVariable Long id, @RequestBody Cart cart) {
        try {
            return ResponseEntity.ok(cartService.update(id, cart));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            cartService.delete(id);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}