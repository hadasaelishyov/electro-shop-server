package com.example.demo.services;

import com.example.demo.dto.OrderDto;
import com.example.demo.dto.OrderMapper;
import com.example.demo.entities.*;
import com.example.demo.exceptions.InsufficientInventoryException;
import com.example.demo.exceptions.InvalidOrderStateException;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing orders in the e-commerce system.
 * Handles order creation, processing, status updates, and management.
 */
@Service
public class OrderService {
    @Autowired
    private OrderRepo orderRepo;
    @Autowired
    private OrderMapper orderMapper;


    @Autowired
    private OrderItemRepo orderItemRepo;

    @Autowired
    private CartRepo cartRepo;

    @Autowired
    private     CartItemRepo cartItemRepo;

    @Autowired
    private ProductRepo productRepo;

    /**
     * Get all orders with optional pagination
     */
    public List<OrderDto> getAllPaginated() {
        return orderRepo.findAll()
                .stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all orders (without pagination)
     */
    public List<OrderDto> getAll() {
        return orderRepo.findAll().
                stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get order by ID
     */
    public Optional<OrderDto> getById(Long id) {
        return orderRepo.findById(id)
                .map(order -> orderMapper.toDto(order));
    }


    /**
     * Get orders by user email with pagination
     */
    public List<OrderDto> getByUserEmail(String email)
    {
        return orderRepo.findByUser_Email(email)
                .stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get orders by status with pagination
     */


    /**
     * Get orders by date range with pagination
     */
    public List<OrderDto> getByDateRange(LocalDate startDate, LocalDate endDate) {
        return orderRepo.findByOrderDateBetween(startDate, endDate)
                .stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Filter orders with advanced criteria and pagination
     */
    public List<OrderDto> filterOrders(Long userId,
                                    LocalDate startDate, LocalDate endDate,
                                    Double minAmount) {
        return orderRepo.findOrdersByFilters(userId,startDate, endDate, minAmount)
                .stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Create a new order
     */
    @Transactional

    public Order add(Order order) {
        // Validate order data
        if (order.getUser() == null) {
            throw new IllegalArgumentException("Order must have a user");
        }

        // Set timestamps
        if (order.getCreatedAt() == null) {
            order.setCreatedAt(LocalDateTime.now());
        }
        order.setUpdatedAt(LocalDateTime.now());


        return orderRepo.save(order);
    }

    /**
     * Update an existing order
     */
    @Transactional

    public Order update(Long id, Order updatedOrder) {
        Order existingOrder = orderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        // Update order fields
        if (updatedOrder.getShippingAddress() != null) {
            existingOrder.setShippingAddress(updatedOrder.getShippingAddress());
        }
        if (updatedOrder.getShippingCity() != null) {
            existingOrder.setShippingCity(updatedOrder.getShippingCity());
        }
        if (updatedOrder.getShippingZipCode() != null) {
            existingOrder.setShippingZipCode(updatedOrder.getShippingZipCode());
        }
        if (updatedOrder.getShippingCountry() != null) {
            existingOrder.setShippingCountry(updatedOrder.getShippingCountry());
        }


        existingOrder.setUpdatedAt(LocalDateTime.now());
        return orderRepo.save(existingOrder);
    }

    /**
     * Delete an order (should only be available for PENDING orders)
     */
    @Transactional
    public void delete(Long id) {
        Order order = orderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));


        orderRepo.deleteById(id);
    }


    /**
     * Update product inventory when order is marked as delivered
     */
    private void updateProductInventoryForDeliveredOrder(Order order) {
        // Implementation depends on your inventory management approach
        // This is a placeholder - actual implementation would depend on your requirements
    }

    /**
     * Restore product inventory when order is cancelled
     */
    private void restoreProductInventory(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setQuantity(product.getQuantity() + item.getQuantity());
            productRepo.save(product);
        }
    }

    /**
     * Create an order from a shopping cart
     */
    @Transactional
    public Order createOrderFromCart(Long cartId, String shippingAddress,
                                     String shippingCity, String shippingZipCode,
                                     String shippingCountry) {
        Cart cart = cartRepo.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found with id: " + cartId));

        // Only process active carts
        if (!cart.isActive()) {
            throw new InvalidOrderStateException("Cannot create order from inactive cart");
        }

        // Check if cart is empty
        if (cart.getCartItems().isEmpty()) {
            throw new InvalidOrderStateException("Cannot create order from empty cart");
        }

        // Validate inventory before creating order
        validateInventory(cart);

        // Create new order
        Order order = new Order(cart.getUser(), LocalDate.now());
        order.setShippingAddress(shippingAddress);
        order.setShippingCity(shippingCity);
        order.setShippingZipCode(shippingZipCode);
        order.setShippingCountry(shippingCountry);
        orderRepo.save(order);

        // Get cart items
        List<CartItem> cartItems = cartItemRepo.findByCartId(cartId);
        double totalAmount = 0.0;

        // Create order items from cart items and update inventory
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            // Update product inventory
            int newQuantity = product.getQuantity() - cartItem.getQuantity();
            if (newQuantity < 0) {
                throw new InsufficientInventoryException(
                        "Not enough inventory for product: " + product.getName());
            }
            product.setQuantity(newQuantity);
            productRepo.save(product);

            // Create order item
            double itemPrice = cartItem.getUnitPrice() * cartItem.getQuantity();
            totalAmount += itemPrice;

            OrderItem orderItem = new OrderItem(order, product, cartItem.getQuantity(), cartItem.getUnitPrice());
            orderItemRepo.save(orderItem);
            order.getOrderItems().add(orderItem); // חשוב!

        }

        // Update order total
        order.setTotalAmount(totalAmount);
        try {
             orderRepo.save(order);
        } catch (Exception e) {
            e.printStackTrace(); // או לוג
            System.out.println("שגיאה: " + e.getMessage());
            Throwable cause = e.getCause();
            while (cause != null) {
                System.out.println("Caused by: " + cause.getMessage());
                cause = cause.getCause();
            }
            throw e; // כדי לראות בדיוק מה קרה
        }

        // Deactivate the cart
        cart.setActive(false);
        cartRepo.save(cart);

        return order;

    }

    /**
     * Validate that there is sufficient inventory for all items in the cart
     */
    private void validateInventory(Cart cart) {
        for (CartItem item : cart.getCartItems()) {
            Product product = item.getProduct();
            if (product.getQuantity() < item.getQuantity()) {
                throw new InsufficientInventoryException(
                        "Not enough inventory for product: " + product.getName() +
                                ". Available: " + product.getQuantity() +
                                ", Requested: " + item.getQuantity());
            }
        }
    }



    /**
     * Get revenue statistics by date range
     */
    public List<Object[]> getRevenueByDateRange(LocalDate startDate, LocalDate endDate) {
        return orderRepo.sumOrderAmountsByDate(startDate, endDate);
    }

    /**
     * Get recent orders
     */
    public List<OrderDto> getRecentOrders() {
        return orderRepo.findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }
}