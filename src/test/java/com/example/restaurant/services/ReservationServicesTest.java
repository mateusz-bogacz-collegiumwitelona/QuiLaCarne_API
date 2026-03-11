package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.domain.ReservationDishDoamin;
import com.example.restaurant.dto.domain.ReservationDomain;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.ReservationResponse;
import com.example.restaurant.dto.response.TableListResponse;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.repository.interfaces.IOrderRepository;
import com.example.restaurant.repository.interfaces.IReservationRepository;
import com.example.restaurant.repository.interfaces.ITableRespository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReservationServicesTest {

    @Mock
    private ITableRespository _tableRepo;
    @Mock
    private IReservationRepository _reservationRepo;
    @Mock
    private IOrderRepository _orderRepo;

    @InjectMocks
    private ReservationServices _reservationServices;

    @Test
    void create_ShouldFail_WhenDatesAreNull() {
        ReservationRequest request = new ReservationRequest();

        ResultHandler<ReservationResponse> result = _reservationServices.create(request, TestConstants.FAKE_TOKEN);

        assertFalse(result.isSuccess());
        assertEquals("Dates cannot be null", result.getMessage());
    }

    @Test
    void create_ShouldFail_WhenStartTimeInPast() {
        ReservationRequest request = new ReservationRequest();
        request.setStartTime(OffsetDateTime.now().minusHours(1));
        request.setEndTime(OffsetDateTime.now().plusHours(1));

        ResultHandler<ReservationResponse> result = _reservationServices.create(request, TestConstants.FAKE_TOKEN);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("least 30 minutes in advance"));
    }

    @Test
    void create_ShouldFail_WhenDurationTooLong() {
        ReservationRequest request = new ReservationRequest();
        request.setStartTime(OffsetDateTime.now().plusHours(1));
        request.setEndTime(OffsetDateTime.now().plusHours(5));

        ResultHandler<ReservationResponse> result = _reservationServices.create(request, TestConstants.FAKE_TOKEN);

        assertFalse(result.isSuccess());
        assertEquals("Reservation cannot exceed 3 hours", result.getMessage());
    }

    @Test
    void create_ShouldFail_WhenTableIsNotAvailable() {
        ReservationRequest request = createValidRequest();

        when(_tableRepo.isTableExist(TestConstants.FAKE_TABLE_TOKEN)).thenReturn(true);
        when(_tableRepo.findAllTables(eq("pl"), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of());

        ResultHandler<ReservationResponse> result = _reservationServices.create(request, TestConstants.FAKE_TOKEN);

        assertFalse(result.isSuccess());
        assertEquals(HttpStatus.CONFLICT.value(), result.getStatusCode());
        assertEquals("Table is already reserved in this timeframe", result.getMessage());
    }

    @Test
    void create_ShouldSucceed_AndMapDishes_WhenPreOrderIncluded() {
        ReservationRequest request = createValidRequest();

        ReservationDishRequest dishReq = new ReservationDishRequest();
        dishReq.setDishToken("dish-1");
        dishReq.setQuantity(2);
        request.setDishes(List.of(dishReq));

        ReservationDishDoamin dishDomain = new ReservationDishDoamin("Burger", 40, 2);
        ReservationDomain domainResponse = new ReservationDomain(List.of(dishDomain), 80);

        when(_tableRepo.isTableExist(TestConstants.FAKE_TABLE_TOKEN)).thenReturn(true);
        when(_tableRepo.findAllTables(eq("pl"), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of(TableListResponse.builder().token(TestConstants.FAKE_TABLE_TOKEN).build()));

        when(_reservationRepo.createReservation(request, TestConstants.FAKE_TOKEN)).thenReturn(TestConstants.FAKE_RESERVATION_TOKEN);
        when(_orderRepo.createOrderForReservation(TestConstants.FAKE_RESERVATION_TOKEN, TestConstants.FAKE_TABLE_TOKEN, request.getDishes()))
                .thenReturn(domainResponse);

        ResultHandler<ReservationResponse> result = _reservationServices.create(request, TestConstants.FAKE_TOKEN);

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.CREATED.value(), result.getStatusCode());

        ReservationResponse data = result.getData();
        assertTrue(data.isActive());
        assertEquals(80, data.getTotalPrice());
        assertEquals(1, data.getDishes().size());
        assertEquals("Burger", data.getDishes().get(0).getDishName());
    }

    private ReservationRequest createValidRequest() {
        ReservationRequest request = new ReservationRequest();
        request.setTableToken(TestConstants.FAKE_TABLE_TOKEN);
        request.setStartTime(OffsetDateTime.now().plusHours(1));
        request.setEndTime(OffsetDateTime.now().plusHours(3));
        return request;
    }
}