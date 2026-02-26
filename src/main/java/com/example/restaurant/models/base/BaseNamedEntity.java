package com.example.restaurant.models.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseNamedEntity extends BaseEntity {
    @Column(nullable = false, unique = true)
    private String name;
}
