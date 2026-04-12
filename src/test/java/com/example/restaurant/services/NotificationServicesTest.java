package com.example.restaurant.services;

import com.example.restaurant.helpers.WebSocketEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServicesTest {

    @Mock
    private SimpMessagingTemplate _template;

    @InjectMocks
    private NotificationServices _notificationServices;

    @Test
    @DisplayName("sendToTopic: Should prepend /topic prefix")
    void sendToTopic_ShouldPrependPrefix() {
        String topic = "/updates";
        String payload = "test-data";
        _notificationServices.sendToTopic(topic, payload);
        verify(_template, times(1)).convertAndSend("/topic/updates", payload);
    }

    @Test
    @DisplayName("sendEventToTopic: Should handle topic names without leading slash")
    void sendEventToTopic_ShouldHandleNoLeadingSlash() {
        String topic = "dishes";
        WebSocketEvent<String> event = new WebSocketEvent<>();
        _notificationServices.sendEventToTopic(topic, event);
        verify(_template, times(1)).convertAndSend("/topic/dishes", event);
    }

    @Test
    @DisplayName("sendEventToTopic: Should handle topic names with leading slash")
    void sendEventToTopic_ShouldHandleLeadingSlash() {
        String topic = "/orders";
        WebSocketEvent<String> event = new WebSocketEvent<>();
        _notificationServices.sendEventToTopic(topic, event);
        verify(_template, times(1)).convertAndSend("/topic/orders", event);
    }
}