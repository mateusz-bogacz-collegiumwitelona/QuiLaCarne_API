package com.example.restaurant.repository;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.models.Reservations;
import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.ReservationStatus;
import com.example.restaurant.repository.interfaces.jpa.IJpaReservationStatusRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaReservationsRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaTableRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationRepositoryTest {
    @Mock
    private IJpaUserRepository _jpaUserRepo;

    @Mock
    private IJpaTableRepository _jpaTableRepo;

    @Mock
    private IJpaReservationStatusRepository _jpaReservationStatusRepo;

    @Mock
    private IJpaReservationsRepository  _jpaReservationsRepo;

    @InjectMocks
    private ReservationRepository _reservationRepo;

    @Test
    void createReservation_ShouldReturnToken_WhenDataIsValid() {
        ReservationRequest request = new ReservationRequest();
        request.setTableToken(TestConstants.FAKE_TABLE_TOKEN);
        request.setStartTime(OffsetDateTime.now());
        request.setEndTime(OffsetDateTime.now().plusHours(2));

        Users  user = new Users();
        RestaurantTables table = new RestaurantTables();
        ReservationStatus status = new ReservationStatus();

        when(_jpaUserRepo.findByToken(TestConstants.FAKE_TOKEN)).thenReturn(Optional.of(user));
        when(_jpaTableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN)).thenReturn(table);
        when(_jpaReservationStatusRepo.findByToken("ACTIVE")).thenReturn(Optional.of(status));

        when(_jpaReservationsRepo.saveAndFlush(any(Reservations.class))).thenAnswer(i -> {
            Reservations res = i.getArgument(0);
            res.setToken("mocked-generated-token");
            return res;
        });

        String token = _reservationRepo.createReservation(request, TestConstants.FAKE_TOKEN);

        assertNotNull(token);
        verify(_jpaReservationsRepo, times(1)).saveAndFlush(any(Reservations.class));
    }

    @Test
    void createReservation_ShouldThrowException_WhenUserNotFound() {
        ReservationRequest request = new ReservationRequest();

        when(_jpaUserRepo.findByToken(anyString())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                _reservationRepo.createReservation(request, "invalid-user")
        );

        assertEquals("User not found", exception.getMessage());
    }
}
