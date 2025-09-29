package com.example.food.ops.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RequiredArgsConstructor
public class TopicsConfig {

    private final KafkaTopics topics;

    @Value("${kafka.topics.partitions:3}")
    private int partitions;

    @Value("${kafka.topics.replicas:3}")
    private int replicas;

    @Bean
    public NewTopic restaurantAccepted() {
        return TopicBuilder.name(topics.getRestaurantAccepted()).partitions(partitions).replicas(replicas).build();
    }

    @Bean
    public NewTopic restaurantRejected() {
        return TopicBuilder.name(topics.getRestaurantRejected()).partitions(partitions).replicas(replicas).build();
    }

    @Bean
    public NewTopic orderReady() {
        return TopicBuilder.name(topics.getRestaurantAccepted()).partitions(partitions).replicas(replicas).build();
    }
}
