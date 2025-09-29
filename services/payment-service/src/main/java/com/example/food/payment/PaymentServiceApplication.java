package com.example.food.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.example.food.payment", "com.example.food.common"})
public class PaymentServiceApplication {
  public static void main(String[] args) { SpringApplication.run(PaymentServiceApplication.class, args); }
}
