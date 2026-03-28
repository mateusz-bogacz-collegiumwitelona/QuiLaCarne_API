package com.example.restaurant.repository;

import com.example.restaurant.TestConstants;
import com.example.restaurant.models.Reservations;
import com.example.restaurant.models.lookup.ReservationStatus;
import com.example.restaurant.repository.interfaces.jpa.IJpaReservationStatusRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaReservationsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationRepositoryTest {
    @Mock
    private IJpaReservationStatusRepository _jpaReservationStatusRepo;
    @Mock
    private IJpaReservationsRepository _jpaReservationsRepo;

    @InjectMocks
    private ReservationRepository _reservationRepo;

    @Test
    void save_ShouldCallJpaSave() {
        Reservations res = new Reservations();
        _reservationRepo.save(res);
        verify(_jpaReservationsRepo, times(1)).saveAndFlush(res);
    }

    @Test
    void findAll_ShouldCallJpaFindAll() {
        Page<Reservations> page = new PageImpl<>(List.of());
        when(_jpaReservationsRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        _reservationRepo.findAll(mock(Specification.class), mock(Pageable.class));

        verify(_jpaReservationsRepo).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void findStatusByToken_ShouldReturnStatus_WhenFound() {
        ReservationStatus status = new ReservationStatus();
        when(_jpaReservationStatusRepo.findByToken("ACTIVE")).thenReturn(Optional.of(status));

        ReservationStatus result = _reservationRepo.findStatusByToken("ACTIVE");

        assertNotNull(result);
        verify(_jpaReservationStatusRepo).findByToken("ACTIVE");
    }

    @Test
    void findByToken_ShouldReturnReservation_WhenFound() {
        Reservations mockReservation = new Reservations();
        when(_jpaReservationsRepo.findByToken(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.of(mockReservation));

        Optional<Reservations> result = _reservationRepo.findByToken(TestConstants.FAKE_RESERVATION_TOKEN);

        assertTrue(result.isPresent());
        verify(_jpaReservationsRepo).findByToken(TestConstants.FAKE_RESERVATION_TOKEN);
    }
}