package com.example.food.user.service;

import com.example.food.user.model.UserProfileEntity;
import com.example.food.user.mapping.UserProfileMapper;
import com.example.food.user.repository.UserProfileRepository;
import fd.user.UserRegisteredV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserListeners {
  private final UserProfileRepository profiles;
  private final UserProfileMapper mapper;

  @KafkaListener(id="user-registered", topics="fd.user.registered.v1", groupId = "user-service")
  public void onUserRegistered(UserRegisteredV1 evt) {
    log.info("KAFKA RECV topic=fd.user.registered.v1 userId={} username={}", evt.getUserId(), evt.getUsername());
    UserProfileEntity p = mapper.toProfile(evt);
    profiles.save(p);
  }
}
