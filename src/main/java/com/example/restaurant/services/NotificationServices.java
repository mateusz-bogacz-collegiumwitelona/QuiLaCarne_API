package com.example.restaurant.services;

import com.example.restaurant.helpers.WebSocketEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServices {
  private final SimpMessagingTemplate _template;

  @Async
  public void sendEventToTopic(String topic, WebSocketEvent<?> event) {
    String destination = topic.startsWith("/") ? "/topic" + topic : "/topic/" + topic;
    _template.convertAndSend(destination, event);
  }
}
