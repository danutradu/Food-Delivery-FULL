package com.example.food.ops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.food.ops", "com.example.food.common"})
public class RestaurantOpsApplication {
  public static void main(String[] args) { SpringApplication.run(RestaurantOpsApplication.class, args); }
}
