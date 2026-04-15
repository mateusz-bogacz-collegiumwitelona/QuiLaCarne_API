package com.example.restaurant.services.queue;

import com.example.restaurant.dto.domain.EmailDomain;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({"PMD.TooManyMethods", "PMD.CouplingBetweenObjects", "PMD.GodClass"})
public class EmailQueueProducer {
    private final StringRedisTemplate _redisTemplate;
    private final ObjectMapper _mapper;

    private static final String QUEUE_NAME = "email_queue";

    public void enqueueEmail(EmailDomain job) {
        try {
            String json = _mapper.writeValueAsString(job);
            _redisTemplate.opsForList().leftPush(QUEUE_NAME, json);
            if (log.isInfoEnabled()) {
                log.info("Email job to {} pushed to queue.", job.to());
            }
        } catch (JsonProcessingException ex) {
            if (log.isErrorEnabled()) {
                log.error("Failed to enqueue email job", ex);
            }
        }
    }
}
