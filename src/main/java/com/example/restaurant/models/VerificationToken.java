package com.example.restaurant.models;

import com.example.restaurant.enums.TokenTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "verification_tokens")
@Getter
@Setter
public class VerificationToken {
    @Id
    private UUID id = UUID.randomUUID();

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Enumerated(EnumType.STRING)
    private TokenTypeEnum type;

    private OffsetDateTime expiryDate;

    private OffsetDateTime createdAt;

    public boolean isExpired() {
        return expiryDate.isBefore(OffsetDateTime.now());
    }
}
