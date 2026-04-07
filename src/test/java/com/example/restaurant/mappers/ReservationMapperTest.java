package com.example.restaurant.mappers;

import com.example.restaurant.dto.response.ClientReservationResponse;
import com.example.restaurant.dto.response.ReservationDetailsResponse;
import com.example.restaurant.dto.response.TodayReservationsResponse;
import com.example.restaurant.models.Reservations;
import com.example.restaurant.models.lookup.ReservationStatus;
import com.example.restaurant.models.lookup.TableStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ReservationMapperTest {

    private ReservationMapper _reservationMapper;

    @BeforeEach
    void setUp() {
        _reservationMapper = new ReservationMapper() {
            @Override
            public ClientReservationResponse toClientReservationResponse(Reservations reservations, String lang) {
                return null;
            }

            @Override
            public ReservationDetailsResponse toReservationDetailsResponse(Reservations reservations, String lang) {
                return null;
            }

            @Override
            public TodayReservationsResponse toTodayReservationsResponse(Reservations reservations, String lang) {
                return null;
            }
        };
    }

    @Test
    @DisplayName("mapStatus: Should return 'UNKNOWN' when statuses set is null")
    void mapStatus_ShouldReturnUnknown_WhenNull() {
        assertEquals("UNKNOWN", _reservationMapper.mapStatus(null, "pl"));
    }

    @Test
    @DisplayName("mapStatus: Should return 'UNKNOWN' when statuses set is empty")
    void mapStatus_ShouldReturnUnknown_WhenEmpty() {
        assertEquals("UNKNOWN", _reservationMapper.mapStatus(new HashSet<>(), "pl"));
    }

    @Test
    @DisplayName("mapStatus: Should return translated status")
    void mapStatus_ShouldReturnTranslatedStatus() {
        ReservationStatus status = new ReservationStatus();
        status.setNamePl("Aktywna");
        status.setNameEn("Active");

        assertEquals("Aktywna", _reservationMapper.mapStatus(Set.of(status), "pl"));
        assertEquals("Active", _reservationMapper.mapStatus(Set.of(status), "en"));
    }

    @Test
    @DisplayName("mapTableStatus: Should return 'UNKNOWN' when statuses set is null")
    void mapTableStatus_ShouldReturnUnknown_WhenNull() {
        assertEquals("UNKNOWN", _reservationMapper.mapTableStatus(null, "pl"));
    }

    @Test
    @DisplayName("mapTableStatus: Should return 'UNKNOWN' when statuses set is empty")
    void mapTableStatus_ShouldReturnUnknown_WhenEmpty() {
        assertEquals("UNKNOWN", _reservationMapper.mapTableStatus(new HashSet<>(), "pl"));
    }

    @Test
    @DisplayName("mapTableStatus: Should return translated table status")
    void mapTableStatus_ShouldReturnTranslatedStatus() {
        TableStatus status = new TableStatus();
        status.setNamePl("Zajęty");
        status.setNameEn("Occupied");

        assertEquals("Zajęty", _reservationMapper.mapTableStatus(Set.of(status), "pl"));
        assertEquals("Occupied", _reservationMapper.mapTableStatus(Set.of(status), "en"));
    }
}