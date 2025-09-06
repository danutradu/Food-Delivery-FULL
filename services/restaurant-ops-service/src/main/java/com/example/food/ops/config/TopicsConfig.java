package com.example.food.ops.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicsConfig {
  @Bean public NewTopic restaurantAccepted() { return TopicBuilder.name("fd.restaurant.accepted.v1").partitions(3).replicas(1).build(); }
  @Bean public NewTopic restaurantRejected() { return TopicBuilder.name("fd.restaurant.rejected.v1").partitions(3).replicas(1).build(); }
  @Bean public NewTopic orderReady() { return TopicBuilder.name("fd.restaurant.order-ready.v1").partitions(3).replicas(1).build(); }
}
