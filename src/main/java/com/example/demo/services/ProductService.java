package com.example.demo.services;

import com.example.demo.dto.OrderMapper;
import com.example.demo.dto.ProductDto;
import com.example.demo.dto.ProductImageDto;
import com.example.demo.dto.ProductSpecificationDto;
import com.example.demo.entities.Category;
import com.example.demo.entities.Product;
import com.example.demo.entities.ProductImage;
import com.example.demo.entities.ProductSpecification;
import com.example.demo.exceptions.ResourceNotFoundException;
import com.example.demo.repositories.CategoryRepo;
import com.example.demo.repositories.ProductRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService {
    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private CategoryRepo categoryRepo;
    @Autowired
    private OrderMapper orderMapper;


    public ProductService() {
    }

    /**
     * Get all products (with optional pagination)
     */
    public List<ProductDto> getAll() {
        return productRepo.findAll()
                .stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all active products (with pagination)
     */
    public List<ProductDto> getAllActive() {
        return productRepo.findProductsByFilters(null, null, null, null, null)
                .stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get product by ID
     */
    public Optional<ProductDto> getById(Long id) {
        return productRepo.findById(id)
                .map(orderMapper::toDto);
    }

    /**
     * Get products by category ID (with pagination)
     */
    public List<ProductDto> getByCategoryId(Long categoryId) {
        return productRepo.findByCategoryId(categoryId)
                .stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Search products by name (with pagination)
     */
    public Product searchByName(String name) {
        return productRepo.findByNameContainingIgnoreCase(name);
    }

    /**
     * Advanced product search with filters
     */
    public List<ProductDto> searchProducts(String name, Long categoryId, String brand,
                                        Double minPrice, Double maxPrice) {
        return productRepo.findProductsByFilters(name, categoryId, brand, minPrice, maxPrice)
                .stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get products by brand
     */
    public List<Product> getByBrand(String brand) {
        return productRepo.findByBrand(brand);
    }

    /**
     * Get all available brands
     */
    public List<String> getAllBrands() {
        return productRepo.findDistinctBrands();
    }

    /**
     * Add a new product
     */
    @Transactional
    public Product createFromDTO(ProductDto productDTO) {
        // Validate required fields
        if (productDTO.getName() == null || productDTO.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required");
        }
        if (productDTO.getCategoryId() == null) {
            throw new IllegalArgumentException("Category ID is required");
        }
        if (productDTO.getPrice() <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }

        // Find category
        Category category = categoryRepo.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + productDTO.getCategoryId()));

        // Create new product
        Product product = new Product();
        product.setName(productDTO.getName().trim());
        product.setDescription(productDTO.getDescription() != null ? productDTO.getDescription().trim() : "");
        product.setPrice(productDTO.getPrice());
        product.setQuantity(productDTO.getQuantity());
        product.setBrand(productDTO.getBrand() != null ? productDTO.getBrand().trim() : "");
        product.setModel(productDTO.getModel() != null ? productDTO.getModel().trim() : "");
        product.setActive(productDTO.isActive());
        product.setCategory(category);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        // Add images if provided
        if (productDTO.getImages() != null && !productDTO.getImages().isEmpty()) {
            for (ProductImageDto imageDTO : productDTO.getImages()) {
                if (imageDTO.getImageUrl() != null && !imageDTO.getImageUrl().trim().isEmpty()) {
                    ProductImage image = new ProductImage(product, imageDTO.getImageUrl().trim(), imageDTO.isMain());
                    product.getImages().add(image);
                }
            }
        }

        // Add specifications if provided
        if (productDTO.getSpecifications() != null && !productDTO.getSpecifications().isEmpty()) {
            for (ProductSpecificationDto specDTO : productDTO.getSpecifications()) {
                if (specDTO.getSpecName() != null && !specDTO.getSpecName().trim().isEmpty() &&
                        specDTO.getSpecValue() != null && !specDTO.getSpecValue().trim().isEmpty()) {
                    ProductSpecification spec = new ProductSpecification(product,
                            specDTO.getSpecName().trim(), specDTO.getSpecValue().trim());
                    product.getSpecifications().add(spec);
                }
            }
        }

        return productRepo.save(product);
    }

    /**
     * Update an existing product
     */
    @Transactional
    public ProductDto updateFromDTO(Long id, ProductDto productDTO) {
        Product existingProduct = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Validate required fields
        if (productDTO.getName() != null && productDTO.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Product name cannot be empty");
        }
        if (productDTO.getPrice() <= 0) {
            throw new IllegalArgumentException("Price must be greater than 0");
        }

        // Update basic fields
        if (productDTO.getName() != null) {
            existingProduct.setName(productDTO.getName().trim());
        }
        if (productDTO.getDescription() != null) {
            existingProduct.setDescription(productDTO.getDescription().trim());
        }
        existingProduct.setPrice(productDTO.getPrice());
        existingProduct.setQuantity(productDTO.getQuantity());
        if (productDTO.getBrand() != null) {
            existingProduct.setBrand(productDTO.getBrand().trim());
        }
        if (productDTO.getModel() != null) {
            existingProduct.setModel(productDTO.getModel().trim());
        }
        existingProduct.setActive(productDTO.isActive());
        existingProduct.setUpdatedAt(LocalDateTime.now());

        // Update category if provided
        if (productDTO.getCategoryId() != null && existingProduct.getCategory()!=null&&!productDTO.getCategoryId().equals(existingProduct.getCategory().getId())) {
            Category category = categoryRepo.findById(productDTO.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + productDTO.getCategoryId()));
            existingProduct.setCategory(category);
        }

        // Update images if provided
        if (productDTO.getImages() != null) {
            // Clear existing images that are not in the new list
            existingProduct.getImages().clear();

            for (ProductImageDto imageDTO : productDTO.getImages()) {
                if (imageDTO.getImageUrl() != null && !imageDTO.getImageUrl().trim().isEmpty()) {
                    ProductImage image = new ProductImage(existingProduct, imageDTO.getImageUrl().trim(), imageDTO.isMain());
                    existingProduct.getImages().add(image);
                }
            }
        }

        // Update specifications if provided
        if (productDTO.getSpecifications() != null) {
            existingProduct.getSpecifications().clear();

            for (ProductSpecificationDto specDTO : productDTO.getSpecifications()) {
                if (specDTO.getSpecName() != null && !specDTO.getSpecName().trim().isEmpty() &&
                        specDTO.getSpecValue() != null && !specDTO.getSpecValue().trim().isEmpty()) {
                    ProductSpecification spec = new ProductSpecification(existingProduct,
                            specDTO.getSpecName().trim(), specDTO.getSpecValue().trim());
                    existingProduct.getSpecifications().add(spec);
                }
            }
        }

         productRepo.save(existingProduct);
        return orderMapper.toDto(existingProduct);
    }


    /**
     * Delete a specification
     */
    @Transactional
    public Product deleteSpecification(Long productId, Long specId) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        product.setSpecifications(
                product.getSpecifications().stream()
                        .filter(spec -> !spec.getId().equals(specId))
                        .collect(Collectors.toList())
        );

        return productRepo.save(product);
    }
    /**
     * Delete an image
     */
    @Transactional
    public Product deleteImage(Long productId, String imageUrl) {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        product.setImages(
                product.getImages().stream()
                        .filter(img -> !img.getImageUrl().equals(imageUrl))
                        .collect(Collectors.toList())
        );

        return productRepo.save(product);
    }

    /**
     * Deactivate a product (soft delete)
     */
    @Transactional
    public Product deactivateProduct(Long id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setActive(false);
        product.setUpdatedAt(LocalDateTime.now());

        return productRepo.save(product);
    }

    /**
     * Activate a product
     */
    @Transactional
    public Product activateProduct(Long id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setActive(true);
        product.setUpdatedAt(LocalDateTime.now());

        return productRepo.save(product);
    }

    /**
     * Hard delete a product
     */
    @Transactional
    public void delete(Long id) {
        productRepo.deleteById(id);
    }

    /**
     * Get low stock products
     */
    public List<Product> getLowStockProducts(int threshold) {
        return productRepo.findByQuantityLessThan(threshold);
    }

    /**
     * Get out of stock products
     */
    public List<Product> getOutOfStockProducts() {
        return productRepo.findByQuantity(0);
    }

    /**
     * Get products by price range
     */
    public List<Product> getProductsByPriceRange(double minPrice, double maxPrice) {
        return productRepo.findByPriceBetween(minPrice, maxPrice);
    }

    /**
     * Get products by price range and category
     */
    public List<Product> getProductsByPriceRangeAndCategory(double minPrice, double maxPrice, Long categoryId) {
        return productRepo.findByPriceRangeAndCategory(minPrice, maxPrice, categoryId);
    }

    /**
     * Get popular products
     */
    public List<Product> getPopularProducts(int limit) {
        return productRepo.findPopularProducts(limit);
    }

    /**
     * Update product stock quantity
     */
    @Transactional
    public Product updateStock(Long id, int newQuantity) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setQuantity(newQuantity);
        product.setUpdatedAt(LocalDateTime.now());

        return productRepo.save(product);
    }

    /**
     * Adjust stock (add or remove quantity)
     */
    @Transactional
    public Product adjustStock(Long id, int quantityChange) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        int newQuantity = product.getQuantity() + quantityChange;
        if (newQuantity < 0) {
            newQuantity = 0; // Prevent negative stock
        }

        product.setQuantity(newQuantity);
        product.setUpdatedAt(LocalDateTime.now());

        return productRepo.save(product);
    }
}