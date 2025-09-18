package com.example.food.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicsConfig {
  @Bean public NewTopic orderCreated() { return TopicBuilder.name("fd.order.created.v1").partitions(3).replicas(1).build(); }
  @Bean public NewTopic paymentRequested() { return TopicBuilder.name("fd.payment.requested.v1").partitions(3).replicas(1).build(); }
  @Bean public NewTopic restaurantAcceptanceRequested() { return TopicBuilder.name("fd.restaurant.acceptance-requested.v1").partitions(3).replicas(1).build(); }
  @Bean public NewTopic deliveryRequested() { return TopicBuilder.name("fd.delivery.requested.v1").partitions(3).replicas(1).build(); }
  @Bean public NewTopic paymentFailed() { return TopicBuilder.name("fd.payment.failed.v1").partitions(3).replicas(1).build(); }
}
