package com.example.demo.controllers;

import com.example.demo.entities.Review;
import com.example.demo.services.ReviewService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reviews")
@CrossOrigin(origins = "http://localhost:5173")
public class ReviewController {

    @Autowired
    private ReviewService reviewService;

    // Get all reviews (admin)
    @GetMapping
    public ResponseEntity<List<Review>> getAll() {
        return ResponseEntity.ok(reviewService.getAll());
    }

    // Get all reviews with pagination (admin)
    @GetMapping("/paginated")
    public ResponseEntity<List<Review>> getAllPaginated(
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        return ResponseEntity.ok(reviewService.getAllPaginated());
    }

    // Get review by ID
    @GetMapping("/{id}")
    public ResponseEntity<Review> getById(@PathVariable Long id) {
        return reviewService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Get reviews by product ID
    @GetMapping("/product/{productId}")
    public ResponseEntity<List<Review>> getByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getByProductId(productId));
    }

    // Get reviews by product ID with pagination
    @GetMapping("/product/{productId}/paginated")
    public ResponseEntity<List<Review>> getByProductIdPaginated(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase("ASC") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        return ResponseEntity.ok(reviewService.getByProductId(productId));
    }

    // Get approved reviews by product ID
    @GetMapping("/product/{productId}/approved")
    public ResponseEntity<List<Review>> getApprovedByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getApprovedByProductId(productId));
    }

    // Get reviews by user ID
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Review>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(reviewService.getByUserId(userId));
    }

    // Get reviews by user ID with pagination
    @GetMapping("/user/{userId}/paginated")
    public ResponseEntity<List<Review>> getByUserIdPaginated(
            @PathVariable Long userId) {

        return ResponseEntity.ok(reviewService.getByUserId(userId));
    }

    // Get pending reviews (admin)
    @GetMapping("/pending")
    public ResponseEntity<List<Review>> getPendingReviews() {
        return ResponseEntity.ok(reviewService.getPendingReviews());
    }

    // Check if user has reviewed a product
    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> hasUserReviewedProduct(
            @RequestParam Long userId,
            @RequestParam Long productId) {

        boolean hasReviewed = reviewService.hasUserReviewedProduct(userId, productId);
        Map<String, Boolean> response = new HashMap<>();
        response.put("hasReviewed", hasReviewed);

        return ResponseEntity.ok(response);
    }

    // Get rating distribution for a product
    @GetMapping("/product/{productId}/ratings")
    public ResponseEntity<List<Object[]>> getRatingDistribution(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getRatingDistribution(productId));
    }

    // Get average rating for a product
    @GetMapping("/product/{productId}/average-rating")
    public ResponseEntity<Map<String, Double>> getAverageRating(@PathVariable Long productId) {
        double avgRating = reviewService.calculateAverageRating(productId);
        Map<String, Double> response = new HashMap<>();
        response.put("averageRating", avgRating);

        return ResponseEntity.ok(response);
    }

    // Get top rated products
    @GetMapping("/top-rated")
    public ResponseEntity<List<Object[]>> getTopRatedProducts(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(reviewService.getTopRatedProducts(limit));
    }

    // Get recent reviews
    @GetMapping("/recent")
    public ResponseEntity<List<Review>> getRecentReviews(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(reviewService.getRecentReviews(limit));
    }

    // Add new review
    @PostMapping
    public ResponseEntity<Review> add(@RequestBody Review review) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.add(review));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (IllegalStateException e) {
            // User has already reviewed this product
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        }
    }

    // Update review
    @PutMapping("/{id}")
    public ResponseEntity<Review> update(@PathVariable Long id, @RequestBody Review review) {
        try {
            return ResponseEntity.ok(reviewService.update(id, review));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Moderate review (approve/reject) - admin function
    @PatchMapping("/{id}/moderate")
    public ResponseEntity<Review> moderateReview(
            @PathVariable Long id,
            @RequestParam boolean approved) {
        try {
            return ResponseEntity.ok(reviewService.moderateReview(id, approved));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Mark review as verified purchase
    @PatchMapping("/{id}/verify-purchase")
    public ResponseEntity<Review> verifyPurchase(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(reviewService.verifyPurchase(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Delete review
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            reviewService.delete(id);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Delete all reviews for a product
    @DeleteMapping("/product/{productId}")
    public ResponseEntity<Void> deleteByProductId(@PathVariable Long productId) {
        try {
            reviewService.deleteByProductId(productId);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}