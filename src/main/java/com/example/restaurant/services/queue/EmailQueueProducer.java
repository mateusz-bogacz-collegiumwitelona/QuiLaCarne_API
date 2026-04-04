package com.example.restaurant.services.queue;

import com.example.restaurant.dto.domain.EmailDomain;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailQueueProducer {
    private final StringRedisTemplate _redisTemplate;
    private final ObjectMapper _mapper;

    private static final String QUEUE_NAME = "email_queue";

    public void enqueueEmail(EmailDomain job) {
        try {
            String json = _mapper.writeValueAsString(job);
            _redisTemplate.opsForList().leftPush(QUEUE_NAME, json);
            log.info("Email job to {} pushed to queue.", job.to());
        } catch (Exception ex) {
            log.error("Failed to enqueue email job", ex);
        }
    }
}
