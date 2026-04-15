package com.example.restaurant.other;

import com.example.restaurant.dto.domain.EmailDomain;
import com.example.restaurant.services.queue.EmailQueueProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailQueueProducerTest {

    @Mock
    private StringRedisTemplate _redisTemplate;

    @Mock
    private ObjectMapper _mapper;

    @Mock
    private ListOperations<String, String> _listOperations;

    @InjectMocks
    private EmailQueueProducer _emailQueueProducer;

    @Test
    @DisplayName("enqueueEmail: Should serialize and push to Redis queue successfully")
    void enqueueEmail_ShouldPushToRedisQueue_WhenSuccessful() throws Exception {
        EmailDomain mockEmail = mock(EmailDomain.class);
        String expectedJson = "{\"to\":\"test@example.com\",\"subject\":\"Test\"}";

        when(_mapper.writeValueAsString(mockEmail)).thenReturn(expectedJson);
        when(_redisTemplate.opsForList()).thenReturn(_listOperations);

        _emailQueueProducer.enqueueEmail(mockEmail);

        verify(_listOperations, times(1)).leftPush("email_queue", expectedJson);
    }

    @Test
    @DisplayName("enqueueEmail: Should catch exception and not interact with Redis when serialization fails")
    void enqueueEmail_ShouldCatchException_WhenSerializationFails() throws Exception {
        EmailDomain mockEmail = mock(EmailDomain.class);

        when(_mapper.writeValueAsString(mockEmail)).thenThrow(mock(JsonProcessingException.class));

        assertDoesNotThrow(() -> _emailQueueProducer.enqueueEmail(mockEmail));

        verifyNoInteractions(_redisTemplate);
    }
}