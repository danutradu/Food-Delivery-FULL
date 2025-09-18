package com.example.food.payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicsConfig {
  @Bean public NewTopic paymentAuthorized() { return TopicBuilder.name("fd.payment.authorized.v1").partitions(3).replicas(1).build(); }
  @Bean public NewTopic paymentFailed() { return TopicBuilder.name("fd.payment.failed.v1").partitions(3).replicas(1).build(); }
}
