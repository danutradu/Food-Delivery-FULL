package com.example.food.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.food.catalog", "com.example.food.common"})
public class CatalogServiceApplication {
  public static void main(String[] args) { SpringApplication.run(CatalogServiceApplication.class, args); }
}
