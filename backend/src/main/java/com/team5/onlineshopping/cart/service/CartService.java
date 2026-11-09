package com.team5.onlineshopping.cart.service;

import com.team5.onlineshopping.cart.dto.CartItemRequest;
import com.team5.onlineshopping.cart.dto.CartItemUpdateRequest;
import com.team5.onlineshopping.cart.dto.CartResponse;

public interface CartService {

    CartResponse getCustomerCart(Long customerId);

    CartResponse addToCart(Long customerId, CartItemRequest request);

    CartResponse updateCartItemQuantity(Long customerId, Long itemId, CartItemUpdateRequest request);

    CartResponse removeCartItem(Long customerId, Long itemId);

    CartResponse clearCart(Long customerId);
}
