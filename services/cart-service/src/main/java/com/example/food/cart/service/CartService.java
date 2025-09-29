package com.example.food.cart.service;

import com.example.food.cart.config.KafkaTopics;
import com.example.food.cart.dto.AddItemRequest;
import com.example.food.cart.exception.CartNotFoundException;
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

    public CartEntity addItem(UUID cartId, AddItemRequest req, UUID customerUserId) {
        log.info("CartItemAdded cartId={} name={} qty={} priceCents={}", cartId, req.name(), req.quantity(), req.unitPriceCents());

        var cart = cartRepository.findById(cartId).orElseGet(() -> {
            var nc = new CartEntity();
            nc.setId(cartId);
            nc.setCustomerUserId(customerUserId);
            return nc;
        });

        var item = CartFactory.createCartItem(req);
        item.setCart(cart);
        cart.getItems().add(item);

        return cartRepository.save(cart);
    }

    @Transactional
    public void checkout(UUID cartId) {
        log.info("CartCheckedOut cartId={}", cartId);

        var cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId.toString()));

        var event = CartFactory.createCartCheckedOut(cart);
        outboxService.publish(topics.getCartCheckedOut(), cart.getItems().toString(), event);

        cartRepository.delete(cart);
        log.info("Cart checked out and deleted cartId={}", cartId);
    }
}
