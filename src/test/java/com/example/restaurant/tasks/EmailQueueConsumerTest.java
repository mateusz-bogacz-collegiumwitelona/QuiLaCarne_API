package com.example.restaurant.tasks;

import com.example.restaurant.dto.domain.EmailDomain;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailQueueConsumerTest {

    @Mock
    private StringRedisTemplate _redisTemplate;

    @Mock
    private ObjectMapper _mapper;

    @Mock
    private JavaMailSender _sender;

    @Mock
    private TemplateEngine _templateEngine;

    @Mock
    private ListOperations<String, String> _listOperations;

    @InjectMocks
    private EmailQueueConsumer _emailQueueConsumer;

    @Test
    @DisplayName("processQueue: Should process email successfully when queue is not empty")
    void processQueue_ShouldSendEmail_WhenJobExists() throws Exception {
        String jsonJob = "{\"to\":\"test@example.com\",\"subject\":\"Test\",\"template\":\"test_template\"}";
        EmailDomain mockEmail = mock(EmailDomain.class);
        MimeMessage mockMimeMessage = mock(MimeMessage.class);

        when(mockEmail.to()).thenReturn("test@example.com");
        when(mockEmail.subject()).thenReturn("Test");
        when(mockEmail.template()).thenReturn("test_template");

        when(_redisTemplate.opsForList()).thenReturn(_listOperations);
        when(_listOperations.rightPop(eq("email_queue"), any(Duration.class))).thenReturn(jsonJob);
        when(_mapper.readValue(jsonJob, EmailDomain.class)).thenReturn(mockEmail);
        when(_templateEngine.process(eq("test_template"), any(Context.class))).thenReturn("<html>Test</html>");
        when(_sender.createMimeMessage()).thenReturn(mockMimeMessage);

        assertDoesNotThrow(() -> _emailQueueConsumer.processQueue());

        verify(_sender, times(1)).send(mockMimeMessage);
    }

    @Test
    @DisplayName("processQueue: Should do nothing when queue is empty")
    void processQueue_ShouldDoNothing_WhenQueueIsEmpty() {
        when(_redisTemplate.opsForList()).thenReturn(_listOperations);
        when(_listOperations.rightPop(eq("email_queue"), any(Duration.class))).thenReturn(null);

        assertDoesNotThrow(() -> _emailQueueConsumer.processQueue());

        verifyNoInteractions(_mapper, _sender, _templateEngine);
    }

    @Test
    @DisplayName("processQueue: Should catch exception and not crash when processing fails")
    void processQueue_ShouldCatchException_WhenProcessingFails() throws Exception {
        String invalidJson = "{invalid_json}";

        when(_redisTemplate.opsForList()).thenReturn(_listOperations);
        when(_listOperations.rightPop(eq("email_queue"), any(Duration.class))).thenReturn(invalidJson);
        when(_mapper.readValue(invalidJson, EmailDomain.class)).thenThrow(new RuntimeException("JSON Error"));

        assertDoesNotThrow(() -> _emailQueueConsumer.processQueue());

        verifyNoInteractions(_sender, _templateEngine);
    }
}