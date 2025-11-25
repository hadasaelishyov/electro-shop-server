package com.example.demo.repositories;

import com.example.demo.entities.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface ReviewRepo extends JpaRepository<Review, Long> {

    // כל הביקורות על מוצר מסוים
    List<Review> findByProductId(Long productId);


    // לסדר את הביקורות של מוצר מהחדשות לישנות
    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    // ביקורות של משתמש ספציפי
    List<Review> findByUserId(Long userId);


    // ביקורות שעדיין לא אושרו (למנהל)
    List<Review> findByApprovedFalse();

    // ביקורות שאושרו (לתצוגה)
    List<Review> findByProductIdAndApprovedTrue(Long productId);

    // ביקורות מאומתות (verified purchase)
    List<Review> findByProductIdAndVerifiedPurchaseTrue(Long productId);

    // חיפוש לפי דירוג
    List<Review> findByProductIdAndRating(Long productId, int rating);

    // חישוב דירוג ממוצע של מוצר
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.approved = true")
    Double calculateAverageRatingForProduct(@Param("productId") Long productId);

    // חישוב התפלגות דירוגים
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.approved = true GROUP BY r.rating ORDER BY r.rating DESC")
    List<Object[]> countRatingsByProductId(@Param("productId") Long productId);

    @Query("SELECT r.product.id, r.product.name, AVG(r.rating) as avgRating, COUNT(r) as reviewCount " +
            "FROM Review r " +
            "WHERE r.approved = true " +
            "GROUP BY r.product.id, r.product.name " +
            "HAVING COUNT(r) >= 3 " +
            "ORDER BY avgRating DESC, reviewCount DESC " +
            "LIMIT :limit")
    List<Object[]> findTopRatedProducts(@Param("limit") int limit);

    List<Review> findTopByOrderByCreatedAtDesc();

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    void deleteByProductId(Long productId);

    @Query("SELECT r.rating as rating, COUNT(r) as count " +
            "FROM Review r " +
            "WHERE r.product.id = :productId AND r.approved = true " +
            "GROUP BY r.rating " +
            "ORDER BY r.rating")
    List<Object[]> countRatingsByProduct(@Param("productId") Long productId);}