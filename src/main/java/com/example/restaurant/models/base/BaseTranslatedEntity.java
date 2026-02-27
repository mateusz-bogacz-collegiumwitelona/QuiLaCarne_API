package com.example.restaurant.models.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
@Getter
@Setter
public class BaseTranslatedEntity extends BaseEntity{
    @Column(nullable = false, unique = true)
    private String namePl;

    @Column(nullable = false, unique = true)
    private String nameEn;
}
