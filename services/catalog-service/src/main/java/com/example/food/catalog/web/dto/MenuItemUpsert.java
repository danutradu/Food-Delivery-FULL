package com.example.food.catalog.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record MenuItemUpsert(@NotNull UUID restaurantId, UUID sectionId, @NotBlank String name, String description, int priceCents, boolean available, int version) {}
