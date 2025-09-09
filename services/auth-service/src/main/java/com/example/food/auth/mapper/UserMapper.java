package com.example.food.auth.mapper;

import com.example.food.auth.dto.RegisterRequest;
import com.example.food.auth.model.RoleEntity;
import com.example.food.auth.model.UserEntity;
import fd.user.UserRegisteredV1;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {UUID.class, Instant.class}, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface UserMapper {

  @Mapping(target="id", expression="java(UUID.randomUUID())")
  @Mapping(target="passwordHash", ignore = true)
  @Mapping(target="enabled", constant="true")
  @Mapping(target="roles", source="roles")
  UserEntity toEntity(RegisterRequest req, Set<RoleEntity> roles);

  @Mapping(target="eventId", expression="java(UUID.randomUUID())")
  @Mapping(target="occurredAt", expression="java(Instant.now())")
  @Mapping(target="userId", expression="java(user.getId())")
  @Mapping(target="username", source="username")
  @Mapping(target="email", source="email")
  @Mapping(target="roles", expression="java(user.getRoles().stream().map(RoleEntity::getName).toList())")
  UserRegisteredV1 toUserRegistered(UserEntity user);
}
