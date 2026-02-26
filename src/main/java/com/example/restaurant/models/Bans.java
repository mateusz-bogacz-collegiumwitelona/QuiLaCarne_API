package com.example.restaurant.models;

import com.example.restaurant.models.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "bans")
@Getter
@Setter
public class Bans extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banned_by_id")
    private Users bannedBy;

    private String reason;
    private OffsetDateTime expiresAt;
    private Boolean isActive;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "x_ban_status",
            joinColumns = @JoinColumn(name = "ban_id"),
            inverseJoinColumns = @JoinColumn(name = "ban_status_id")
    )
    private Set<BanStatus> banStatuses = new HashSet<>();
}
