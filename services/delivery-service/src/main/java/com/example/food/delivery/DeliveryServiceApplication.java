package com.example.food.delivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.food.delivery", "com.example.food.common"})
public class DeliveryServiceApplication {
  public static void main(String[] args) { SpringApplication.run(DeliveryServiceApplication.class, args); }
}
