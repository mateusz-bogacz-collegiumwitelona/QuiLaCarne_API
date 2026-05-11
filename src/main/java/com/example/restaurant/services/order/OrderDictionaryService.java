package com.example.restaurant.services.order;

import com.example.restaurant.annotations.Auditable;
import com.example.restaurant.dto.request.AddEntityRequest;
import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.helpers.DictionaryHelper;
import com.example.restaurant.models.OrderItems;
import com.example.restaurant.models.Orders;
import com.example.restaurant.models.lookup.OrderItemsStatus;
import com.example.restaurant.models.lookup.OrderStatus;
import com.example.restaurant.repository.interfaces.IOrderRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderDictionaryService {
  private final IOrderRepository _orderRepo;
  private final OrderSyncPublisher _syncPublisher;

  @Cacheable(
      value = "orderStatuses",
      key = "T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()")
  public DictionaryResponse getDictionary() {
    String lang = LocaleContextHolder.getLocale().getLanguage();
    return new DictionaryResponse(DictionaryHelper.map(_orderRepo.findAllStatuses(), lang));
  }

  @Transactional
  @Auditable(action = "ADD_ORDER_STATUS")
  @CacheEvict(value = "orderStatuses", allEntries = true)
  public void addStatus(AddEntityRequest request) {
    OrderStatus status =
        DictionaryHelper.createEntity(
            OrderStatus::new,
            request,
            _orderRepo::isStatusNameTaken,
            "Order status already exists");
    _orderRepo.saveStatus(status);

    _syncPublisher.publishOrderStatusChange(status, true);
  }

  @Transactional
  @Auditable(action = "REMOVE_ORDER_STATUS")
  @CacheEvict(value = "orderStatuses", allEntries = true)
  public void removeStatus(String token) {
    DictionaryHelper.deleteEntity(
        token,
        _orderRepo::findStatusByToken,
        _orderRepo::saveStatus,
        statusToRemove -> {
          OrderStatus fallbackStatus = _orderRepo.findStatusByToken("OTHER");
          List<Orders> affectedOrders = _orderRepo.findOrdersByStatus(statusToRemove);

          for (Orders order : affectedOrders) {
            order.getStatuses().remove(statusToRemove);
            order.getStatuses().add(fallbackStatus);
            _orderRepo.save(order);
            _syncPublisher.publishOrderUpdated(order, null);
          }
        });

    _syncPublisher.publishOrderStatusDeleted(token);
  }

  @Cacheable(
      value = "orderItemStatuses",
      key = "T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()")
  public DictionaryResponse getItemStatusesDictionary() {
    String lang = LocaleContextHolder.getLocale().getLanguage();
    return new DictionaryResponse(DictionaryHelper.map(_orderRepo.findAllItemStatuses(), lang));
  }

  @Transactional
  @Auditable(action = "ADD_ORDER_ITEM_STATUS")
  @CacheEvict(value = "orderItemStatuses", allEntries = true)
  public void addItemStatus(AddEntityRequest request) {
    OrderItemsStatus status =
        DictionaryHelper.createEntity(
            OrderItemsStatus::new,
            request,
            _orderRepo::isItemStatusNameTaken,
            "Order item status already exists");
    _orderRepo.saveItemStatus(status);
    _syncPublisher.publishOrderItemStatusChange(status, true);
  }

  @Transactional
  @Auditable(action = "REMOVE_ORDER_ITEM_STATUS")
  @CacheEvict(value = "orderItemStatuses", allEntries = true)
  public void removeItemStatus(String token) {
    DictionaryHelper.deleteEntity(
        token,
        _orderRepo::findItemStatusByToken,
        _orderRepo::saveItemStatus,
        statusToRemove -> {
          OrderItemsStatus fallbackStatus = _orderRepo.findItemStatusByToken("OTHER");
          List<OrderItems> affectedItems = _orderRepo.findOrderItemsByStatus(statusToRemove);

          for (OrderItems item : affectedItems) {
            item.getStatuses().remove(statusToRemove);
            item.getStatuses().add(fallbackStatus);
            _orderRepo.saveItem(item);
          }
        });

    _syncPublisher.publishOrderItemStatusDeleted(token);
  }
}
