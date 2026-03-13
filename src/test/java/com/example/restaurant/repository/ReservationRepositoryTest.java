package com.example.restaurant.repository;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.request.ClientReservationRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.ClientReservationResponse;
import com.example.restaurant.dto.response.ReservationDetailsResponse;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.mappers.ReservationMapper;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
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
    private IJpaReservationsRepository _jpaReservationsRepo;

    @Mock
    private ReservationMapper _reservationMapper;

    @InjectMocks
    private ReservationRepository _reservationRepo;

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    private final String PL = "pl";

    @Test
    void createReservation_ShouldReturnToken_WhenDataIsValid() {
        ReservationRequest request = new ReservationRequest();
        request.setTableToken(TestConstants.FAKE_TABLE_TOKEN);
        request.setStartTime(OffsetDateTime.now());
        request.setEndTime(OffsetDateTime.now().plusHours(2));

        Users user = new Users();
        RestaurantTables table = new RestaurantTables();
        ReservationStatus status = new ReservationStatus();

        when(_jpaUserRepo.findByToken(TestConstants.FAKE_USER_TOKEN)).thenReturn(Optional.of(user));
        when(_jpaTableRepo.findByToken(TestConstants.FAKE_TABLE_TOKEN)).thenReturn(table);
        when(_jpaReservationStatusRepo.findByToken("ACTIVE")).thenReturn(Optional.of(status));

        when(_jpaReservationsRepo.saveAndFlush(any(Reservations.class))).thenAnswer(i -> {
            Reservations res = i.getArgument(0);
            res.setToken(TestConstants.FAKE_USER_TOKEN);
            return res;
        });

        String token = _reservationRepo.createReservation(request, TestConstants.FAKE_USER_TOKEN);

        assertNotNull(token);
        verify(_jpaReservationsRepo, times(1)).saveAndFlush(any(Reservations.class));
    }

    @Test
    void createReservation_ShouldThrowException_WhenUserNotFound() {
        ReservationRequest request = new ReservationRequest();

        when(_jpaUserRepo.findByToken(anyString())).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                _reservationRepo.createReservation(request, TestConstants.FAKE_USER_TOKEN)
        );

        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void history_ShouldApplyPaginationAndMappingCorrectly() {

        ClientReservationRequest filter = new ClientReservationRequest();
        PaggedRequest pagged = new PaggedRequest();
        pagged.setPage(2);
        pagged.setSize(5);

        Reservations mockEntity = new Reservations();
        mockEntity.setToken(TestConstants.FAKE_RESERVATION_TOKEN);
        Page<Reservations> entityPage = new PageImpl<>(List.of(mockEntity));

        ClientReservationResponse mockDto = ClientReservationResponse.builder()
                .token(TestConstants.FAKE_RESERVATION_TOKEN)
                .build();

        when(_jpaReservationsRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(entityPage);

        when(_reservationMapper.toClientReservationResponse(mockEntity, PL))
                .thenReturn(mockDto);

        PagedResult<ClientReservationResponse> result = _reservationRepo.history(
                TestConstants.FAKE_USER_TOKEN,
                PL,
                filter,
                pagged
        );

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(TestConstants.FAKE_RESERVATION_TOKEN, result.getItems().get(0).getToken());

        verify(_jpaReservationsRepo).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable capturedPageable = pageableCaptor.getValue();

        assertEquals(1, capturedPageable.getPageNumber());
        assertEquals(5, capturedPageable.getPageSize());
    }

    @Test
    void details_ShouldReturnMappedResponse_WhenReservationExists() {
        Reservations mockEntity = new Reservations();
        ReservationDetailsResponse mockResponse = new ReservationDetailsResponse();
        mockResponse.setStatus("Aktywna");

        when(_jpaReservationsRepo.findByTokenAndUser_Token(
                TestConstants.FAKE_RESERVATION_TOKEN,
                TestConstants.FAKE_USER_TOKEN
        )).thenReturn(Optional.of(mockEntity));

        when(_reservationMapper.toReservationDetailsResponse(mockEntity, PL))
                .thenReturn(mockResponse);

        ReservationDetailsResponse result = _reservationRepo.details(
                TestConstants.FAKE_RESERVATION_TOKEN,
                TestConstants.FAKE_USER_TOKEN,
                PL
        );

        assertNotNull(result);
        assertEquals("Aktywna", result.getStatus());
    }

    @Test
    void details_ShouldThrowException_WhenReservationNotFoundOrNotOwned() {
        when(_jpaReservationsRepo.findByTokenAndUser_Token(
                TestConstants.FAKE_RESERVATION_TOKEN,
                TestConstants.FAKE_USER_TOKEN
        )).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> _reservationRepo.details(
                TestConstants.FAKE_RESERVATION_TOKEN,
                TestConstants.FAKE_USER_TOKEN,
                PL
        ));

        assertEquals("Reservation not found", exception.getMessage());
        verify(_reservationMapper, never()).toReservationDetailsResponse(any(), anyString());
    }
}
