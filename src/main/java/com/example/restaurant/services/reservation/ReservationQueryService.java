package com.example.restaurant.services.reservation;

import com.example.restaurant.dto.domain.OrderSummaryDomain;
import com.example.restaurant.dto.request.ClientReservationRequest;
import com.example.restaurant.dto.request.PaggedRequest;
import com.example.restaurant.dto.response.ClientReservationResponse;
import com.example.restaurant.dto.response.ReservationDetailsResponse;
import com.example.restaurant.fasade.interfaces.IOrderFacade;
import com.example.restaurant.helpers.PagedResult;
import com.example.restaurant.mappers.ReservationMapper;
import com.example.restaurant.models.Reservations;
import com.example.restaurant.models.lookup.ReservationStatus;
import com.example.restaurant.repository.interfaces.IReservationRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationQueryService {
  private final IReservationRepository _reservationRepo;
  private final IOrderFacade _orderServices;
  private final ReservationMapper _reservationMapper;

  public PagedResult<ClientReservationResponse> history(
      ClientReservationRequest request, PaggedRequest pagged, String userToken) {
    String lang = LocaleContextHolder.getLocale().getLanguage();

    Pageable pageable =
        PageRequest.of(
            Math.max(0, pagged.getPage() - 1),
            pagged.getSize(),
            Sort.by(Sort.Direction.ASC, "startTime"));

    Specification<Reservations> spec =
        (root, query, criteriaBuilder) -> {
          List<Predicate> predicates = new ArrayList<>();

          predicates.add(criteriaBuilder.equal(root.get("user").get("token"), userToken));

          if (request.getFromDate() != null)
            predicates.add(
                criteriaBuilder.greaterThanOrEqualTo(root.get("startTime"), request.getFromDate()));

          if (request.getToDate() != null)
            predicates.add(
                criteriaBuilder.lessThanOrEqualTo(root.get("startTime"), request.getToDate()));

          if (request.getStatusToken() != null && !request.getStatusToken().isEmpty()) {
            Join<Reservations, ReservationStatus> statusJoin = root.join("reservationStatus");
            predicates.add(
                criteriaBuilder.equal(statusJoin.get("token"), request.getStatusToken()));
          }

          return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

    Page<Reservations> page = _reservationRepo.findAll(spec, pageable);

    return new PagedResult<>(
        page.map(r -> _reservationMapper.toClientReservationResponse(r, lang)));
  }

  public ReservationDetailsResponse details(String reservationToken, String userToken) {
    return _reservationRepo
        .findByTokenAndUserToken(reservationToken, userToken)
        .map(
            r -> {
              ReservationDetailsResponse response =
                  _reservationMapper.toReservationDetailsResponse(
                      r, LocaleContextHolder.getLocale().getLanguage());

              OrderSummaryDomain orderSummary =
                  _orderServices.getOrderSummaryForReservation(r.getToken());

              response.setDishes(orderSummary.dishes());
              response.setTotalPrice(orderSummary.totalPrice());

              return response;
            })
        .orElse(null);
  }
}
