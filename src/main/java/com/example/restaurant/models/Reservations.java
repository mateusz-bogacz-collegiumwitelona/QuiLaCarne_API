package com.example.restaurant.models;


import com.example.restaurant.models.base.BaseEntity;
import com.example.restaurant.models.lookup.ReservationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "reservations")
@Getter
@Setter
public class Reservations extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private RestaurantTables tableId ;

    @Column(name = "start_time", updatable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", updatable = false)
    private OffsetDateTime endTime;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "x_reservations_status",
            joinColumns = @JoinColumn(name = "reservations_id "),
            inverseJoinColumns = @JoinColumn(name = "reservations_status_id")
    )
    private Set<ReservationStatus> reservationStatus = new HashSet<>();
}
