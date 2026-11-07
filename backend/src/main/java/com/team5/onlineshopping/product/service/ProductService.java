package com.team5.onlineshopping.product.service;

import com.team5.onlineshopping.common.enums.Role;
import com.team5.onlineshopping.common.exception.BadRequestException;
import com.team5.onlineshopping.common.exception.ResourceNotFoundException;
import com.team5.onlineshopping.product.dto.ProductCreateRequest;
import com.team5.onlineshopping.product.dto.ProductResponse;
import com.team5.onlineshopping.product.dto.ProductReviewRequest;
import com.team5.onlineshopping.product.dto.ProductReviewResponse;
import com.team5.onlineshopping.product.dto.ProductUpdateRequest;
import com.team5.onlineshopping.product.entity.Product;
import com.team5.onlineshopping.product.entity.ProductReview;
import com.team5.onlineshopping.product.repository.ProductRepository;
import com.team5.onlineshopping.product.repository.ProductReviewRepository;
import com.team5.onlineshopping.user.entity.User;
import com.team5.onlineshopping.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductReviewRepository productReviewRepository;
    private final UserRepository userRepository;

    public ProductService(ProductRepository productRepository,
                          ProductReviewRepository productReviewRepository,
                          UserRepository userRepository) {
        this.productRepository = productRepository;
        this.productReviewRepository = productReviewRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllActiveProducts() {
        return productRepository.findByIsActiveTrue()
                .stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return ProductResponse.fromEntity(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String query, String category, String brand) {
        List<Product> products;
        if (query != null && !query.trim().isEmpty()) {
            products = productRepository.searchActiveProducts(query.trim());
        } else if (category != null && !category.trim().isEmpty()) {
            products = productRepository.findByCategoryIgnoreCaseAndIsActiveTrue(category.trim());
        } else if (brand != null && !brand.trim().isEmpty()) {
            products = productRepository.findByBrandIgnoreCaseAndIsActiveTrue(brand.trim());
        } else {
            products = productRepository.findByIsActiveTrue();
        }

        return products.stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsBySeller(Long sellerId) {
        return productRepository.findBySellerId(sellerId)
                .stream()
                .map(ProductResponse::fromEntity)
                .toList();
    }

    @Transactional
    public ProductResponse createProduct(Long sellerId, ProductCreateRequest request) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found with id: " + sellerId));

        if (seller.getRole() != Role.SELLER && seller.getRole() != Role.ADMIN) {
            throw new BadRequestException("Only sellers or admins can create products");
        }

        Product product = new Product(
                seller,
                request.getName().trim(),
                request.getBrand().trim(),
                request.getCategory().trim(),
                request.getDescription(),
                request.getSpecifications(),
                request.getPrice(),
                request.getAvailableStock()
        );

        Product savedProduct = productRepository.save(product);
        return ProductResponse.fromEntity(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(Long productId, Long sellerId, ProductUpdateRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        User user = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + sellerId));

        if (!product.getSeller().getId().equals(sellerId) && user.getRole() != Role.ADMIN) {
            throw new BadRequestException("You are not authorized to update this product");
        }

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            product.setName(request.getName().trim());
        }
        if (request.getBrand() != null && !request.getBrand().trim().isEmpty()) {
            product.setBrand(request.getBrand().trim());
        }
        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            product.setCategory(request.getCategory().trim());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getSpecifications() != null) {
            product.setSpecifications(request.getSpecifications());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getAvailableStock() != null) {
            product.setAvailableStock(request.getAvailableStock());
        }
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        Product updatedProduct = productRepository.save(product);
        return ProductResponse.fromEntity(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long productId, Long sellerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        User user = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + sellerId));

        if (!product.getSeller().getId().equals(sellerId) && user.getRole() != Role.ADMIN) {
            throw new BadRequestException("You are not authorized to delete this product");
        }

        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional
    public ProductReviewResponse addReview(Long productId, Long customerId, ProductReviewRequest request) {
        Product product = productRepository.findByIdAndIsActiveTrue(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

        if (productReviewRepository.findByProductIdAndCustomerId(productId, customerId).isPresent()) {
            throw new BadRequestException("You have already reviewed this product");
        }

        ProductReview review = new ProductReview(
                product,
                customer,
                request.getRating(),
                request.getComment()
        );

        ProductReview savedReview = productReviewRepository.save(review);

        // Recalculate product rating & review count
        List<ProductReview> reviews = productReviewRepository.findByProductIdAndIsApprovedTrue(productId);
        int totalReviews = reviews.size();
        double sum = reviews.stream().mapToInt(ProductReview::getRating).sum();
        BigDecimal average = totalReviews > 0
                ? BigDecimal.valueOf(sum / totalReviews).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        product.setReviewCount(totalReviews);
        product.setAverageRating(average);
        productRepository.save(product);

        return ProductReviewResponse.fromEntity(savedReview);
    }

    @Transactional(readOnly = true)
    public List<ProductReviewResponse> getProductReviews(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }

        return productReviewRepository.findByProductIdAndIsApprovedTrue(productId)
                .stream()
                .map(ProductReviewResponse::fromEntity)
                .toList();
    }
}
