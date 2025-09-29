package com.example.food.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.food.order", "com.example.food.common"})
public class OrderServiceApplication {
  public static void main(String[] args) { SpringApplication.run(OrderServiceApplication.class, args); }
}
