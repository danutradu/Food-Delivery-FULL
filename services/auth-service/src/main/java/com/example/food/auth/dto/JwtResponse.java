package com.example.food.auth.dto;

public record JwtResponse(String accessToken, long expiresAtEpochSeconds, String tokenType) {}
