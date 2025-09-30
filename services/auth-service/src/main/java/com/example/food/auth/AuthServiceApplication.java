package com.example.food.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ConfigurationPropertiesScan
@ComponentScan(basePackages = {"com.example.food.auth", "com.example.food.common"})
@EnableJpaRepositories(basePackages = {"com.example.food.auth", "com.example.food.common.outbox"})
@EntityScan(basePackages = {"com.example.food.auth", "com.example.food.common.outbox"})
public class AuthServiceApplication {
  public static void main(String[] args) { SpringApplication.run(AuthServiceApplication.class, args); }
}
