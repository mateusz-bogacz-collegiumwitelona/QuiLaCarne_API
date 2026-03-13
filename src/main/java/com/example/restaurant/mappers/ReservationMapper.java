package com.example.restaurant.mappers;

import com.example.restaurant.dto.response.ClientReservationResponse;
import com.example.restaurant.models.Reservations;
import com.example.restaurant.models.lookup.ReservationStatus;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface ReservationMapper {
    @Mapping(target = "status", source = "reservationStatus", qualifiedByName = "mapStatus")
    ClientReservationResponse toClientReservationResponse(Reservations reservations, @Context String lang);

    @Named("mapStatus")
    default String mapStatus(Set<ReservationStatus> statuses, @Context String lang) {
        if (statuses == null || statuses.isEmpty()) {
            return "UNKNOWN";
        }

        return statuses.iterator().next().translate(lang);
    }
}
