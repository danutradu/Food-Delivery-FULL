package com.example.food.delivery.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicsConfig {
  @Bean public NewTopic courierAssigned() { return TopicBuilder.name("fd.delivery.courier-assigned.v1").partitions(3).replicas(1).build(); }
  @Bean public NewTopic pickedUp() { return TopicBuilder.name("fd.delivery.picked-up.v1").partitions(3).replicas(1).build(); }
  @Bean public NewTopic delivered() { return TopicBuilder.name("fd.delivery.delivered.v1").partitions(3).replicas(1).build(); }
}
