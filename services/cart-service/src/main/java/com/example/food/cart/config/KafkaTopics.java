package com.example.food.cart.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class KafkaTopics {

  @Value("${kafka.topics.cart-checked-out}")
  private String cartCheckedOut;
}
