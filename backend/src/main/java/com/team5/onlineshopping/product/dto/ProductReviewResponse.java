package com.team5.onlineshopping.product.dto;

import com.team5.onlineshopping.product.entity.ProductReview;

import java.time.Instant;

public class ProductReviewResponse {

    private Long id;
    private Long productId;
    private Long customerId;
    private String customerName;
    private Integer rating;
    private String comment;
    private boolean approved;
    private Instant createdAt;

    public ProductReviewResponse() {
    }

    public static ProductReviewResponse fromEntity(ProductReview review) {
        ProductReviewResponse response = new ProductReviewResponse();
        response.setId(review.getId());
        if (review.getProduct() != null) {
            response.setProductId(review.getProduct().getId());
        }
        if (review.getCustomer() != null) {
            response.setCustomerId(review.getCustomer().getId());
            response.setCustomerName(review.getCustomer().getFullName());
        }
        response.setRating(review.getRating());
        response.setComment(review.getComment());
        response.setApproved(review.isApproved());
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
