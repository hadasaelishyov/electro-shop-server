package com.example.demo.controllers;

import com.example.demo.dto.ProductDto;
import com.example.demo.entities.Product;
import com.example.demo.entities.ProductSpecification;
import com.example.demo.services.ProductService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
@CrossOrigin(origins = "http://localhost:5173")
public class ProductController {

    @Autowired
    private ProductService productService;

    // Get all products
    @GetMapping
    public ResponseEntity<List<ProductDto>> getAll() {
        List<ProductDto>products = productService.getAll();
        return ResponseEntity.ok(products);
    }


    // Get product by ID
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getById(@PathVariable Long id) {
        return productService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Search products with filters
    @GetMapping("/search")
    public ResponseEntity<List<ProductDto> >search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {

        return ResponseEntity.ok(productService.searchProducts(name, categoryId, brand, minPrice, maxPrice));
    }

    // Get products by category
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ProductDto>> getByCategory(
            @PathVariable Long categoryId){
        return ResponseEntity.ok(productService.getByCategoryId(categoryId));
    }

    // Get products by brand
    @GetMapping("/brand/{brand}")
    public ResponseEntity<List<Product>> getByBrand(@PathVariable String brand) {
        return ResponseEntity.ok(productService.getByBrand(brand));
    }

    // Get all available brands
    @GetMapping("/brands")
    public ResponseEntity<List<String>> getAllBrands() {
        return ResponseEntity.ok(productService.getAllBrands());
    }

    // Get products by price range
    @GetMapping("/price-range")
    public ResponseEntity<List<Product>> getByPriceRange(
            @RequestParam double minPrice,
            @RequestParam double maxPrice) {
        return ResponseEntity.ok(productService.getProductsByPriceRange(minPrice, maxPrice));
    }

    // Get products by price range and category
    @GetMapping("/price-range-category")
    public ResponseEntity<List<Product>> getByPriceRangeAndCategory(
            @RequestParam double minPrice,
            @RequestParam double maxPrice,
            @RequestParam Long categoryId) {
        return ResponseEntity.ok(productService.getProductsByPriceRangeAndCategory(minPrice, maxPrice, categoryId));
    }

    // Get popular products
    @GetMapping("/popular")
    public ResponseEntity<List<Product>> getPopularProducts(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(productService.getPopularProducts(limit));
    }

    // Get low stock products
    @GetMapping("/low-stock")
    public ResponseEntity<List<Product>> getLowStockProducts(
            @RequestParam(defaultValue = "5") int threshold) {
        return ResponseEntity.ok(productService.getLowStockProducts(threshold));
    }

    // Get out of stock products
    @GetMapping("/out-of-stock")
    public ResponseEntity<List<Product>> getOutOfStockProducts() {
        return ResponseEntity.ok(productService.getOutOfStockProducts());
    }

    // Create new product
    @PostMapping
    public ResponseEntity<?> create(@RequestBody ProductDto productDTO) {
        try {
            Product product = productService.createFromDTO(productDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(product);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create product: " + e.getMessage()));
        }
    }

    // Update product
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ProductDto productDTO) {
        try {
            ProductDto product = productService.updateFromDTO(id, productDTO);
            return ResponseEntity.ok(product);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update product: " + e.getMessage()));
        }
    }



    // Delete specification
    @DeleteMapping("/{productId}/specifications/{specId}")
    public ResponseEntity<Product> deleteSpecification(
            @PathVariable Long productId,
            @PathVariable Long specId) {
        try {
            return ResponseEntity.ok(productService.deleteSpecification(productId, specId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    @DeleteMapping("/{productId}/specifications/{imageUrl}")
    public ResponseEntity<Product> deleteImage(
            @PathVariable Long productId,
            @PathVariable String imageUrl) {
        try {
            return ResponseEntity.ok(productService.deleteImage(productId, imageUrl));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Update product stock
    @PatchMapping("/{id}/stock")
    public ResponseEntity<Product> updateStock(
            @PathVariable Long id,
            @RequestParam int quantity) {
        try {
            return ResponseEntity.ok(productService.updateStock(id, quantity));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Adjust product stock (increment/decrement)
    @PatchMapping("/{id}/adjust-stock")
    public ResponseEntity<Product> adjustStock(
            @PathVariable Long id,
            @RequestParam int quantityChange) {
        try {
            return ResponseEntity.ok(productService.adjustStock(id, quantityChange));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Activate product
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Product> activateProduct(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(productService.activateProduct(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Deactivate product
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Product> deactivateProduct(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(productService.deactivateProduct(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Delete product
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        try {
            productService.delete(id);
            return ResponseEntity.ok().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}