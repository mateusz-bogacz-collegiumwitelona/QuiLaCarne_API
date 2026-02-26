package com.example.restaurant.models;

import com.example.restaurant.models.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "guest_reports")
@Getter
@Setter
public class GuestReports extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id")
    private Users guest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private Users reporter;

    private String reason;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "x_guest_report_status",
            joinColumns = @JoinColumn(name="guest_report_id"),
            inverseJoinColumns = @JoinColumn(name = "status_id")
    )
    private Set<GuestReportStatus> statuses =  new HashSet<>();

}
