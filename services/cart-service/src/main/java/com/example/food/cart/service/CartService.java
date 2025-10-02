package com.example.food.cart.service;

import com.example.food.cart.config.KafkaTopics;
import com.example.food.cart.dto.AddItemRequest;
import com.example.food.cart.dto.CartResponse;
import com.example.food.cart.exception.*;
import com.example.food.cart.model.CartEntity;
import com.example.food.cart.repository.CartRepository;
import com.example.food.cart.util.CartFactory;
import com.example.food.common.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    private final CartRepository cartRepository;
    private final OutboxService outboxService;
    private final KafkaTopics topics;
    private final MenuItemCacheService menuItemCacheService;

    public CartResponse addItem(AddItemRequest req, UUID customerUserId) {
        log.info("Adding item to cart customerUserId={} restaurantId={} menuItemId={} quantity={}", customerUserId, req.restaurantId(), req.menuItemId(), req.quantity());

        var menuItem = menuItemCacheService.getMenuItem(req.restaurantId(), req.menuItemId());
        if (menuItem == null || !menuItem.available()) {
            throw new MenuItemNotFoundException(req.restaurantId(), req.menuItemId());
        }

        var cart = cartRepository.findByCustomerUserId(customerUserId).orElseGet(() -> {
            var nc = new CartEntity();
            nc.setId(UUID.randomUUID());
            nc.setCustomerUserId(customerUserId);
            nc.setRestaurantId(req.restaurantId());
            nc.setCurrency(menuItem.currency());
            return nc;
        });

        if (!cart.getRestaurantId().equals(req.restaurantId())) {
            throw new RestaurantMismatchException(cart.getRestaurantId(), req.restaurantId());
        }

        var existingItem = cart.getItems().stream()
                .filter(item -> item.getMenuItemId().equals(req.menuItemId()))
                .findFirst();

        if (existingItem.isPresent()) {
            var item = existingItem.get();
            int oldQuantity = item.getQuantity();
            item.setQuantity(oldQuantity + req.quantity());
            log.info("Merged with existing item menuItemId={} oldQuantity={} added={} newQuantity={}", req.menuItemId(), oldQuantity, req.quantity(), item.getQuantity());
        } else {
            var item = CartFactory.createCartItem(req.menuItemId(), menuItem.name(), menuItem.priceCents(), req.quantity());
            item.setCart(cart);
            cart.getItems().add(item);
            log.info("Added new item to cart menuItemId={} quantity={}", req.menuItemId(), req.quantity());
        }

        var savedCart = cartRepository.save(cart);
        return CartFactory.createCartResponse(savedCart);
    }

    public CartResponse getCart(UUID customerUserId) {
        var cart = cartRepository.findByCustomerUserId(customerUserId).orElse(null);
        return cart != null ? CartFactory.createCartResponse(cart) : null;
    }

    public CartResponse updateItemQuantity(UUID customerUserId, UUID itemId, int quantity) {
        log.info("Updating item quantity customerUserId={} itemId={} quantity={}", customerUserId, itemId, quantity);

        var cart = cartRepository.findByCustomerUserId(customerUserId)
                .orElseThrow(() -> new CartNotFoundForUserException(customerUserId));

        var item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new CartItemNotFoundException(itemId));

        if (quantity <= 0) {
            cart.getItems().remove(item);
            log.info("Item removed due to zero quantity itemId={}", itemId);
        } else {
            item.setQuantity(quantity);
            log.info("Item quantity updated itemId={} newQuantity={}", itemId, quantity);
        }

        var savedCart = cartRepository.save(cart);
        return CartFactory.createCartResponse(savedCart);
    }

    public CartResponse removeItem(UUID customerUserId, UUID itemId) {
        log.info("Removing item customerUserId={} itemId={}", customerUserId, itemId);

        var cart = cartRepository.findByCustomerUserId(customerUserId)
                .orElseThrow(() -> new CartNotFoundForUserException(customerUserId));

        var removed = cart.getItems().removeIf(item -> item.getId().equals(itemId));
        if (!removed) {
            throw new CartItemNotFoundException(itemId);
        }

        log.info("Item removed from cart itemId={}", itemId);
        var savedCart = cartRepository.save(cart);
        return CartFactory.createCartResponse(savedCart);
    }

    public void clearCart(UUID customerUserId) {
        log.info("Clearing cart customerUserId={}", customerUserId);

        var cart = cartRepository.findByCustomerUserId(customerUserId);
        if (cart.isPresent()) {
            cartRepository.delete(cart.get());
            log.info("Cart cleared customerUserId={}", customerUserId);
        }
    }

    @Transactional
    public void checkout(UUID customerUserId) {
        log.info("CartCheckedOut customerUserId={}", customerUserId);

        var cart = cartRepository.findByCustomerUserId(customerUserId)
                .orElseThrow(() -> new CartNotFoundForUserException(customerUserId));

        if (cart.getItems().isEmpty()) {
            throw new EmptyCartException(customerUserId);
        }

        var event = CartFactory.createCartCheckedOut(cart);
        outboxService.publish(topics.getCartCheckedOut(), event.getCartId().toString(), event);

        cartRepository.delete(cart);
        log.info("Cart checked out and deleted cartId={} customerUserId={} items={} total={}{} restaurantId={}",
                cart.getId(), customerUserId, cart.getItemCount(), cart.getTotalCents(), cart.getCurrency(), cart.getRestaurantId());
    }
}
