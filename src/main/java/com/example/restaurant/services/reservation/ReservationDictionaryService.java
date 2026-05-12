package com.example.restaurant.services.reservation;

import com.example.restaurant.dto.response.DictionaryResponse;
import com.example.restaurant.helpers.DictionaryHelper;
import com.example.restaurant.repository.interfaces.IReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationDictionaryService {
  private final IReservationRepository _reservationRepo;

  @Cacheable(
      value = "reservationStatuses",
      key = "T(org.springframework.context.i18n.LocaleContextHolder).getLocale().getLanguage()")
  public DictionaryResponse getDictionary() {
    String lang = LocaleContextHolder.getLocale().getLanguage();
    return new DictionaryResponse(DictionaryHelper.map(_reservationRepo.findAllStatuses(), lang));
  }
}
