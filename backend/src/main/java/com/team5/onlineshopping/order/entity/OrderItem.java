package com.team5.onlineshopping.order.entity;

import com.team5.onlineshopping.common.enums.OrderItemStatus;
import com.team5.onlineshopping.product.entity.Product;
import com.team5.onlineshopping.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @NotNull
    @Min(1)
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull
    @DecimalMin("0.00")
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @NotNull
    @DecimalMin("0.00")
    @Column(name = "allocated_discount", nullable = false, precision = 10, scale = 2)
    private BigDecimal allocatedDiscount = BigDecimal.ZERO;

    @NotNull
    @DecimalMin("0.00")
    @Column(name = "net_paid_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal netPaidAmount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false, length = 30)
    private OrderItemStatus itemStatus = OrderItemStatus.PROCESSING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public OrderItem() {
    }

    public OrderItem(Order order, Product product, User seller, Integer quantity,
                     BigDecimal unitPrice, BigDecimal allocatedDiscount) {
        this.order = order;
        this.product = product;
        this.seller = seller;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.allocatedDiscount = allocatedDiscount != null ? allocatedDiscount : BigDecimal.ZERO;
        BigDecimal gross = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.netPaidAmount = gross.subtract(this.allocatedDiscount).max(BigDecimal.ZERO);
        this.itemStatus = OrderItemStatus.PROCESSING;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        if (this.allocatedDiscount == null) {
            this.allocatedDiscount = BigDecimal.ZERO;
        }
        if (this.netPaidAmount == null && this.unitPrice != null && this.quantity != null) {
            BigDecimal gross = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
            this.netPaidAmount = gross.subtract(this.allocatedDiscount).max(BigDecimal.ZERO);
        }
        if (this.itemStatus == null) {
            this.itemStatus = OrderItemStatus.PROCESSING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public User getSeller() {
        return seller;
    }

    public void setSeller(User seller) {
        this.seller = seller;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getAllocatedDiscount() {
        return allocatedDiscount;
    }

    public void setAllocatedDiscount(BigDecimal allocatedDiscount) {
        this.allocatedDiscount = allocatedDiscount;
    }

    public BigDecimal getNetPaidAmount() {
        return netPaidAmount;
    }

    public void setNetPaidAmount(BigDecimal netPaidAmount) {
        this.netPaidAmount = netPaidAmount;
    }

    public OrderItemStatus getItemStatus() {
        return itemStatus;
    }

    public void setItemStatus(OrderItemStatus itemStatus) {
        this.itemStatus = itemStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
