package com.example.food.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.food.user", "com.example.food.common"})
public class UserServiceApplication {
  public static void main(String[] args) { SpringApplication.run(UserServiceApplication.class, args); }
}
