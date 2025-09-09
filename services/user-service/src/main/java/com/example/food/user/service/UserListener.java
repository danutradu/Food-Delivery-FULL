package com.example.food.user.service;

import fd.user.UserRegisteredV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserListener {
    private final UserService userService;

    @KafkaListener(id = "user-registered", topics = "fd.user.registered.v1", groupId = "user-service")
    public void onUserRegistered(UserRegisteredV1 event) {
        log.info("KAFKA RECV topic=fd.user.registered.v1 userId={} username={}", event.getUserId(), event.getUsername());
        userService.createUserProfile(event);
    }
}
