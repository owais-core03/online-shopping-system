package com.team5.onlineshopping.cart.service;

import com.team5.onlineshopping.cart.dto.CartItemRequest;
import com.team5.onlineshopping.cart.dto.CartItemResponse;
import com.team5.onlineshopping.cart.dto.CartItemUpdateRequest;
import com.team5.onlineshopping.cart.dto.CartResponse;
import com.team5.onlineshopping.cart.entity.Cart;
import com.team5.onlineshopping.cart.entity.CartItem;
import com.team5.onlineshopping.cart.repository.CartItemRepository;
import com.team5.onlineshopping.cart.repository.CartRepository;
import com.team5.onlineshopping.common.exception.BadRequestException;
import com.team5.onlineshopping.common.exception.ResourceNotFoundException;
import com.team5.onlineshopping.product.entity.Product;
import com.team5.onlineshopping.product.repository.ProductRepository;
import com.team5.onlineshopping.user.entity.User;
import com.team5.onlineshopping.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           ProductRepository productRepository,
                           UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCustomerCart(Long customerId) {
        Cart cart = getOrCreateCartEntity(customerId);
        return mapToCartResponse(cart);
    }

    @Override
    public CartResponse addToCart(Long customerId, CartItemRequest request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (!product.isActive()) {
            throw new BadRequestException("Product is inactive and cannot be added to cart");
        }

        Cart cart = getOrCreateCartEntity(customerId);

        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        int totalRequestedQuantity = request.getQuantity();
        if (existingItem != null) {
            totalRequestedQuantity += existingItem.getQuantity();
        }

        if (product.getAvailableStock() < totalRequestedQuantity) {
            throw new IllegalStateException("Insufficient stock for product '" + product.getName() +
                    "'. Requested: " + totalRequestedQuantity + ", Available: " + product.getAvailableStock());
        }

        if (existingItem != null) {
            existingItem.setQuantity(totalRequestedQuantity);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = new CartItem(cart, product, request.getQuantity());
            cart.addItem(newItem);
            cartItemRepository.save(newItem);
        }

        return mapToCartResponse(cart);
    }

    @Override
    public CartResponse updateCartItemQuantity(Long customerId, Long itemId, CartItemUpdateRequest request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than zero");
        }

        Cart cart = getOrCreateCartEntity(customerId);

        CartItem item = cartItemRepository.findByIdAndCartCustomerId(itemId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        Product product = item.getProduct();
        if (!product.isActive()) {
            throw new BadRequestException("Product is no longer active");
        }

        if (product.getAvailableStock() < request.getQuantity()) {
            throw new IllegalStateException("Insufficient stock for product '" + product.getName() +
                    "'. Requested: " + request.getQuantity() + ", Available: " + product.getAvailableStock());
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        return mapToCartResponse(cart);
    }

    @Override
    public CartResponse removeCartItem(Long customerId, Long itemId) {
        Cart cart = getOrCreateCartEntity(customerId);

        CartItem item = cartItemRepository.findByIdAndCartCustomerId(itemId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with id: " + itemId));

        cart.removeItem(item);
        cartItemRepository.delete(item);

        return mapToCartResponse(cart);
    }

    @Override
    public CartResponse clearCart(Long customerId) {
        Cart cart = getOrCreateCartEntity(customerId);
        cart.clearItems();
        cartRepository.save(cart);
        return mapToCartResponse(cart);
    }

    private Cart getOrCreateCartEntity(Long customerId) {
        return cartRepository.findByCustomerIdWithItems(customerId)
                .orElseGet(() -> {
                    User customer = userRepository.findById(customerId)
                            .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
                    Cart newCart = new Cart(customer);
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse mapToCartResponse(Cart cart) {
        CartResponse response = new CartResponse();
        response.setId(cart.getId());
        response.setCustomerId(cart.getCustomer().getId());

        List<CartItemResponse> itemResponses = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        int totalItemsCount = 0;

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            BigDecimal unitPrice = product.getPrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, RoundingMode.HALF_UP);

            subtotal = subtotal.add(lineTotal);
            totalItemsCount += item.getQuantity();

            CartItemResponse itemResp = new CartItemResponse();
            itemResp.setId(item.getId());
            itemResp.setProductId(product.getId());
            itemResp.setProductName(product.getName());
            itemResp.setProductBrand(product.getBrand());
            itemResp.setProductCategory(product.getCategory());
            itemResp.setUnitPrice(unitPrice);
            itemResp.setQuantity(item.getQuantity());
            itemResp.setLineTotal(lineTotal);
            itemResp.setAvailableStock(product.getAvailableStock());
            itemResp.setInStock(product.isActive() && product.getAvailableStock() >= item.getQuantity());

            itemResponses.add(itemResp);
        }

        BigDecimal shippingFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(shippingFee).setScale(2, RoundingMode.HALF_UP);

        response.setItems(itemResponses);
        response.setTotalItems(totalItemsCount);
        response.setSubtotal(subtotal);
        response.setShippingFee(shippingFee);
        response.setTotal(total);

        return response;
    }
}
