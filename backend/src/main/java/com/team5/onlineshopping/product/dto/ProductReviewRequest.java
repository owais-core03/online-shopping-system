package com.team5.onlineshopping.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ProductReviewRequest {

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    private Integer rating;

    @Size(max = 2000, message = "Comment cannot exceed 2000 characters")
    private String comment;

    private Long orderItemId;

    public ProductReviewRequest() {
    }

    public ProductReviewRequest(Integer rating, String comment) {
        this.rating = rating;
        this.comment = comment;
    }

    public ProductReviewRequest(Integer rating, String comment, Long orderItemId) {
        this.rating = rating;
        this.comment = comment;
        this.orderItemId = orderItemId;
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

    public Long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(Long orderItemId) {
        this.orderItemId = orderItemId;
    }
}
