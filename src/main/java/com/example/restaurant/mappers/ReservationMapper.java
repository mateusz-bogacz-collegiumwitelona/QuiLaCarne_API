package com.example.restaurant.mappers;

import com.example.restaurant.dto.response.ClientReservationResponse;
import com.example.restaurant.dto.response.ReservationDetailsResponse;
import com.example.restaurant.dto.response.TodayReservationsResponse;
import com.example.restaurant.models.Reservations;
import com.example.restaurant.models.lookup.ReservationStatus;
import com.example.restaurant.models.lookup.TableStatus;
import java.util.Set;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ReservationMapper {
  @Mapping(target = "status", source = "reservationStatus", qualifiedByName = "mapStatus")
  ClientReservationResponse toClientReservationResponse(
      Reservations reservations, @Context String lang);

  @Named("mapStatus")
  default String mapStatus(Set<ReservationStatus> statuses, @Context String lang) {
    if (statuses == null || statuses.isEmpty()) return "UNKNOWN";
    return statuses.iterator().next().translate(lang);
  }

  @Mapping(target = "status", source = "reservationStatus", qualifiedByName = "mapStatus")
  @Mapping(target = "dishes", ignore = true)
  @Mapping(target = "totalPrice", ignore = true)
  ReservationDetailsResponse toReservationDetailsResponse(
      Reservations reservations, @Context String lang);

  @Mapping(target = "username", source = "user.username")
  @Mapping(target = "tableNumber", source = "tableId.tableNumber")
  @Mapping(
      target = "tableStatus",
      source = "tableId.tableStatus",
      qualifiedByName = "mapTableStatus")
  @Mapping(target = "status", source = "reservationStatus", qualifiedByName = "mapStatus")
  @Mapping(target = "dishes", ignore = true)
  @Mapping(target = "totalPrice", ignore = true)
  @SuppressWarnings("unused")
  TodayReservationsResponse toTodayReservationsResponse(
      Reservations reservations, @Context String lang);

  @Named("mapTableStatus")
  default String mapTableStatus(Set<TableStatus> statuses, @Context String lang) {
    if (statuses == null || statuses.isEmpty()) return "UNKNOWN";
    return statuses.iterator().next().translate(lang);
  }
}
