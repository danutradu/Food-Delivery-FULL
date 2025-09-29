package com.example.food.user.service;

import com.example.food.user.config.StandardRetryableTopic;
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

    @StandardRetryableTopic
    @KafkaListener(topics = "${kafka.topics.user-registered}", groupId = "${kafka.consumer.group-id}")
    public void onUserRegistered(UserRegisteredV1 event) {
        userService.createUserProfile(event);
    }
}
