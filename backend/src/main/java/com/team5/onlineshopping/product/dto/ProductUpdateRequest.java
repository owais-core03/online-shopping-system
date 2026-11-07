package com.team5.onlineshopping.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProductUpdateRequest {

    @Size(max = 200, message = "Product name cannot exceed 200 characters")
    private String name;

    @Size(max = 100, message = "Brand cannot exceed 100 characters")
    private String brand;

    @Size(max = 100, message = "Category cannot exceed 100 characters")
    private String category;

    private String description;

    private String specifications;

    @DecimalMin(value = "0.01", message = "Price must be greater than zero")
    private BigDecimal price;

    @Min(value = 0, message = "Available stock cannot be negative")
    private Integer availableStock;

    private Boolean active;

    public ProductUpdateRequest() {
    }

    public ProductUpdateRequest(String name, String brand, String category, String description,
                                String specifications, BigDecimal price, Integer availableStock, Boolean active) {
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.description = description;
        this.specifications = specifications;
        this.price = price;
        this.availableStock = availableStock;
        this.active = active;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSpecifications() {
        return specifications;
    }

    public void setSpecifications(String specifications) {
        this.specifications = specifications;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public void setAvailableStock(Integer availableStock) {
        this.availableStock = availableStock;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
