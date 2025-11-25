package com.example.demo.controllers;

import com.example.demo.dto.OrderDto;
import com.example.demo.entities.Order;
import com.example.demo.entities.Payment;
import com.example.demo.entities.PaymentMethod;
import com.example.demo.exceptions.InvalidOrderStateException;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.services.OrderService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "http://localhost:5173")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // Basic CRUD operations

    @GetMapping
    public ResponseEntity<List<OrderDto>> getAll() {
        return ResponseEntity.ok(orderService.getAll());
    }

    @GetMapping("/paginated")
    public ResponseEntity<List<OrderDto>> getAllPaginated() {
        return ResponseEntity.ok(orderService.getAllPaginated());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getById(@PathVariable Long id) {
        return orderService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Order> create(@RequestBody Order order) {
        try {
            return ResponseEntity.ok(orderService.add(order));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> update(@PathVariable Long id, @RequestBody Order order) {
        try {
            return ResponseEntity.ok(orderService.update(id, order));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            orderService.delete(id);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

    }

    // Specialized endpoints for order management

    @GetMapping("/user/{email}")
    public ResponseEntity<List<OrderDto>> getByUserEmail(
            @PathVariable String email ){
        return ResponseEntity.ok(orderService.getByUserEmail(email));
    }

    @GetMapping("/dateRange")
    public ResponseEntity<List<OrderDto>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate){
        return ResponseEntity.ok(orderService.getByDateRange(startDate, endDate));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<OrderDto>> filterOrders(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Double minAmount){
        return ResponseEntity.ok(orderService.filterOrders(userId, startDate, endDate, minAmount));
    }

    // Order status management



    // Order creation from cart

    @PostMapping("/cart/{cartId}")
    public ResponseEntity<?> createFromCart(
            @PathVariable Long cartId,
            @RequestParam String shippingAddress,
            @RequestParam String shippingCity,
            @RequestParam String shippingZipCode,
            @RequestParam String shippingCountry) {
        try {
            Order order = orderService.createOrderFromCart(
                    cartId, shippingAddress, shippingCity, shippingZipCode, shippingCountry);
            return ResponseEntity.ok(order);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (InvalidOrderStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Payment processing



    @GetMapping("/recent")
    public ResponseEntity<List<OrderDto>> getRecentOrders() {
        return ResponseEntity.ok(orderService.getRecentOrders());
    }
}