package com.example.restaurant.dto.sync;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncDictionariesResponse {
  private List<SyncDictionaryResponse> allergens;
  private List<SyncDictionaryResponse> dishCategories;
  private List<SyncDictionaryResponse> banStatuses;
  private List<SyncDictionaryResponse> reportStatuses;
  private List<SyncDictionaryResponse> orderStatuses;
  private List<SyncDictionaryResponse> orderItemStatuses;
  private List<SyncDictionaryResponse> reservationStatuses;
  private List<SyncDictionaryResponse> tableStatuses;
}
