package com.team5.onlineshopping.checkout.dto;

import com.team5.onlineshopping.common.enums.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class CheckoutRequest {

    @NotNull(message = "Delivery address is required")
    @Valid
    private AddressDto deliveryAddress;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String couponCode;

    public CheckoutRequest() {
    }

    public CheckoutRequest(AddressDto deliveryAddress, PaymentMethod paymentMethod, String couponCode) {
        this.deliveryAddress = deliveryAddress;
        this.paymentMethod = paymentMethod;
        this.couponCode = couponCode;
    }

    public AddressDto getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(AddressDto deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }
}
