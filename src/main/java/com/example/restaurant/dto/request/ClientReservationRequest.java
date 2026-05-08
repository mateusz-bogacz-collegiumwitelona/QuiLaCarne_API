package com.example.restaurant.dto.request;

import io.swagger.v3.oas.annotations.Parameter;
import java.time.OffsetDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class ClientReservationRequest {
  @Parameter(description = "From this date")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime fromDate;

  @Parameter(description = "To this date")
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime toDate;

  @Parameter(description = "If give status token, list show only reservation with this status")
  String statusToken;
}
