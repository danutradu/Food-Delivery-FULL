package com.example.food.user.mapping;

import com.example.food.user.model.UserProfileEntity;
import fd.user.UserRegisteredV1;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserProfileMapper {
  @Mapping(target="userId", source="userId")
  @Mapping(target="username", source="username")
  @Mapping(target="email", source="email")
  UserProfileEntity toProfile(UserRegisteredV1 evt);
}
