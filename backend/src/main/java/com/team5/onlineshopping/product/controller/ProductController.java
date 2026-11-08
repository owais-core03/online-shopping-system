package com.team5.onlineshopping.product.controller;

import com.team5.onlineshopping.common.dto.ApiResponse;
import com.team5.onlineshopping.product.dto.ProductCreateRequest;
import com.team5.onlineshopping.product.dto.ProductResponse;
import com.team5.onlineshopping.product.dto.ProductReviewRequest;
import com.team5.onlineshopping.product.dto.ProductReviewResponse;
import com.team5.onlineshopping.product.dto.ProductUpdateRequest;
import com.team5.onlineshopping.product.service.ProductService;
import com.team5.onlineshopping.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "id") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDirection) {

        if (page != null || size != null) {
            int pageIndex = (page != null && page >= 0) ? page : 0;
            int pageSize = (size != null && size > 0) ? Math.min(size, 100) : 20;

            org.springframework.data.domain.Sort.Direction direction =
                    "desc".equalsIgnoreCase(sortDirection)
                            ? org.springframework.data.domain.Sort.Direction.DESC
                            : org.springframework.data.domain.Sort.Direction.ASC;

            String sortField = sanitizeSortField(sortBy);
            org.springframework.data.domain.Pageable pageable =
                    org.springframework.data.domain.PageRequest.of(pageIndex, pageSize, org.springframework.data.domain.Sort.by(direction, sortField));

            org.springframework.data.domain.Page<ProductResponse> productPage =
                    productService.searchProducts(query, category, brand, pageable);

            return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", productPage.getContent()));
        }

        List<ProductResponse> products = productService.searchProducts(query, category, brand);
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", products));
    }

    private String sanitizeSortField(String sortBy) {
        if (sortBy == null) {
            return "id";
        }
        return switch (sortBy.toLowerCase()) {
            case "name" -> "name";
            case "price" -> "price";
            case "brand" -> "brand";
            case "category" -> "category";
            case "averagerating", "rating" -> "averageRating";
            case "createdat" -> "createdAt";
            default -> "id";
        };
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product details retrieved successfully", product));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<ApiResponse<List<ProductReviewResponse>>> getProductReviews(@PathVariable Long id) {
        List<ProductReviewResponse> reviews = productService.getProductReviews(id);
        return ResponseEntity.ok(ApiResponse.success("Product reviews retrieved successfully", reviews));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProductCreateRequest request) {
        ProductResponse createdProduct = productService.createProduct(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", createdProduct));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProductUpdateRequest request) {
        ProductResponse updatedProduct = productService.updateProduct(id, userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", updatedProduct));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        productService.deleteProduct(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    @GetMapping("/seller/my-products")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getMyProducts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ProductResponse> products = productService.getProductsBySeller(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Seller products retrieved successfully", products));
    }

    @PostMapping("/{id}/reviews")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductReviewResponse>> addReview(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ProductReviewRequest request) {
        ProductReviewResponse review = productService.addReview(id, userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted successfully", review));
    }
}
