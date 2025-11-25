package com.example.demo.services;

import com.example.demo.entities.Product;
import com.example.demo.entities.Review;
import com.example.demo.entities.User;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.ProductRepo;
import com.example.demo.repositories.ReviewRepo;
import com.example.demo.repositories.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing product reviews in the e-commerce system.
 * Handles review creation, moderation, and management.
 */
@Service
public class ReviewService {
    @Autowired
    private ReviewRepo reviewRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ProductRepo productRepo;

    /**
     * Get all reviews (admin function)
     */
    public List<Review> getAll() {
        return reviewRepo.findAll();
    }

    /**
     * Get paginated reviews (admin function)
     */
    public List<Review> getAllPaginated() {
        return reviewRepo.findAll();
    }

    /**
     * Get review by ID
     */
    public Optional<Review> getById(Long id) {
        return reviewRepo.findById(id);
    }

    /**
     * Get reviews by product ID
     */
    public List<Review> getByProductId(Long productId) {
        return reviewRepo.findByProductId(productId);
    }


    /**
     * Get reviews by user ID
     */
    public List<Review> getByUserId(Long userId) {
        return reviewRepo.findByUserId(userId);
    }

    /**
     * Get approved reviews by product ID
     */
    public List<Review> getApprovedByProductId(Long productId) {
        return reviewRepo.findByProductIdAndApprovedTrue(productId);
    }



    /**
     * Get reviews pending moderation (admin function)
     */
    public List<Review> getPendingReviews() {
        return reviewRepo.findByApprovedFalse();
    }

    /**
     * Check if user has already reviewed a product
     */
    public boolean hasUserReviewedProduct(Long userId, Long productId) {
        return reviewRepo.existsByUserIdAndProductId(userId, productId);
    }

    /**
     * Add a new review with validation
     */
    @Transactional
    public Review add(Review review) {
        // Validate the review
        if (review.getRating() < 1 || review.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        if (review.getComment() == null || review.getComment().trim().isEmpty()) {
            throw new IllegalArgumentException("Review comment cannot be empty");
        }

        if (review.getUser() == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        if (review.getProduct() == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }

        // Check if user exists
        User user = userRepo.findById(review.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + review.getUser().getId()));

        // Check if product exists
        Product product = productRepo.findById(review.getProduct().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + review.getProduct().getId()));

        // Check if user has already reviewed this product
        if (reviewRepo.existsByUserIdAndProductId(user.getId(), product.getId())) {
            throw new IllegalStateException("User has already reviewed this product");
        }

        // Set timestamps
        if (review.getCreatedAt() == null) {
            review.setCreatedAt(LocalDateTime.now());
        }
        review.setUpdatedAt(LocalDateTime.now());

        // Save the review
        Review savedReview = reviewRepo.save(review);

        // Update product's average rating (optional)
        updateProductAverageRating(product.getId());

        return savedReview;
    }

    /**
     * Update an existing review with validation
     */
    @Transactional
    public Review update(Long id, Review updatedReview) {
        Review existingReview = reviewRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));

        // Validate rating
        if (updatedReview.getRating() < 1 || updatedReview.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // Validate comment
        if (updatedReview.getComment() == null || updatedReview.getComment().trim().isEmpty()) {
            throw new IllegalArgumentException("Review comment cannot be empty");
        }

        // Update fields
        existingReview.setRating(updatedReview.getRating());
        existingReview.setComment(updatedReview.getComment());
        existingReview.setUpdatedAt(LocalDateTime.now());

        // If approved status is changing
        if (updatedReview.isApproved() != existingReview.isApproved()) {
            existingReview.setApproved(updatedReview.isApproved());
        }

        // Update verified purchase status if provided
        if (updatedReview.isVerifiedPurchase() != existingReview.isVerifiedPurchase()) {
            existingReview.setVerifiedPurchase(updatedReview.isVerifiedPurchase());
        }

        Review savedReview = reviewRepo.save(existingReview);

        // If we changed the rating, update the product's average rating
        updateProductAverageRating(existingReview.getProduct().getId());

        return savedReview;
    }

    /**
     * Approve/reject a review (admin function)
     */
    @Transactional
    public Review moderateReview(Long id, boolean approved) {
        Review review = reviewRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));

        review.setApproved(approved);
        review.setUpdatedAt(LocalDateTime.now());

        Review savedReview = reviewRepo.save(review);

        // Update product's average rating if review was approved
        if (approved) {
            updateProductAverageRating(review.getProduct().getId());
        }

        return savedReview;
    }

    /**
     * Mark a review as verified purchase
     */
    @Transactional
    public Review verifyPurchase(Long id) {
        Review review = reviewRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));

        review.setVerifiedPurchase(true);
        review.setUpdatedAt(LocalDateTime.now());

        return reviewRepo.save(review);
    }

    /**
     * Delete a review
     */
    @Transactional
    public void delete(Long id) {
        Review review = reviewRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with id: " + id));

        Long productId = review.getProduct().getId();

        reviewRepo.deleteById(id);

        // Update product's average rating
        updateProductAverageRating(productId);
    }

    /**
     * Delete all reviews by product ID
     */
    @Transactional
    public void deleteByProductId(Long productId) {
        if (!productRepo.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }

        reviewRepo.deleteByProductId(productId);
    }

    /**
     * Calculate average rating for a product
     */
    public double calculateAverageRating(Long productId) {
        List<Review> reviews = reviewRepo.findByProductIdAndApprovedTrue(productId);
        if (reviews.isEmpty()) {
            return 0.0;
        }

        double sum = reviews.stream().mapToInt(Review::getRating).sum();
        return sum / reviews.size();
    }

    /**
     * Update product's average rating calculation
     */
    private void updateProductAverageRating(Long productId) {
        // This method would update the product's average rating
        // You would need to add an averageRating field to your Product entity
        // This is left as an extension point for your implementation

        // Example implementation:
        /*
        double avgRating = calculateAverageRating(productId);
        Product product = productRepo.findById(productId).orElse(null);
        if (product != null) {
            product.setAverageRating(avgRating);
            productRepo.save(product);
        }
        */
    }

    /**
     * Get rating distribution for a product
     */
    public List<Object[]> getRatingDistribution(Long productId) {
        return reviewRepo.countRatingsByProduct(productId);
    }

    /**
     * Get top rated products
     */
    public List<Object[]> getTopRatedProducts(int limit) {
        return reviewRepo.findTopRatedProducts(limit);
    }

    /**
     * Get recent reviews
     */
    public List<Review> getRecentReviews(int limit) {
        return reviewRepo.findTopByOrderByCreatedAtDesc();
    }
}