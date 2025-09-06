package com.example.food.ops;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class RestaurantOpsApplication {
  public static void main(String[] args) { SpringApplication.run(RestaurantOpsApplication.class, args); }
}
