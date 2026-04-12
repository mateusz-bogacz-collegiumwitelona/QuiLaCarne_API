package com.example.restaurant.mappers;

import com.example.restaurant.dto.response.SyncUserResponse;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.base.BaseEntity;
import com.example.restaurant.models.lookup.Roles;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface SyncMapper {
    @Mapping(target = "roleTokens", source = "roles", qualifiedByName = "mapRoleTokens")
    @Mapping(target = "isStaff", source = "roles", qualifiedByName = "checkIfStaff")
    SyncUserResponse toSyncUserResponse(Users user);

    @Named("mapRoleTokens")
    @SuppressWarnings("unused")
    default List<String> mapRoleTokens(Set<Roles> roles) {
        if (roles == null) return List.of();
        return roles.stream().map(BaseEntity::getToken).toList();
    }

    @Named("checkIfStaff")
    @SuppressWarnings("unused")
    default boolean checkIfStaff(Set<Roles> roles) {
        if (roles == null) return false;
        return roles.stream()
                .anyMatch(r -> r.getName().equals("ROLE_WAITER")
                        || r.getName().equals("ROLE_MANAGER")
                        || r.getName().equals("ROLE_ADMIN"));
    }
}
