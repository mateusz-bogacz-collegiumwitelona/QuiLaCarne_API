package com.example.restaurant.models.base;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@MappedSuperclass
@Getter
@Setter
@SQLRestriction("deleted_at IS NULL")
public abstract class BaseTranslatedEntity extends BaseEntity {
  @Column(nullable = false, unique = true)
  private String namePl;

  @Column(nullable = false, unique = true)
  private String nameEn;

  @Column(name = "deleted_at")
  private OffsetDateTime deletedAt;

  public String translate(String lang) {
    return "en".equalsIgnoreCase(lang) ? nameEn : namePl;
  }
}
