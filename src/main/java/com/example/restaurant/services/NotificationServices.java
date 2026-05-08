package com.example.restaurant.services;

import com.example.restaurant.helpers.WebSocketEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationServices {
  private final SimpMessagingTemplate _template;

  public void sendToTopic(String topic, Object payload) {
    _template.convertAndSend("/topic" + topic, payload);
  }

  public void sendEventToTopic(String topic, WebSocketEvent<?> event) {
    String destination = topic.startsWith("/") ? "/topic" + topic : "/topic/" + topic;
    _template.convertAndSend(destination, event);
  }
}
