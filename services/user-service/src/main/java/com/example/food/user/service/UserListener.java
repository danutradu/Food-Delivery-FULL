package com.example.food.user.service;

import fd.user.UserRegisteredV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserListener {
    private final UserService userService;

    @RetryableTopic(
            attempts = "${kafka.retry.attempts}",
            backoff = @Backoff(delayExpression = "${kafka.retry.delay}", multiplierExpression = "${kafka.retry.multiplier}", maxDelayExpression = "${kafka.retry.max-delay}"),
            dltStrategy = DltStrategy.FAIL_ON_ERROR,
            include = {Exception.class}
    )
    @KafkaListener(id = "user-registered", topics = "fd.user.registered.v1", groupId = "user-service")
    public void onUserRegistered(UserRegisteredV1 event) {
        userService.createUserProfile(event);
    }
}
