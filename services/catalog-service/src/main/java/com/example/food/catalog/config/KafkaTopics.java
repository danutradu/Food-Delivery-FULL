package com.example.food.catalog.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class KafkaTopics {

  @Value("${kafka.topics.restaurant-created}")
  private String restaurantCreated;
}
