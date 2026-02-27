package com.example.restaurant.models.lookup;

import com.example.restaurant.models.base.BaseNamedEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "ban_status")
@Getter
@Setter
public class BanStatus extends BaseNamedEntity {}
