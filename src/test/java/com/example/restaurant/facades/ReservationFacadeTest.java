package com.example.restaurant.facades;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.example.restaurant.TestConstants;
import com.example.restaurant.dto.domain.OrderSummaryDomain;
import com.example.restaurant.dto.domain.ReservationDishDoamin;
import com.example.restaurant.dto.domain.ReservationDomain;
import com.example.restaurant.dto.request.ClientReservationRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.request.ReservationDishRequest;
import com.example.restaurant.dto.request.ReservationRequest;
import com.example.restaurant.dto.response.ClientReservationResponse;
import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.dto.response.ReservationDetailsResponse;
import com.example.restaurant.dto.response.ReservationResponse;
import com.example.restaurant.enums.WebSocketEventType;
import com.example.restaurant.exceptions.EntityNotFoundException;
import com.example.restaurant.fasade.ReservationFacade;
import com.example.restaurant.fasade.interfaces.IOrderFacade;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.mappers.ReservationMapper;
import com.example.restaurant.mappers.SyncMapper;
import com.example.restaurant.models.Reservations;
import com.example.restaurant.models.RestaurantTables;
import com.example.restaurant.models.Users;
import com.example.restaurant.models.lookup.ReservationStatus;
import com.example.restaurant.repository.interfaces.IReservationRepository;
import com.example.restaurant.repository.interfaces.ITableRespository;
import com.example.restaurant.repository.interfaces.IUserRepository;
import com.example.restaurant.services.NotificationServices;
import com.example.restaurant.services.interfaces.ITableServices;
import com.example.restaurant.services.reservation.ReservationCommandService;
import com.example.restaurant.services.reservation.ReservationDictionaryService;
import com.example.restaurant.services.reservation.ReservationQueryService;
import com.example.restaurant.services.reservation.ReservationSyncPublisher;
import com.example.restaurant.validators.reservation.ReservationCreateValidator;
import com.example.restaurant.validators.reservation.ReservationDurationValidator;
import com.example.restaurant.validators.reservation.TableAvailabilityValidator;
import com.example.restaurant.validators.reservation.TableExistenceValidator;
import java.time.OffsetDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationFasade Unit Tests")
class ReservationFacadeTest {
  @Mock private ITableRespository _tableRepo;

  @Mock private IReservationRepository _reservationRepo;

  @Mock private IUserRepository _userRepo;

  @Mock private IOrderFacade _orderServices;

  @Mock private ReservationMapper _reservationMapper;

  @Mock private NotificationServices _notification;

  @Mock private ITableServices _tableServices;

  @InjectMocks private ReservationFacade _reservationFacade;

  @Spy private SyncMapper _syncMapper = Mappers.getMapper(SyncMapper.class);

  @BeforeEach
  void setUp() {
    LocaleContextHolder.setLocale(Locale.ENGLISH);

    ReservationSyncPublisher syncPublisher =
        new ReservationSyncPublisher(_notification, _syncMapper);

    List<ReservationCreateValidator> validators =
        List.of(
            new ReservationDurationValidator(),
            new TableExistenceValidator(_tableRepo),
            new TableAvailabilityValidator(_tableRepo));

    ReservationCommandService commandService =
        new ReservationCommandService(
            _tableRepo,
            _reservationRepo,
            _userRepo,
            _orderServices,
            syncPublisher,
            _tableServices,
            validators);

    ReservationQueryService queryService =
        new ReservationQueryService(_reservationRepo, _orderServices, _reservationMapper);

    ReservationDictionaryService dictionaryService =
        new ReservationDictionaryService(_reservationRepo);

    this._reservationFacade =
        new ReservationFacade(commandService, dictionaryService, queryService);
  }

  @Test
  @DisplayName("Create: Failure - Should throw IllegalStateException when duration exceeds 3 hours")
  void create_ThrowsException_WhenDurationTooLong() {
    ReservationRequest request = new ReservationRequest();
    request.setStartTime(OffsetDateTime.now().plusHours(1));
    request.setEndTime(OffsetDateTime.now().plusHours(5));

    assertThrows(
        IllegalStateException.class,
        () -> _reservationFacade.create(request, TestConstants.FAKE_USER_TOKEN));
  }

  @Test
  @DisplayName("Create: Failure - Should throw EntityNotFoundException when table does not exist")
  void create_ThrowsException_WhenTableNotFound() {
    ReservationRequest request = createValidRequest();
    when(_tableRepo.isTableExist(anyString())).thenReturn(false);

    assertThrows(
        EntityNotFoundException.class, () -> _reservationFacade.create(request, "user-token"));
  }

  @Test
  @DisplayName("Create: Success - Should save reservation and create order with dishes")
  void create_SuccessfulWithPreOrder() {
    ReservationRequest request = createValidRequest();
    request.setDishes(List.of(new ReservationDishRequest()));

    when(_tableRepo.isTableExist(anyString())).thenReturn(true);
    when(_tableRepo.isTableAvailable(anyString(), any(), any())).thenReturn(true);
    when(_userRepo.findByToken(anyString())).thenReturn(new Users());
    when(_tableRepo.findByToken(anyString())).thenReturn(new RestaurantTables());
    when(_reservationRepo.findStatusByToken(anyString())).thenReturn(new ReservationStatus());

    doAnswer(
            invocation -> {
              Reservations r = invocation.getArgument(0);
              r.setToken("NEW_RESERVATION_TOKEN");
              return null;
            })
        .when(_reservationRepo)
        .save(any(Reservations.class));

    ReservationDishDoamin dishDomain = new ReservationDishDoamin("Burger", 40, 2);
    when(_orderServices.createOrderForReservation(any(), any(), any()))
        .thenReturn(new ReservationDomain(List.of(dishDomain), 80));

    ReservationResponse response =
        _reservationFacade.create(request, TestConstants.FAKE_USER_TOKEN);

    assertNotNull(response);
    assertTrue(response.isActive());
    assertEquals(80, response.getTotalPrice());
    verify(_reservationRepo, times(1)).save(any(Reservations.class));

    verify(_notification, times(1))
        .sendEventToTopic(
            eq("/reservations/updates"),
            argThat(
                event ->
                    event.getEventType() == WebSocketEventType.CREATED
                        && event.getEntityType().equals("RESERVATION")
                        && "NEW_RESERVATION_TOKEN".equals(event.getToken())
                        && event.getPayload() != null));
  }

  @Test
  @DisplayName("History: Success - Should return PagedResult with mapped responses")
  void history_ReturnsPagedResult() {
    PaggedRequest pagged = new PaggedRequest();
    Reservations mockEntity = new Reservations();
    Page<Reservations> entityPage = new PageImpl<>(List.of(mockEntity));

    when(_reservationRepo.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(entityPage);
    when(_reservationMapper.toClientReservationResponse(any(), eq("en")))
        .thenReturn(new ClientReservationResponse());

    PagedResult<ClientReservationResponse> result =
        _reservationFacade.history(new ClientReservationRequest(), pagged, "token");

    assertNotNull(result);
    assertEquals(1, result.getItems().size());
  }

  @Test
  @DisplayName("Details: Success - Should return detailed response with order summary")
  void details_ReturnsSuccessfulResponse() {
    Reservations mockEntity = new Reservations();
    mockEntity.setToken(TestConstants.FAKE_RESERVATION_TOKEN);

    ReservationDetailsResponse mockDetails = new ReservationDetailsResponse();
    mockDetails.setStatus("Active");
    OrderSummaryDomain mockOrderSummary = new OrderSummaryDomain(100, new ArrayList<>());

    when(_reservationRepo.findByTokenAndUserToken(anyString(), anyString()))
        .thenReturn(Optional.of(mockEntity));
    when(_reservationMapper.toReservationDetailsResponse(any(), anyString()))
        .thenReturn(mockDetails);
    when(_orderServices.getOrderSummaryForReservation(TestConstants.FAKE_RESERVATION_TOKEN))
        .thenReturn(mockOrderSummary);

    ReservationDetailsResponse result =
        _reservationFacade.details(
            TestConstants.FAKE_RESERVATION_TOKEN, TestConstants.FAKE_USER_TOKEN);

    assertNotNull(result);
    assertEquals("Active", result.getStatus());
    assertEquals(100, result.getTotalPrice());
    assertNotNull(result.getDishes());
  }

  @Test
  @DisplayName("Cancel: Success - Should change status to CANCELLED")
  void cancel_Successful() {
    Reservations mockReservation = new Reservations();
    mockReservation.setToken("RES_TOKEN_TO_CANCEL");

    ReservationStatus activeStatus = new ReservationStatus();
    activeStatus.setToken("ACTIVE");
    mockReservation.setReservationStatus(Set.of(activeStatus));

    when(_reservationRepo.findByTokenAndUserToken(anyString(), anyString()))
        .thenReturn(Optional.of(mockReservation));
    when(_reservationRepo.findStatusByToken("CANCELLED")).thenReturn(new ReservationStatus());

    assertDoesNotThrow(() -> _reservationFacade.cancel("res-token", "user-token"));
    verify(_reservationRepo).save(mockReservation);
    verify(_notification, times(1))
        .sendEventToTopic(
            eq("/reservations/updates"),
            argThat(
                event ->
                    event.getEventType() == WebSocketEventType.UPDATED
                        && event.getEntityType().equals("RESERVATION")
                        && "RES_TOKEN_TO_CANCEL".equals(event.getToken())
                        && event.getPayload() != null));
  }

  @Test
  @DisplayName(
      "Assign Waiter: Success - Should set status to IN_PROGRESS, delegate to OrderServices and update table status")
  void assignWaiter_Successful() {
    Reservations mockReservation = new Reservations();
    mockReservation.setToken("RES_TOKEN_ASSIGN_WAITER");

    RestaurantTables mockTable = new RestaurantTables();
    mockTable.setToken("TABLE_TOKEN");
    mockReservation.setTableId(mockTable);

    ReservationStatus activeStatus = new ReservationStatus();
    activeStatus.setToken("ACTIVE");
    mockReservation.setReservationStatus(Set.of(activeStatus));

    when(_userRepo.isInRole(anyString(), anyString())).thenReturn(true);
    when(_reservationRepo.findByToken(anyString())).thenReturn(Optional.of(mockReservation));
    when(_reservationRepo.findStatusByToken("IN_PROGRESS")).thenReturn(new ReservationStatus());

    assertDoesNotThrow(() -> _reservationFacade.assignWaiter("res-token", "waiter-token"));

    verify(_orderServices).assignWaiterToOrders("res-token", "waiter-token");

    verify(_tableServices).changeStatusToOccupied("TABLE_TOKEN");

    verify(_notification, times(1))
        .sendEventToTopic(
            eq("/reservations/updates"),
            argThat(
                event ->
                    event.getEventType() == WebSocketEventType.UPDATED
                        && event.getEntityType().equals("RESERVATION")
                        && "RES_TOKEN_ASSIGN_WAITER".equals(event.getToken())
                        && event.getPayload() != null));
  }

  @Test
  @DisplayName(
      "Assign Waiter: Failure - Should throw IllegalStateException when user is not a waiter")
  void assignWaiter_ThrowsException_WhenNotWaiter() {
    when(_userRepo.isInRole(anyString(), anyString())).thenReturn(false);

    assertThrows(
        IllegalStateException.class,
        () -> _reservationFacade.assignWaiter("res-token", "user-token"));
  }

  @Test
  @DisplayName("Is Absent: Success - Should set status to NO_SHOW when reservation is ACTIVE")
  void isAbsent_Successful() {
    Reservations mockRes = new Reservations();
    mockRes.setToken("RES_TOKEN_NO_SHOW");

    ReservationStatus activeStatus = new ReservationStatus();
    activeStatus.setToken("ACTIVE");
    mockRes.setReservationStatus(Set.of(activeStatus));

    when(_reservationRepo.findByToken(anyString())).thenReturn(Optional.of(mockRes));
    when(_reservationRepo.findStatusByToken("NO_SHOW")).thenReturn(new ReservationStatus());

    assertDoesNotThrow(() -> _reservationFacade.isAbsent("res-token"));
    verify(_orderServices).isAbsent("res-token");

    verify(_notification, times(1))
        .sendEventToTopic(
            eq("/reservations/updates"),
            argThat(
                event ->
                    event.getEventType() == WebSocketEventType.UPDATED
                        && event.getEntityType().equals("RESERVATION")
                        && "RES_TOKEN_NO_SHOW".equals(event.getToken())
                        && event.getPayload() != null));
  }

  @Test
  @DisplayName(
      "Is Absent: Failure - Should throw IllegalStateException when reservation is not ACTIVE")
  void isAbsent_ThrowsException_WhenNotActive() {
    Reservations mockRes = new Reservations();
    ReservationStatus cancelledStatus = new ReservationStatus();
    cancelledStatus.setToken("CANCELLED");
    mockRes.setReservationStatus(Set.of(cancelledStatus));

    when(_reservationRepo.findByToken(anyString())).thenReturn(Optional.of(mockRes));

    assertThrows(IllegalStateException.class, () -> _reservationFacade.isAbsent("res-token"));
  }

  @Test
  @DisplayName("getDictionary: Returns empty list when repository returns empty")
  void getDictionary_ShouldReturnEmptyList_WhenRepoReturnsEmpty() {
    when(_reservationRepo.findAllStatuses()).thenReturn(new java.util.ArrayList<>());
    DictionaryResponse result = _reservationFacade.getDictionary();
    assertTrue(result.getItem().isEmpty());
  }

  @Test
  @DisplayName("getDictionary: Returns Polish names when language is pl")
  void getDictionary_ShouldReturnPolishNames_WhenLanguageIsPl() {
    LocaleContextHolder.setLocale(Locale.of(TestConstants.LANG_PL));

    ReservationStatus status = new ReservationStatus();
    status.setToken(TestConstants.STATUS_ACTIVE);
    status.setNamePl("Aktywna PL");
    status.setNameEn("Active EN");

    when(_reservationRepo.findAllStatuses()).thenReturn(List.of(status));

    DictionaryResponse result = _reservationFacade.getDictionary();

    assertEquals(1, result.getItem().size());
    assertEquals(TestConstants.STATUS_ACTIVE, result.getItem().getFirst().getToken());
    assertEquals("Aktywna PL", result.getItem().getFirst().getName());
  }

  @Test
  @DisplayName("getDictionary: Returns English names when language is not pl")
  void getDictionary_ShouldReturnEnglishNames_WhenLanguageIsNotPl() {
    Locale.of(TestConstants.LANG_EN);
    ReservationStatus status = new ReservationStatus();
    status.setToken(TestConstants.STATUS_CANCELLED);
    status.setNamePl("Anulowana PL");
    status.setNameEn("Cancelled EN");

    when(_reservationRepo.findAllStatuses()).thenReturn(List.of(status));

    DictionaryResponse result = _reservationFacade.getDictionary();

    assertEquals(1, result.getItem().size());
    assertEquals(TestConstants.STATUS_CANCELLED, result.getItem().getFirst().getToken());
    assertEquals("Cancelled EN", result.getItem().getFirst().getName());
  }

  @Test
  @DisplayName("Mark As Complete: Success - Should set status to COMPLETED and table to CLEANING")
  void markAsComplete_Successful() {
    Reservations mockReservation = new Reservations();
    mockReservation.setToken("RES_TOKEN_COMPLETE");

    RestaurantTables mockTable = new RestaurantTables();
    mockTable.setToken("TABLE_TOKEN");
    mockReservation.setTableId(mockTable);

    ReservationStatus inProgressStatus = new ReservationStatus();
    inProgressStatus.setToken("IN_PROGRESS");
    mockReservation.setReservationStatus(Set.of(inProgressStatus));

    when(_reservationRepo.findByToken(anyString())).thenReturn(Optional.of(mockReservation));
    when(_reservationRepo.findStatusByToken("COMPLETED")).thenReturn(new ReservationStatus());

    assertDoesNotThrow(() -> _reservationFacade.markAsComplete("RES_TOKEN_COMPLETE"));

    verify(_reservationRepo).save(mockReservation);
    verify(_tableServices).changeStatusToClean("TABLE_TOKEN");

    verify(_notification, times(1))
        .sendEventToTopic(
            eq("/reservations/updates"),
            argThat(
                event ->
                    event.getEventType() == WebSocketEventType.UPDATED
                        && event.getEntityType().equals("RESERVATION")
                        && "RES_TOKEN_COMPLETE".equals(event.getToken())
                        && event.getPayload() != null));
  }

  @Test
  @DisplayName(
      "Mark As Complete: Failure - Should throw IllegalStateException when reservation is not IN_PROGRESS")
  void markAsComplete_ThrowsException_WhenNotInProgress() {
    Reservations mockReservation = new Reservations();
    ReservationStatus activeStatus = new ReservationStatus();
    activeStatus.setToken("ACTIVE");
    mockReservation.setReservationStatus(Set.of(activeStatus));

    when(_reservationRepo.findByToken(anyString())).thenReturn(Optional.of(mockReservation));

    assertThrows(
        IllegalStateException.class, () -> _reservationFacade.markAsComplete("RES_TOKEN_COMPLETE"));
  }

  private ReservationRequest createValidRequest() {
    ReservationRequest request = new ReservationRequest();
    request.setTableToken("TABLE_1");
    request.setStartTime(OffsetDateTime.now().plusHours(1));
    request.setEndTime(OffsetDateTime.now().plusHours(2));
    request.setDishes(new ArrayList<>());
    return request;
  }
}
