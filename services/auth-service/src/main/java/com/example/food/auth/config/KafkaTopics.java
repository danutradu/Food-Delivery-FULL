package com.example.food.auth.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopics {
  @Bean public NewTopic userRegistered() { return TopicBuilder.name("fd.user.registered.v1").partitions(3).replicas(1).build(); }
}
