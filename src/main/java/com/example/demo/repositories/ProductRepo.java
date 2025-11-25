package com.example.demo.repositories;

import com.example.demo.entities.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepo extends JpaRepository<Product, Long> {

    List<Product> findByCategoryId(Long categoryId);


    Product findByNameContainingIgnoreCase(String name);

    // תמיכה בחיפוש עם דפדוף

    List<Product> findByBrand(String brand);

    // חיפוש לפי מודל
    List<Product> findByModel(String model);

    boolean existsByName(String name);

    List<Product> findByPriceBetween(double minPrice, double maxPrice);


    List<Product> findByQuantityLessThan(int quantity);

    // מוצרים שאזלו מהמלאי
    List<Product> findByQuantity(int quantity);

    // מוצרים אקטיביים בלבד
    List<Product> findByActiveTrue();

    // שאילתה מורכבת - סינון לפי מחיר וקטגוריה
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :minPrice AND :maxPrice AND p.category.id = :categoryId")
    List<Product> findByPriceRangeAndCategory(
            @Param("minPrice") double minPrice,
            @Param("maxPrice") double maxPrice,
            @Param("categoryId") Long categoryId
    );

    // שאילתה מורכבת - חיפוש לפי מספר פרמטרים
    @Query("SELECT p FROM Product p WHERE " +
            "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
            "(:brand IS NULL OR p.brand = :brand) AND " +
            "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
            "p.active = true")
    List<Product> findProductsByFilters(
            @Param("name") String name,
            @Param("categoryId") Long categoryId,
            @Param("brand") String brand,
            @Param("minPrice") Double minPrice,
            @Param("maxPrice") Double maxPrice
    );

    // מציאת מוצרים פופולריים (לפי הכמות בהזמנות)
    @Query(value = "SELECT p.* FROM product p " +
            "JOIN order_items oi ON p.id = oi.product_id " +
            "GROUP BY p.id " +
            "ORDER BY SUM(oi.quantity) DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Product> findPopularProducts(@Param("limit") int limit);

    // מציאת המותגים הייחודיים
    @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL")
    List<String> findDistinctBrands();
}