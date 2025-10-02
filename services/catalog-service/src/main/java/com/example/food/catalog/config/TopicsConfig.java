package com.example.food.catalog.config;

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
    public NewTopic restaurantCreated() {
        return TopicBuilder.name(topics.getRestaurantCreated()).partitions(partitions).replicas(replicas).build();
    }

    @Bean
    public NewTopic menuItemCreated() {
        return TopicBuilder.name(topics.getMenuItemCreated()).partitions(partitions).replicas(replicas).build();
    }

    @Bean
    public NewTopic menuItemUpdated() {
        return TopicBuilder.name(topics.getMenuItemUpdated()).partitions(partitions).replicas(replicas).build();
    }

    @Bean
    public NewTopic menuItemDeleted() {
        return TopicBuilder.name(topics.getMenuItemDeleted()).partitions(partitions).replicas(replicas).build();
    }
}
