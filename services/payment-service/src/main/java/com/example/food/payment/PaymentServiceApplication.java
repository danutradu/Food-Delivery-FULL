package com.example.food.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.food.payment", "com.example.food.common"})
@EnableJpaRepositories(basePackages = {"com.example.food.payment", "com.example.food.common.outbox"})
@EntityScan(basePackages = {"com.example.food.payment", "com.example.food.common.outbox"})
public class PaymentServiceApplication {
  public static void main(String[] args) { SpringApplication.run(PaymentServiceApplication.class, args); }
}
