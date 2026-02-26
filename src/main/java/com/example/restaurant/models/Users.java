package com.example.restaurant.models;

import com.example.restaurant.models.base.BaseEntity;
import com.example.restaurant.models.named.Roles;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
public class Users extends BaseEntity {
    private String username;
    private String email;
    private String password;
    private Boolean twoFactorEnabled;
    private String mfaSecret;
    private Boolean isActive;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "x_user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Roles> roles = new HashSet<>();
}
