package com.example.restaurant.models;

import com.example.restaurant.models.base.BaseNamedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "guest_report_status")
@Getter
@Setter
public class GuestReportStatus extends BaseNamedEntity {}
