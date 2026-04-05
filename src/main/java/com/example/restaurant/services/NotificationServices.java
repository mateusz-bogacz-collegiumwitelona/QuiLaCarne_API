package com.example.restaurant.services;

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
}
