package com.example.food.user.service;

import com.example.food.user.exception.UserProfileNotFoundException;
import com.example.food.user.mapping.UserProfileMapper;
import com.example.food.user.model.UserProfileEntity;
import com.example.food.user.repository.UserProfileRepository;
import fd.user.UserRegisteredV1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserProfileRepository userProfiles;
    private final UserProfileMapper mapper;

    public void createUserProfile(UserRegisteredV1 event) {
        log.info("Creating user profile for userId={} username={}", event.getUserId(), event.getUsername());

        var profile = mapper.toProfile(event);
        userProfiles.save(profile);
    }

    public UserProfileEntity getUserProfile(UUID userId) {
        return userProfiles.findById(userId)
                .orElseThrow(() -> new UserProfileNotFoundException("User profile not found: " + userId));
    }
}
