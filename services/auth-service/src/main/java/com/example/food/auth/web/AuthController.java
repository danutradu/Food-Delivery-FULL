package com.example.food.auth.web;

import com.example.food.auth.domain.RoleEntity;
import com.example.food.auth.domain.UserEntity;
import com.example.food.auth.repo.RoleRepository;
import com.example.food.auth.repo.UserRepository;
import com.example.food.auth.security.TokenService;
import com.example.food.auth.web.dto.JwtResponse;
import com.example.food.auth.web.dto.LoginRequest;
import com.example.food.auth.web.dto.RegisterRequest;
import com.example.food.auth.web.mapper.UserMappers;
import fd.user.UserRegisteredV1;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final UserRepository users;
  private final RoleRepository roles;
  private final TokenService tokens;
  private final KafkaTemplate<String, Object> kafka;
  private final UserMappers mapper;
  private final PasswordEncoder encoder = new BCryptPasswordEncoder();

  @PostMapping("/register")
  public ResponseEntity<JwtResponse> register(@Valid @RequestBody RegisterRequest req) {
    if (users.findByUsername(req.username()).isPresent()) return ResponseEntity.badRequest().build();
    if (users.findByEmail(req.email()).isPresent()) return ResponseEntity.badRequest().build();

    RoleEntity role = roles.findByName("CUSTOMER").orElseThrow();
    UserEntity u = mapper.toEntity(req, Set.of(role));
    u.setPasswordHash(encoder.encode(req.password()));
    users.save(u);

    UserRegisteredV1 evt = mapper.toUserRegistered(u);
    ProducerRecord<String,Object> rec = new ProducerRecord<>("fd.user.registered.v1", u.getId().toString(), evt);
    rec.headers().add("eventType", "fd.user.UserRegisteredV1".getBytes());
    kafka.send(rec);

    var it = tokens.issue(u);
    return ResponseEntity.ok(new JwtResponse(it.token(), it.expiresAtEpochSeconds(), "Bearer"));
  }

  @PostMapping("/login")
  public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest req) {
    Optional<UserEntity> userOpt = users.findByUsername(req.username());
    if (userOpt.isEmpty()) return ResponseEntity.status(401).build();
    UserEntity u = userOpt.get();
    if (!u.isEnabled()) return ResponseEntity.status(403).build();
    if (!encoder.matches(req.password(), u.getPasswordHash())) return ResponseEntity.status(401).build();

    var it = tokens.issue(u);
    return ResponseEntity.ok(new JwtResponse(it.token(), it.expiresAtEpochSeconds(), "Bearer"));
  }
}
