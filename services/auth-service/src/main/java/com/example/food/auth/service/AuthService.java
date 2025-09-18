package com.example.food.auth.service;

import com.example.food.auth.dto.JwtResponse;
import com.example.food.auth.dto.LoginRequest;
import com.example.food.auth.dto.RegisterRequest;
import com.example.food.auth.exception.AccountDisabledException;
import com.example.food.auth.exception.AuthenticationException;
import com.example.food.auth.exception.UserAlreadyExistsException;
import com.example.food.auth.mapper.UserMapper;
import com.example.food.auth.repository.RoleRepository;
import com.example.food.auth.repository.UserRepository;
import com.example.food.auth.security.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final TokenService tokens;
    private final KafkaTemplate<String, SpecificRecord> kafka;
    private final UserMapper mapper;
    private final PasswordEncoder encoder;

    @Transactional
    public JwtResponse register(RegisterRequest req) {
        log.info("UserRegistered username = {} email = {}", req.username(), req.email());

        if (users.findByUsername(req.username()).isPresent()) {
            throw new UserAlreadyExistsException("Username already exists");
        }
        if (users.findByEmail(req.email()).isPresent()) {
            throw new UserAlreadyExistsException("Email already exists");
        }

        var role = roles.findByName("CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role not found in database"));

        var user = mapper.toEntity(req, Set.of(role));
        user.setPasswordHash(encoder.encode(req.password()));
        users.save(user);

        var event = mapper.toUserRegistered(user);
        ProducerRecord<String, SpecificRecord> record = new ProducerRecord<>("fd.user.registered.v1", user.getId().toString(), event);
        record.headers().add("eventType", "fd.user.UserRegisteredV1".getBytes());
        record.headers().add("eventId", event.getEventId().toString().getBytes());

        kafka.send(record).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish user registered event userId={}", user.getId(), ex);
            } else {
                log.info("Published user registered event userId={}", user.getId());
            }
        });

        var issuedToken = tokens.issue(user);
        return new JwtResponse(issuedToken.token(), issuedToken.expiresAtEpochSeconds(), "Bearer");
    }

    public JwtResponse login(LoginRequest req) {
        log.info("UserLogin username={}", req.username());

        var user = users.findByUsername(req.username())
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

        if (!user.isEnabled()) {
            throw new AccountDisabledException("Account is disabled");
        }

        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid credentials");
        }

        var issuedToken = tokens.issue(user);
        return new JwtResponse(issuedToken.token(), issuedToken.expiresAtEpochSeconds(), "Bearer");
    }
}
