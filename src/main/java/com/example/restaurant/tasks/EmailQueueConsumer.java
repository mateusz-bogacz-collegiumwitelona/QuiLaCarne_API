package com.example.restaurant.tasks;

import com.example.restaurant.dto.domain.EmailDomain;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailQueueConsumer {
    private final StringRedisTemplate _redisTemplate;
    private final ObjectMapper _mapper;
    private final JavaMailSender _sender;
    private final TemplateEngine _templateEngine;

    private static final String QUEUE_NAME = "email_queue";

    @Scheduled(fixedDelay = 100)
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public void processQueue() {
        String jsonJob = _redisTemplate.opsForList().rightPop(QUEUE_NAME, Duration.ofSeconds(5));

        if (jsonJob != null) {
            try {
                EmailDomain job = _mapper.readValue(jsonJob, EmailDomain.class);
                sendActualEmail(job);
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("Failed to process email from queue. JSON: {}", jsonJob, e);
                }
            }
        }
    }

    private void sendActualEmail(EmailDomain job) throws Exception {
        Context context = new Context();

        if (job.variables() != null) {
            context.setVariables(job.variables());
        }

        String html = _templateEngine.process(job.template(), context);

        MimeMessage message = _sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setTo(job.to());
        helper.setSubject(job.subject());
        helper.setText(html, true);

        _sender.send(message);
        if (log.isInfoEnabled()) {
            log.info("Email successfully sent to {} from queue.", job.to());
        }
    }
}
