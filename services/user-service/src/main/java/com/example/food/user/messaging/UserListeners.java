package com.example.food.user.messaging;

import com.example.food.user.domain.UserProfileEntity;
import com.example.food.user.mapping.UserProfileMapper;
import com.example.food.user.repo.UserProfileRepository;
import fd.user.UserRegisteredV1;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserListeners {
  private final UserProfileRepository profiles;
  private final UserProfileMapper mapper;

  @KafkaListener(id="user-registered", topics="fd.user.registered.v1", groupId = "user-service")
  public void onUserRegistered(UserRegisteredV1 evt) {
    UserProfileEntity p = mapper.toProfile(evt);
    profiles.save(p);
  }
}
