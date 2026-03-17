package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.domain.OrderSummaryDomain;
import com.example.restaurant.dto.domain.ReservationDishDoamin;
import com.example.restaurant.dto.domain.ReservationDomain;
import com.example.restaurant.dto.request.ClientReservationRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.*;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.repository.interfaces.IOrderRepository;
import com.example.restaurant.repository.interfaces.IReservationRepository;
import com.example.restaurant.repository.interfaces.ITableRespository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
    void create_ShouldFail_WhenDurationTooLong() {
        ReservationRequest request = new ReservationRequest();
        request.setStartTime(OffsetDateTime.now().plusHours(1));
        request.setEndTime(OffsetDateTime.now().plusHours(5));

        ResultHandler<ReservationResponse> result = _reservationServices.create(request, TestConstants.FAKE_USER_TOKEN);

        assertFalse(result.isSuccess());
        assertEquals("Reservation cannot exceed 3 hours", result.getMessage());
    }

    @Test
    void create_ShouldFail_WhenDurationTooShort() {
        ReservationRequest request = new ReservationRequest();
        request.setStartTime(OffsetDateTime.now().plusHours(1));
        request.setEndTime(OffsetDateTime.now().plusHours(1).plusMinutes(15));

        ResultHandler<ReservationResponse> result = _reservationServices.create(request, TestConstants.FAKE_USER_TOKEN);

        assertFalse(result.isSuccess());
        assertEquals("Reservation must be at least 30 minutes long", result.getMessage());
    }

    @Test
    void create_ShouldFail_WhenTableIsNotAvailable() {
        ReservationRequest request = createValidRequest();

        when(_tableRepo.isTableExist(TestConstants.FAKE_TABLE_TOKEN)).thenReturn(true);
        when(_tableRepo.findAllTables(eq("pl"), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of());

        ResultHandler<ReservationResponse> result = _reservationServices.create(request, TestConstants.FAKE_USER_TOKEN);

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

        when(_reservationRepo.createReservation(request, TestConstants.FAKE_USER_TOKEN)).thenReturn(TestConstants.FAKE_RESERVATION_TOKEN);
        when(_orderRepo.createOrderForReservation(TestConstants.FAKE_RESERVATION_TOKEN, TestConstants.FAKE_TABLE_TOKEN, request.getDishes()))
                .thenReturn(domainResponse);

        ResultHandler<ReservationResponse> result = _reservationServices.create(request, TestConstants.FAKE_USER_TOKEN);

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

    @Test
    void history_ShouldReturnSuccess_WithPagedResult() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("pl"));

        ClientReservationRequest filter = new ClientReservationRequest();
        PaggedRequest pagged = new PaggedRequest();
        pagged.setPage(1);
        pagged.setSize(10);

        Page<ClientReservationResponse> emptyPage = new PageImpl<>(List.of());
        PagedResult<ClientReservationResponse> expectedResult = new PagedResult<>(emptyPage);

        when(_reservationRepo.history(TestConstants.FAKE_USER_TOKEN, "pl", filter, pagged))
                .thenReturn(expectedResult);

        ResultHandler<PagedResult<ClientReservationResponse>> result = _reservationServices
                .history(
                        filter,
                        pagged,
                        TestConstants.FAKE_USER_TOKEN
                );

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertEquals(expectedResult, result.getData());
        assertEquals("User reservations retrieved successfully", result.getMessage());

        verify(_reservationRepo, times(1)).history(TestConstants.FAKE_USER_TOKEN, "pl", filter, pagged);

        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void history_ShouldReturnFailure_WhenRepositoryThrowsException() {
        String userToken = TestConstants.FAKE_USER_TOKEN;
        ClientReservationRequest filter = new ClientReservationRequest();
        PaggedRequest pagged = new PaggedRequest();

        when(_reservationRepo.history(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () ->
                _reservationServices.history(filter, pagged, userToken)
        );
    }

    @Test
    void details_ShouldReturnSuccess_WithDishesAndPrice() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("pl"));

        ReservationDetailsResponse mockDetails = new ReservationDetailsResponse();
        mockDetails.setStatus("Aktywna");

        ReservationDishResponse dishRes = new ReservationDishResponse();
        dishRes.setDishName("Burger");
        dishRes.setPrice(80);
        dishRes.setQuantity(2);
        OrderSummaryDomain mockSummary = new OrderSummaryDomain(160, List.of(dishRes));

        when(_reservationRepo.details(
                TestConstants.FAKE_RESERVATION_TOKEN,
                TestConstants.FAKE_USER_TOKEN,
                "pl"
        )).thenReturn(mockDetails);

        when(_orderRepo.getOrderSummaryForReservation(TestConstants.FAKE_RESERVATION_TOKEN)).thenReturn(mockSummary);

        ResultHandler<ReservationDetailsResponse> result = _reservationServices.details(
                TestConstants.FAKE_RESERVATION_TOKEN,
                TestConstants.FAKE_USER_TOKEN
        );

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertEquals("Aktywna", result.getData().getStatus());
        assertEquals(160, result.getData().getTotalPrice());
        assertEquals(1, result.getData().getDishes().size());
        assertEquals("Burger", result.getData().getDishes().get(0).getDishName());

        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void details_ShouldReturnNotFound_WhenReservationDoesNotExist() {
        when(_reservationRepo.details(
                eq(TestConstants.FAKE_RESERVATION_TOKEN),
                eq(TestConstants.FAKE_USER_TOKEN),
                anyString()
        )).thenThrow(new RuntimeException("Reservation not found"));

        assertThrows(RuntimeException.class, () ->
                _reservationServices.details(
                        TestConstants.FAKE_RESERVATION_TOKEN,
                        TestConstants.FAKE_USER_TOKEN
                )
        );

        verify(_orderRepo, never()).getOrderSummaryForReservation(anyString());
    }
}