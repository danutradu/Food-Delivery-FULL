package com.example.food.auth.security;

import com.example.food.auth.domain.RoleEntity;
import com.example.food.auth.domain.UserEntity;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TokenService {

  private final JwtEncoder jwtEncoder;

  public record IssuedToken(String token, long expiresAtEpochSeconds) {}

  public IssuedToken issue(UserEntity user) {
    var now = Instant.now();
    var exp = now.plusSeconds(3600);
    var roles = user.getRoles().stream().map(RoleEntity::getName).toList();

    var claims = JwtClaimsSet.builder()
        .issuer("auth-service")
        .subject(user.getId().toString())
        .audience(List.of("food-delivery"))
        .issuedAt(now)
        .expiresAt(exp)
        .claim("username", user.getUsername())
        .claim("email", user.getEmail())
        .claim("roles", roles)
        .build();

    var jwt = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    return new IssuedToken(jwt, exp.getEpochSecond());
  }
}
