package com.example.restaurant.services;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.domain.OrderSummaryDomain;
import com.example.restaurant.dto.domain.ReservationDishDoamin;
import com.example.restaurant.dto.domain.ReservationDomain;
import com.example.restaurant.dto.domain.TodayOrderSummaryDomain;
import com.example.restaurant.dto.request.ClientReservationRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.*;
import com.example.restaurant.exceptions.ReservationNotFoundException;
import com.example.restaurant.helpers.ResultHandler;
import com.example.restaurant.mappers.ReservationMapper;
import com.example.restaurant.models.Reservations;
import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.ReservationStatus;
import com.example.restaurant.repository.interfaces.IOrderRepository;
import com.example.restaurant.repository.interfaces.IReservationRepository;
import com.example.restaurant.repository.interfaces.ITableRespository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.interfaces.IOrderServices;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

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

    @Mock
    private IUserRepository _userRepo;

    @Mock
    private IOrderServices _orderServices;

    @Mock
    private ReservationMapper _reservationMapper;

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
        request.setDishes(List.of(new ReservationDishRequest()));

        Users mockUser = new Users();
        RestaurantTables mockTable = new RestaurantTables();
        ReservationStatus mockStatus = new ReservationStatus();

        when(_tableRepo.isTableExist(anyString())).thenReturn(true);
        when(_tableRepo.findAllTables(eq("pl"), any(), any()))
                .thenReturn(List.of(TableListResponse.builder().token(TestConstants.FAKE_TABLE_TOKEN).build()));
        when(_userRepo.findByToken(anyString())).thenReturn(mockUser);
        when(_tableRepo.findByToken(anyString())).thenReturn(mockTable);
        when(_reservationRepo.findStatusByToken("ACTIVE")).thenReturn(mockStatus);

        ReservationDishDoamin dishDomain = new ReservationDishDoamin("Burger", 40, 2);
        when(_orderServices.createOrderForReservation(any(), any(), any()))
                .thenReturn(new ReservationDomain(List.of(dishDomain), 80));

        ResultHandler<ReservationResponse> result = _reservationServices.create(request, TestConstants.FAKE_USER_TOKEN);

        assertTrue(result.isSuccess());
        verify(_reservationRepo, times(1)).save(any(Reservations.class));
        assertEquals(80, result.getData().getTotalPrice());
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
        PaggedRequest pagged = new PaggedRequest();
        pagged.setPage(1);
        pagged.setSize(10);

        Reservations mockEntity = new Reservations();
        Page<Reservations> entityPage = new PageImpl<>(List.of(mockEntity));

        when(_reservationRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(entityPage);
        when(_reservationMapper.toClientReservationResponse(any(), eq("pl"))).thenReturn(new ClientReservationResponse());

        var result = _reservationServices.history(new ClientReservationRequest(), pagged, TestConstants.FAKE_USER_TOKEN);

        assertTrue(result.isSuccess());
        verify(_reservationMapper).toClientReservationResponse(any(), eq("pl"));

        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void history_ShouldThrowException_WhenRepositoryThrowsException() {
        PaggedRequest pagged = new PaggedRequest();
        pagged.setPage(1);
        pagged.setSize(10);

        when(_reservationRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenThrow(new RuntimeException("Database error"));

        assertThrows(RuntimeException.class, () ->
                _reservationServices.history(new ClientReservationRequest(), pagged, TestConstants.FAKE_USER_TOKEN)
        );
    }


    @Test
    void details_ShouldReturnSuccess_WithDishesAndPrice() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("pl"));

        Reservations mockEntity = new Reservations();

        ReservationDetailsResponse mockDetails = new ReservationDetailsResponse();
        mockDetails.setStatus("Aktywna");

        ReservationDishResponse dishRes = new ReservationDishResponse();
        dishRes.setDishName("Burger");
        dishRes.setPrice(80);
        dishRes.setQuantity(2);
        OrderSummaryDomain mockSummary = new OrderSummaryDomain(160, List.of(dishRes));

        when(_reservationRepo.findByTokenAndUserToken(
                TestConstants.FAKE_RESERVATION_TOKEN,
                TestConstants.FAKE_USER_TOKEN
        )).thenReturn(Optional.of(mockEntity));

        when(_reservationMapper.toReservationDetailsResponse(mockEntity, "pl")).thenReturn(mockDetails);
        when(_orderServices.getOrderSummaryForReservation(TestConstants.FAKE_RESERVATION_TOKEN)).thenReturn(mockSummary);

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
    void details_ShouldThrowException_WhenReservationDoesNotExist() {
        when(_reservationRepo.findByTokenAndUserToken(
                eq(TestConstants.FAKE_RESERVATION_TOKEN),
                eq(TestConstants.FAKE_USER_TOKEN)
        )).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class, () ->
                _reservationServices.details(
                        TestConstants.FAKE_RESERVATION_TOKEN,
                        TestConstants.FAKE_USER_TOKEN
                )
        );

        verify(_orderServices, never()).getOrderSummaryForReservation(anyString());
    }

    @Test
    void cancel_ShouldReturnSuccess_WhenReservationCancelled() {
        Reservations mockReservation = new Reservations();
        ReservationStatus cancelledStatus = new ReservationStatus();
        cancelledStatus.setToken("CANCELLED");

        when(_reservationRepo.findByTokenAndUserToken(
                TestConstants.FAKE_RESERVATION_TOKEN,
                TestConstants.FAKE_USER_TOKEN
        )).thenReturn(Optional.of(mockReservation));

        when(_reservationRepo.findStatusByToken("CANCELLED")).thenReturn(cancelledStatus);

        ResultHandler<Void> result = _reservationServices.cancel(
                TestConstants.FAKE_RESERVATION_TOKEN,
                TestConstants.FAKE_USER_TOKEN
        );

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertEquals("Reservation cancelled successfully", result.getMessage());

        verify(_reservationRepo, times(1)).save(mockReservation);
    }

    @Test
    void cancel_ShouldThrowException_WhenReservationNotFound() {
        when(_reservationRepo.findByTokenAndUserToken(
                TestConstants.FAKE_RESERVATION_TOKEN,
                TestConstants.FAKE_USER_TOKEN
        )).thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class, () ->
                _reservationServices.cancel(
                        TestConstants.FAKE_RESERVATION_TOKEN,
                        TestConstants.FAKE_USER_TOKEN
                )
        );

        verify(_reservationRepo, never()).save(any());
    }

    @Test
    void today_ShouldReturnSuccess_AndFetchOrderDetails() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("pl"));

        PaggedRequest pagged = new PaggedRequest();
        pagged.setPage(1);
        pagged.setSize(10);

        Reservations mockEntity = new Reservations();

        TodayReservationsResponse resDto = TodayReservationsResponse.builder()
                .token(TestConstants.FAKE_RESERVATION_TOKEN)
                .build();

        Page<Reservations> entityPage = new PageImpl<>(List.of(mockEntity));

        when(_reservationRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(entityPage);
        when(_reservationMapper.toTodayReservationsResponse(mockEntity, "pl")).thenReturn(resDto);

        TodayReservationDishResponse dishDto = new TodayReservationDishResponse();
        dishDto.setDishName("Pizza");
        dishDto.setPrice(40);
        dishDto.setNote("Bez cebuli");
        dishDto.setAllergens(List.of("Gluten"));

        TodayOrderSummaryDomain orderSummary = new TodayOrderSummaryDomain(40, List.of(dishDto));

        when(_orderServices.todayOrderDetails(TestConstants.FAKE_RESERVATION_TOKEN, "pl")).thenReturn(orderSummary);

        var result = _reservationServices.today(pagged);

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertEquals(1, result.getData().getItems().size());

        var fetchedRes = result.getData().getItems().get(0);
        assertEquals(40, fetchedRes.getTotalPrice());
        assertEquals(1, fetchedRes.getDishes().size());

        var fetchedDish = fetchedRes.getDishes().get(0);
        assertEquals("Pizza", fetchedDish.getDishName());
        assertEquals("Bez cebuli", fetchedDish.getNote());
        assertEquals("Gluten", fetchedDish.getAllergens().get(0));

        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void removeItemFromReservation_ShouldReturnSuccess_WhenItemRemoved() {
        ReservationDishRequest request = new ReservationDishRequest();
        request.setDishToken(TestConstants.FAKE_DISH_TOKEN);
        request.setQuantity(1);

        var result = _reservationServices.removeItemFromReservation(
                TestConstants.FAKE_USER_TOKEN,
                TestConstants.FAKE_RESERVATION_TOKEN,
                request
        );

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertEquals("Order item removed successfully", result.getMessage());

        verify(_orderServices, times(1)).removeItemFromReservation(
                TestConstants.FAKE_USER_TOKEN,
                TestConstants.FAKE_RESERVATION_TOKEN,
                request
        );
    }

    @Test
    void addItemFromReservation_ShouldReturnSuccess_WhenItemAdded() {
        ReservationDishRequest request1 = new ReservationDishRequest();
        request1.setDishToken(TestConstants.FAKE_DISH_TOKEN);
        request1.setQuantity(1);
        List<ReservationDishRequest> request = List.of(request1);

        var result = _reservationServices.addItemFromReservation(
                TestConstants.FAKE_USER_TOKEN,
                TestConstants.FAKE_RESERVATION_TOKEN,
                request
        );

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertEquals("Order items add successfully", result.getMessage());

        verify(_orderServices, times(1)).addItemFromReservation(
                TestConstants.FAKE_USER_TOKEN,
                TestConstants.FAKE_RESERVATION_TOKEN,
                request
        );
    }

    @Test
    void assignWaiter_ShouldReturnSuccess_WhenUserHasProperRole() {
        Reservations mockReservation = new Reservations();
        ReservationStatus inProgressStatus = new ReservationStatus();
        inProgressStatus.setToken("IN_PROGRESS");

        when(_userRepo.isInRole(anyString(), eq(TestConstants.FAKE_USER_TOKEN))).thenReturn(true);

        when(_reservationRepo.findByToken(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.of(mockReservation));
        when(_reservationRepo.findStatusByToken("IN_PROGRESS")).thenReturn(inProgressStatus);

        var result = _reservationServices.assignWaiter(
                TestConstants.FAKE_RESERVATION_TOKEN,
                TestConstants.FAKE_USER_TOKEN
        );

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());

        verify(_reservationRepo, times(1)).save(mockReservation);
        verify(_orderServices, times(1)).assignWaiterToOrders(
                TestConstants.FAKE_RESERVATION_TOKEN,
                TestConstants.FAKE_USER_TOKEN
        );
    }

    @Test
    void assignWaiter_ShouldReturnFailure_WhenUserLacksRole() {
        when(_userRepo.isInRole(anyString(), eq(TestConstants.FAKE_USER_TOKEN))).thenReturn(false);

        var result = _reservationServices.assignWaiter(
                TestConstants.FAKE_RESERVATION_TOKEN,
                TestConstants.FAKE_USER_TOKEN
        );

        assertFalse(result.isSuccess());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), result.getStatusCode());

        verify(_orderServices, never()).assignWaiterToOrders(anyString(), anyString());
    }

    @Test
    void assignWaiter_ShouldThrowException_WhenReservationNotFound() {
        when(_userRepo.isInRole(anyString(), eq(TestConstants.FAKE_USER_TOKEN))).thenReturn(true);

        when(_reservationRepo.findByToken(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class, () ->
                _reservationServices.assignWaiter(TestConstants.FAKE_RESERVATION_TOKEN, TestConstants.FAKE_USER_TOKEN)
        );

        verify(_orderServices, never()).assignWaiterToOrders(anyString(), anyString());
    }

    @Test
    void assignWaiter_ShouldThrowException_WhenOrderAssignmentFails() {
        Reservations mockReservation = new Reservations();
        ReservationStatus inProgressStatus = new ReservationStatus();

        when(_userRepo.isInRole(anyString(), eq(TestConstants.FAKE_USER_TOKEN))).thenReturn(true);
        when(_reservationRepo.findByToken(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.of(mockReservation));
        when(_reservationRepo.findStatusByToken("IN_PROGRESS")).thenReturn(inProgressStatus);

        doThrow(new RuntimeException("Order not found or you are not the assigned waiter"))
                .when(_orderServices).assignWaiterToOrders(
                        TestConstants.FAKE_RESERVATION_TOKEN,
                        TestConstants.FAKE_USER_TOKEN
                );

        assertThrows(RuntimeException.class, () ->
                _reservationServices.assignWaiter(
                        TestConstants.FAKE_RESERVATION_TOKEN,
                        TestConstants.FAKE_USER_TOKEN
                )
        );
    }

    @Test
    void isAbsent_ShouldReturnSuccess_WhenBothServicesSucceed() {
        Reservations mockRes = new Reservations();
        ReservationStatus activeStatus = new ReservationStatus();
        activeStatus.setToken("ACTIVE");
        mockRes.setReservationStatus(Set.of(activeStatus));

        when(_reservationRepo.findByToken(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.of(mockRes));
        when(_reservationRepo.findStatusByToken("NO_SHOW")).thenReturn(new ReservationStatus());

        ResultHandler<Void> result = _reservationServices.isAbsent(TestConstants.FAKE_RESERVATION_TOKEN);

        assertTrue(result.isSuccess());
        assertEquals(HttpStatus.OK.value(), result.getStatusCode());
        assertEquals("Absent success", result.getMessage());

        verify(_reservationRepo).save(mockRes);
        verify(_orderServices).isAbsent(TestConstants.FAKE_RESERVATION_TOKEN);
    }

    @Test
    void isAbsent_ShouldThrowException_WhenReservationNotFound() {
        when(_reservationRepo.findByToken(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.empty());

        assertThrows(ReservationNotFoundException.class, () ->
                _reservationServices.isAbsent(TestConstants.FAKE_RESERVATION_TOKEN)
        );

        verify(_orderServices, never()).isAbsent(anyString());
    }

    @Test
    void isAbsent_ShouldThrowException_WhenReservationIsNotActive() {
        Reservations mockRes = new Reservations();
        ReservationStatus cancelledStatus = new ReservationStatus();
        cancelledStatus.setToken("CANCELLED");
        mockRes.setReservationStatus(Set.of(cancelledStatus));

        when(_reservationRepo.findByToken(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.of(mockRes));

        assertThrows(IllegalStateException.class, () ->
                _reservationServices.isAbsent(TestConstants.FAKE_RESERVATION_TOKEN)
        );

        verify(_orderServices, never()).isAbsent(anyString());
    }

    @Test
    void isAbsent_ShouldThrowException_WhenOrderServiceFails() {
        Reservations mockRes = new Reservations();
        ReservationStatus activeStatus = new ReservationStatus();
        activeStatus.setToken("ACTIVE");
        mockRes.setReservationStatus(Set.of(activeStatus));

        when(_reservationRepo.findByToken(TestConstants.FAKE_RESERVATION_TOKEN))
                .thenReturn(Optional.of(mockRes));
        when(_reservationRepo.findStatusByToken("NO_SHOW")).thenReturn(new ReservationStatus());

        doThrow(new RuntimeException("Order status not found"))
                .when(_orderServices).isAbsent(TestConstants.FAKE_RESERVATION_TOKEN);

        assertThrows(RuntimeException.class, () ->
                _reservationServices.isAbsent(TestConstants.FAKE_RESERVATION_TOKEN)
        );

        verify(_reservationRepo, times(1)).save(mockRes);
        verify(_orderServices, times(1)).isAbsent(TestConstants.FAKE_RESERVATION_TOKEN);
    }
}