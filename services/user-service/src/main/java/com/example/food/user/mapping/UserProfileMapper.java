package com.example.food.user.mapping;

import com.example.food.user.domain.UserProfileEntity;
import fd.user.UserRegisteredV1;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserProfileMapper {
  @Mapping(target="userId", source="userId")
  @Mapping(target="username", source="username")
  @Mapping(target="email", source="email")
  UserProfileEntity toProfile(UserRegisteredV1 evt);
}
