package com.example.restaurant.repository;

import com.example.restaurant.TestConstants;
import com.example.restaurant.models.OrderItems;
import com.example.restaurant.models.Orders;
import com.example.restaurant.models.lookup.OrderItemsStatus;
import com.example.restaurant.models.lookup.OrderStatus;
import com.example.restaurant.repository.interfaces.jpa.IJpaOrderItemStatusRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaOrderItemsRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaOrderRepository;
import com.example.restaurant.repository.interfaces.jpa.IJpaOrederStatusRepositry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderRepositoryTest {

    @Mock
    private IJpaOrderRepository _jpaOrderRepo;

    @Mock
    private IJpaOrderItemsRepository _jpaOrderItemRepo;

    @Mock
    private IJpaOrederStatusRepositry _jpaOrderStatusRepo;

    @Mock
    private IJpaOrderItemStatusRepository _jpaOrderItemStatusRepo;

    @InjectMocks
    private OrderRepository _orderRepo;

    @Test
    @DisplayName("Save ordrer with items: should save")
    void saveOrderWithItems_ShouldSaveOrderAndItems() {
        Orders mockOrder = new Orders();
        OrderItems mockItem = new OrderItems();
        List<OrderItems> items = List.of(mockItem);

        _orderRepo.saveOrderWithItems(mockOrder, items);

        verify(_jpaOrderRepo, times(1)).saveAndFlush(mockOrder);
        verify(_jpaOrderItemRepo, times(1)).saveAllAndFlush(items);
    }

    @Test
    @DisplayName("Find status by token: should return status if exist")
    void findStatusByToken_ShouldReturnStatus_WhenFound() {
        OrderStatus mockStatus = new OrderStatus();
        when(_jpaOrderStatusRepo.findByToken("PENDING"))
                .thenReturn(Optional.of(mockStatus));

        OrderStatus result = _orderRepo.findStatusByToken("PENDING");

        assertNotNull(result);
        verify(_jpaOrderStatusRepo, times(1)).findByToken("PENDING");
    }

    @Test
    @DisplayName("Find status: throw exception when not found")
    void findStatusByToken_ShouldThrowException_WhenNotFound() {
        when(_jpaOrderStatusRepo.findByToken(TestConstants.TOKEN_NON_EXISTENT)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> _orderRepo.findStatusByToken(TestConstants.TOKEN_NON_EXISTENT));
    }

    @Test
    @DisplayName("Save all items: shoould save all items")
    void saveAllItems_ShouldSaveAllItems() {
        List<OrderItems> items = List.of(new OrderItems(), new OrderItems());
        _orderRepo.saveAllItems(items);
        verify(_jpaOrderItemRepo, times(1)).saveAll(items);
    }

    @Test
    @DisplayName("Find status by token: should throw exception if status not found")
    void findItemStatusByToken_ShouldThrowException_WhenNotFound() {
        when(_jpaOrderItemStatusRepo.findByToken(TestConstants.TOKEN_NON_EXISTENT)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> _orderRepo.findItemStatusByToken(TestConstants.TOKEN_NON_EXISTENT));
    }

    @Test
    @DisplayName("Find by reservation token: should call jpa")
    void findByReservationToken_ShouldCallJpa() {
        _orderRepo.findByReservationToken(TestConstants.FAKE_RESERVATION_TOKEN);
        verify(_jpaOrderRepo).findByReservation_Token(TestConstants.FAKE_RESERVATION_TOKEN);
    }

    @Test
    @DisplayName("findAllStatuses: Should return list of order statuses from JPA")
    void findAllStatuses_ShouldReturnListOfStatuses() {
        List<OrderStatus> expectedStatuses = List.of(new OrderStatus(), new OrderStatus());
        when(_jpaOrderStatusRepo.findAll()).thenReturn(expectedStatuses);

        List<OrderStatus> result = _orderRepo.findAllStatuses();

        assertEquals(expectedStatuses, result);
        verify(_jpaOrderStatusRepo, times(1)).findAll();
    }

    @Test
    @DisplayName("findAllItemStatuses: Should return list of order item statuses from JPA")
    void findAllItemStatuses_ShouldReturnListOfItemStatuses() {
        List<OrderItemsStatus> expectedStatuses = List.of(new OrderItemsStatus(), new OrderItemsStatus());
        when(_jpaOrderItemStatusRepo.findAll()).thenReturn(expectedStatuses);

        List<OrderItemsStatus> result = _orderRepo.findAllItemStatuses();

        assertEquals(expectedStatuses, result);
        verify(_jpaOrderItemStatusRepo, times(1)).findAll();
    }

    @Test
    @DisplayName("isStatusNameTaken: Should return true and short-circuit when PL name exists")
    void isStatusNameTaken_ShouldReturnTrue_WhenPlNameExists() {
        when(_jpaOrderStatusRepo.findByNamePl(anyString())).thenReturn(Optional.of(new OrderStatus()));

        boolean result = _orderRepo.isStatusNameTaken("Status PL", "Status EN");

        assertTrue(result);
        verify(_jpaOrderStatusRepo, times(1)).findByNamePl("Status PL");
        verify(_jpaOrderStatusRepo, never()).findByNameEn(anyString());
    }

    @Test
    @DisplayName("isStatusNameTaken: Should return true when EN name exists")
    void isStatusNameTaken_ShouldReturnTrue_WhenEnNameExists() {
        when(_jpaOrderStatusRepo.findByNamePl(anyString())).thenReturn(Optional.empty());
        when(_jpaOrderStatusRepo.findByNameEn(anyString())).thenReturn(Optional.of(new OrderStatus()));

        boolean result = _orderRepo.isStatusNameTaken("Status PL", "Status EN");

        assertTrue(result);
        verify(_jpaOrderStatusRepo, times(1)).findByNamePl("Status PL");
        verify(_jpaOrderStatusRepo, times(1)).findByNameEn("Status EN");
    }

    @Test
    @DisplayName("isStatusNameTaken: Should return false when both names are available")
    void isStatusNameTaken_ShouldReturnFalse_WhenBothAreAvailable() {
        when(_jpaOrderStatusRepo.findByNamePl(anyString())).thenReturn(Optional.empty());
        when(_jpaOrderStatusRepo.findByNameEn(anyString())).thenReturn(Optional.empty());

        boolean result = _orderRepo.isStatusNameTaken("Status PL", "Status EN");

        assertFalse(result);
    }

    @Test
    @DisplayName("saveStatus: Should call JPA save")
    void saveStatus_ShouldCallJpaSave() {
        OrderStatus status = new OrderStatus();
        _orderRepo.saveStatus(status);
        verify(_jpaOrderStatusRepo, times(1)).save(status);
    }

    @Test
    @DisplayName("isItemStatusNameTaken: Should return true and short-circuit when PL name exists")
    void isItemStatusNameTaken_ShouldReturnTrue_WhenPlNameExists() {
        when(_jpaOrderItemStatusRepo.findByNamePl(anyString())).thenReturn(Optional.of(new OrderItemsStatus()));

        boolean result = _orderRepo.isItemStatusNameTaken("Item Status PL", "Item Status EN");

        assertTrue(result);
        verify(_jpaOrderItemStatusRepo, times(1)).findByNamePl("Item Status PL");
        verify(_jpaOrderItemStatusRepo, never()).findByNameEn(anyString());
    }

    @Test
    @DisplayName("isItemStatusNameTaken: Should return true when EN name exists")
    void isItemStatusNameTaken_ShouldReturnTrue_WhenEnNameExists() {
        when(_jpaOrderItemStatusRepo.findByNamePl(anyString())).thenReturn(Optional.empty());
        when(_jpaOrderItemStatusRepo.findByNameEn(anyString())).thenReturn(Optional.of(new OrderItemsStatus()));

        boolean result = _orderRepo.isItemStatusNameTaken("Item Status PL", "Item Status EN");

        assertTrue(result);
        verify(_jpaOrderItemStatusRepo, times(1)).findByNamePl("Item Status PL");
        verify(_jpaOrderItemStatusRepo, times(1)).findByNameEn("Item Status EN");
    }

    @Test
    @DisplayName("isItemStatusNameTaken: Should return false when both names are available")
    void isItemStatusNameTaken_ShouldReturnFalse_WhenBothAreAvailable() {
        when(_jpaOrderItemStatusRepo.findByNamePl(anyString())).thenReturn(Optional.empty());
        when(_jpaOrderItemStatusRepo.findByNameEn(anyString())).thenReturn(Optional.empty());

        boolean result = _orderRepo.isItemStatusNameTaken("Item Status PL", "Item Status EN");

        assertFalse(result);
    }

    @Test
    @DisplayName("saveItemStatus: Should call JPA save")
    void saveItemStatus_ShouldCallJpaSave() {
        OrderItemsStatus status = new OrderItemsStatus();
        _orderRepo.saveItemStatus(status);
        verify(_jpaOrderItemStatusRepo, times(1)).save(status);
    }
}
