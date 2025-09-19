package com.example.food.cart.service;

import com.example.food.cart.dto.AddItemRequest;
import com.example.food.cart.exception.CartNotFoundException;
import com.example.food.cart.mapper.CartMapper;
import com.example.food.cart.model.CartEntity;
import com.example.food.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    private final CartRepository carts;
    private final KafkaTemplate<String, SpecificRecord> kafka;
    private final CartMapper mapper;

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

    public void checkout(UUID cartId) {
        log.info("CartCheckedOut cartId={}", cartId);

        var cart = carts.findById(cartId)
                .orElseThrow(() -> new CartNotFoundException(cartId.toString()));

        var event = mapper.toCheckedOut(cart);
        ProducerRecord<String, SpecificRecord> record = new ProducerRecord<>("fd.cart.checked-out.v1", cart.getId().toString(), event);
        record.headers().add("eventType", "fd.cart.CartCheckedOutV1".getBytes());
        record.headers().add("eventId", event.getEventId().toString().getBytes());

        kafka.send(record).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish cart checked out event cartId={}", cartId, ex);
            } else {
                log.info("Published cart checked out event cartId={}", cartId);
            }
        });
    }
}
