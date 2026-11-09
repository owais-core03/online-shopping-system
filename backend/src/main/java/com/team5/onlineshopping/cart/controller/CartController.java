package com.team5.onlineshopping.cart.controller;

import com.team5.onlineshopping.cart.dto.CartItemRequest;
import com.team5.onlineshopping.cart.dto.CartItemUpdateRequest;
import com.team5.onlineshopping.cart.dto.CartResponse;
import com.team5.onlineshopping.cart.service.CartService;
import com.team5.onlineshopping.common.dto.ApiResponse;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(@AuthenticationPrincipal CustomUserDetails userDetails) {
        CartResponse cart = cartService.getCustomerCart(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved successfully", cart));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CartItemRequest request) {
        CartResponse cart = cartService.addToCart(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Item added to cart", cart));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long itemId,
            @Valid @RequestBody CartItemUpdateRequest request) {
        CartResponse cart = cartService.updateCartItemQuantity(userDetails.getId(), itemId, request);
        return ResponseEntity.ok(ApiResponse.success("Cart item quantity updated", cart));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long itemId) {
        CartResponse cart = cartService.removeCartItem(userDetails.getId(), itemId);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", cart));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<CartResponse>> clearCart(@AuthenticationPrincipal CustomUserDetails userDetails) {
        CartResponse cart = cartService.clearCart(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully", cart));
    }
}
