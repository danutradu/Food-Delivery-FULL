package com.example.food.auth.config;

import com.example.food.auth.model.RoleEntity;
import com.example.food.auth.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class DataSeeder {
  @Bean
  CommandLineRunner seedRoles(RoleRepository roles) {
    return args -> {
      for (String r : List.of("CUSTOMER","RESTAURANT_OWNER","COURIER","ADMIN")) {
        roles.findByName(r).orElseGet(() -> {
          RoleEntity e = new RoleEntity();
          e.setName(r);
          return roles.save(e);
        });
      }
    };
  }
}
