package com.example.restaurant.other;

import com.example.restaurant.dto.domain.EmailDomain;
import com.example.restaurant.tasks.EmailQueueConsumer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.Duration;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailQueueConsumerTest {
    @Mock
    private StringRedisTemplate _redisTemplate;

    @Mock
    private ListOperations<String, String> _listOperations;

    @Mock
    private ObjectMapper _mapper;


    @Mock
    private JavaMailSender _sender;

    @Mock
    private TemplateEngine _templateEngine;

    @Mock
    private MimeMessage _mimeMessage;

    @InjectMocks
    private EmailQueueConsumer _emailQueueConsumer;

    private static final String QUEUE_NAME = "email_queue";

    @BeforeEach
    void setUp() {
        lenient().when(_redisTemplate.opsForList()).thenReturn(_listOperations);
    }

    @Test
    @DisplayName("Email queue: should process email successfully")
    void shouldProcessEmailSuccessfully() throws Exception {
        String validJson = "{\"to\":\"test@test.com\",\"subject\":\"Test\",\"template\":\"test-template\"}";
        EmailDomain emailDomain = new EmailDomain(
                "test@test.com",
                "Test Subject",
                "test-template",
                Map.of("key", "value")
        );
        String generatedHtml = "<html>Test</html>";

        when(_listOperations.rightPop(eq(QUEUE_NAME), any(Duration.class))).thenReturn(validJson);
        when(_mapper.readValue(validJson, EmailDomain.class)).thenReturn(emailDomain);
        when(_templateEngine.process(eq(emailDomain.template()), any(org.thymeleaf.context.Context.class)))
                .thenReturn(generatedHtml);
        when(_sender.createMimeMessage()).thenReturn(_mimeMessage);
        _emailQueueConsumer.processQueue();

        verify(_sender, times(1)).send(_mimeMessage);
    }

    @Test
    @DisplayName("Email queue: should do nothing when queue is empty")
    void shouldDoNothingWhenQueueIsEmpty() {
        when(_listOperations.rightPop(eq(QUEUE_NAME), any(Duration.class))).thenReturn(null);

        _emailQueueConsumer.processQueue();

        verifyNoInteractions(_mapper);
        verifyNoInteractions(_templateEngine);
        verifyNoInteractions(_sender);

    }

    @Test
    @DisplayName("Email queue: should handle exception when json is invalid")
    void shouldHandleExceptionWhenJsonIsInvalid() throws JsonProcessingException {
        String invalidJson = "invalid-json-format";

        when(_listOperations.rightPop(eq(QUEUE_NAME), any(Duration.class))).thenReturn(invalidJson);
        when(_mapper.readValue(invalidJson, EmailDomain.class)).thenThrow(new RuntimeException("JSON Parse Error"));

        _emailQueueConsumer.processQueue();

        verifyNoInteractions(_templateEngine);
        verifyNoInteractions(_sender);
    }
}
