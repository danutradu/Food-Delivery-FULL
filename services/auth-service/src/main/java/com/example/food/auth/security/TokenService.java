package com.example.food.auth.security;

import com.example.food.auth.config.JwtProperties;
import com.example.food.auth.model.RoleEntity;
import com.example.food.auth.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TokenService {

  private final JwtEncoder jwtEncoder;
  private final JwtProperties jwtProperties;

  public record IssuedToken(String token, long expiresAtEpochSeconds) {}

  public IssuedToken issue(UserEntity user) {
    var now = Instant.now();
    var exp = now.plusSeconds(jwtProperties.expirationSeconds());
    var roles = user.getRoles().stream().map(RoleEntity::getName).toList();

    var claims = JwtClaimsSet.builder()
        .issuer(jwtProperties.issuer())
        .subject(user.getId().toString())
        .audience(List.of(jwtProperties.audience()))
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
