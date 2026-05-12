package com.example.restaurant.models.lookup;

import com.example.restaurant.models.base.BaseNamedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serial;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class Roles extends BaseNamedEntity implements Serializable {
  @Serial private static final long serialVersionUID = 1L;
}
