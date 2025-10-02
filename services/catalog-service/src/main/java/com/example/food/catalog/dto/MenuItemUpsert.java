package com.example.food.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MenuItemUpsert(@NotNull UUID restaurantId, UUID sectionId, @NotBlank String name, String description, int price, boolean available, int version) {}
