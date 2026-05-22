package com.example.restaurant.services.report;

import com.example.restaurant.dto.sync.SyncReportResponse;
import com.example.restaurant.helpers.WebSocketEvent;
import com.example.restaurant.helpers.staics.WebSocketEntityType;
import com.example.restaurant.helpers.staics.WebSocketTopics;
import com.example.restaurant.mappers.SyncMapper;
import com.example.restaurant.models.GuestReports;
import com.example.restaurant.services.NotificationServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportSyncPublisher {
  private final NotificationServices _notification;
  private final SyncMapper _syncMapper;

  public void publishReportCreate(GuestReports report) {
    WebSocketEvent<SyncReportResponse> event =
        WebSocketEvent.created(
            WebSocketEntityType.REPORT_ENTITY_TYPE,
            report.getToken(),
            _syncMapper.toSyncReportResponse(report));
    _notification.sendEventToTopic(WebSocketTopics.REPORTS_TOPIC, event);
  }

  public void publishReportUpdate(GuestReports report) {
    WebSocketEvent<SyncReportResponse> event =
        WebSocketEvent.updated(
            WebSocketEntityType.REPORT_ENTITY_TYPE,
            report.getToken(),
            _syncMapper.toSyncReportResponse(report));
    _notification.sendEventToTopic(WebSocketTopics.REPORTS_TOPIC, event);
  }
}
