package com.example.food.auth.web.dto;

public record JwtResponse(String accessToken, long expiresAtEpochSeconds, String tokenType) {}
