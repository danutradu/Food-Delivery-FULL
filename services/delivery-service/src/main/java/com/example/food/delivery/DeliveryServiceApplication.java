package com.example.food.delivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.food.delivery", "com.example.food.common"})
@EnableJpaRepositories(basePackages = {"com.example.food.delivery", "com.example.food.common.outbox"})
@EntityScan(basePackages = {"com.example.food.delivery", "com.example.food.common.outbox"})
public class DeliveryServiceApplication {
  public static void main(String[] args) { SpringApplication.run(DeliveryServiceApplication.class, args); }
}
