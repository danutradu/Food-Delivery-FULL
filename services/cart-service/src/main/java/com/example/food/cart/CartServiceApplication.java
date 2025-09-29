package com.example.food.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.food.cart", "com.example.food.common"})
public class CartServiceApplication {
  public static void main(String[] args) { SpringApplication.run(CartServiceApplication.class, args); }
}
