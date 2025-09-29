package com.example.food.auth.service;

import com.example.food.auth.config.KafkaTopics;
import com.example.food.auth.dto.JwtResponse;
import com.example.food.auth.dto.LoginRequest;
import com.example.food.auth.dto.RegisterRequest;
import com.example.food.auth.exception.AccountDisabledException;
import com.example.food.auth.exception.AuthenticationException;
import com.example.food.auth.exception.UserAlreadyExistsException;
import com.example.food.auth.repository.RoleRepository;
import com.example.food.auth.repository.UserRepository;
import com.example.food.auth.security.TokenService;
import com.example.food.auth.util.UserFactory;
import com.example.food.common.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TokenService tokenService;
    private final PasswordEncoder encoder;
    private final OutboxService outboxService;
    private final KafkaTopics topics;

    @Transactional
    public JwtResponse register(RegisterRequest req) {
        log.info("UserRegistered username = {} email = {}", req.username(), req.email());

        if (userRepository.findByUsername(req.username()).isPresent()) {
            throw new UserAlreadyExistsException(req.username());
        }
        if (userRepository.findByEmail(req.email()).isPresent()) {
            throw new UserAlreadyExistsException(req.email());
        }

        var role = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role not found in database"));

        var user = UserFactory.createUser(req, Set.of(role));
        user.setPasswordHash(encoder.encode(req.password()));
        userRepository.save(user);

        var event = UserFactory.createUserRegistered(user);
        outboxService.publish(topics.getUserRegistered(), user.getId().toString(), event);

        var issuedToken = tokenService.issue(user);
        log.info("User registered successfully userId={}", user.getId());
        return new JwtResponse(issuedToken.token(), issuedToken.expiresAtEpochSeconds(), "Bearer");
    }

    public JwtResponse login(LoginRequest req) {
        log.info("UserLogin username={}", req.username());

        var user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new AuthenticationException("Invalid credentials"));

        if (!user.isEnabled()) {
            throw new AccountDisabledException(user.getUsername());
        }

        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new AuthenticationException(user.getUsername());
        }

        var issuedToken = tokenService.issue(user);
        return new JwtResponse(issuedToken.token(), issuedToken.expiresAtEpochSeconds(), "Bearer");
    }
}
