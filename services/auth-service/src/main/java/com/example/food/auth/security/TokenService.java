package com.example.food.auth.security;

import com.example.food.auth.domain.RoleEntity;
import com.example.food.auth.domain.UserEntity;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
public class TokenService {
  @Value("${security.jwt.secret}")
  private String rawSecret;

  private SecretKey key() {
    byte[] secret = Base64.getDecoder().decode(rawSecret);
    return Keys.hmacShaKeyFor(secret);
  }

  public record IssuedToken(String token, long expiresAtEpochSeconds) {}

  public IssuedToken issue(UserEntity user) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(3600);
    List<String> roles = user.getRoles().stream().map(RoleEntity::getName).toList();

    String jwt = Jwts.builder()
        .setSubject(user.getId().toString())
        .setIssuer("auth-service")
        .setAudience("food-delivery")
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(exp))
        .claim("username", user.getUsername())
        .claim("email", user.getEmail())
        .claim("roles", roles)
        .signWith(key(), SignatureAlgorithm.HS256)
        .compact();

    return new IssuedToken(jwt, exp.getEpochSecond());
  }
}
