package com.example.restaurant.models.lookup;

import com.example.restaurant.models.base.BaseNamedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class Roles extends BaseNamedEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
}
