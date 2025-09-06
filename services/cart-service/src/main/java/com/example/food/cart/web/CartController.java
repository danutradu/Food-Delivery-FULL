package com.example.food.cart.web;

import com.example.food.cart.domain.CartEntity;
import com.example.food.cart.domain.CartItemEntity;
import com.example.food.cart.repo.CartRepository;
import com.example.food.cart.web.dto.AddItemRequest;
import com.example.food.cart.web.mapper.CartMappers;
import fd.cart.CartCheckedOutV1;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CartController {

  private final CartRepository carts;
  private final KafkaTemplate<String, Object> kafka;
  private final CartMappers mapper;

  @PreAuthorize("hasRole('CUSTOMER')")
  @PostMapping("/carts/{cartId}/items")
  public CartEntity addItem(@PathVariable("cartId") UUID cartId, @Valid @RequestBody AddItemRequest req, Authentication auth) {
    log.info("CartItemAdded cartId={} name={} qty={} priceCents={}", cartId, req.name(), req.quantity(), req.unitPriceCents());
    CartEntity c = carts.findById(cartId).orElseGet(() -> {
      CartEntity nc = new CartEntity();
      nc.setId(cartId);
      nc.setCustomerUserId(UUID.fromString(((Jwt)auth.getPrincipal()).getSubject()));
      return nc;
    });
    CartItemEntity it = mapper.toItem(req);
    it.setCart(c);
    c.getItems().add(it);
    return carts.save(c);
  }

  @PreAuthorize("hasRole('CUSTOMER')")
  @PostMapping("/carts/{cartId}/checkout")
  public Map<String,Object> checkout(@PathVariable("cartId") UUID cartId) {
    log.info("CartCheckedOut cartId={}", cartId);
    CartEntity c = carts.findById(cartId).orElseThrow();
    CartCheckedOutV1 evt = mapper.toCheckedOut(c);
    ProducerRecord<String,Object> rec = new ProducerRecord<>("fd.cart.checked-out.v1", c.getId().toString(), evt);
    rec.headers().add("eventType","fd.cart.CartCheckedOutV1".getBytes());
    kafka.send(rec);
    return Map.of("status","CHECKED_OUT");
  }
}
