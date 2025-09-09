package com.example.food.cart.service;

import com.example.food.cart.dto.AddItemRequest;
import com.example.food.cart.mapper.CartMapper;
import com.example.food.cart.model.CartEntity;
import com.example.food.cart.repository.CartRepository;
import fd.cart.CartCheckedOutV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    private final CartRepository carts;
    private final KafkaTemplate<String, Object> kafka;
    private final CartMapper mapper;

    @Transactional
    public CartEntity addItem(UUID cartId, AddItemRequest req, UUID customerUserId) {
        log.info("CartItemAdded cartId={} name={} qty={} priceCents={}", cartId, req.name(), req.quantity(), req.unitPriceCents());

        var cart = carts.findById(cartId).orElseGet(() -> {
            var nc = new CartEntity();
            nc.setId(cartId);
            nc.setCustomerUserId(customerUserId);
            return nc;
        });

        var item = mapper.toItem(req);
        item.setCart(cart);
        cart.getItems().add(item);

        return carts.save(cart);
    }

    @Transactional
    public void checkout(UUID cartId) {
        log.info("CartCheckedOut cartId={}", cartId);

        var cart = carts.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found: " + cartId));

        var event = mapper.toCheckedOut(cart);
        ProducerRecord<String, Object> rec = new ProducerRecord<>("fd.cart.checked-out.v1", cart.getId().toString(), event);
        rec.headers().add("eventType", "fd.cart.CartCheckedOutV1".getBytes());
        kafka.send(rec);
    }
}
