package com.team5.onlineshopping.product.repository;

import com.team5.onlineshopping.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    List<Product> findBySellerId(Long sellerId);

    List<Product> findBySellerId(Long sellerId, Pageable pageable);

    List<Product> findByCategoryIgnoreCaseAndIsActiveTrue(String category);

    List<Product> findByBrandIgnoreCaseAndIsActiveTrue(String brand);

    List<Product> findByIsActiveTrue();

    Page<Product> findByIsActiveTrue(Pageable pageable);

    Optional<Product> findByIdAndIsActiveTrue(Long id);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND " +
           "(:query IS NULL OR :query = '' OR " +
           " LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(p.category) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:category IS NULL OR :category = '' OR LOWER(p.category) = LOWER(:category)) AND " +
           "(:brand IS NULL OR :brand = '' OR LOWER(p.brand) = LOWER(:brand))")
    List<Product> searchActiveProducts(
            @Param("query") String query,
            @Param("category") String category,
            @Param("brand") String brand);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND " +
           "(:query IS NULL OR :query = '' OR " +
           " LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(p.brand) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(p.category) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           " LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:category IS NULL OR :category = '' OR LOWER(p.category) = LOWER(:category)) AND " +
           "(:brand IS NULL OR :brand = '' OR LOWER(p.brand) = LOWER(:brand))")
    Page<Product> searchActiveProducts(
            @Param("query") String query,
            @Param("category") String category,
            @Param("brand") String brand,
            Pageable pageable);
}
