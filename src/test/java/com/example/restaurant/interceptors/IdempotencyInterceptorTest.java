package com.example.restaurant.interceptors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.restaurant.services.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;

@ExtendWith(MockitoExtension.class)
class IdempotencyInterceptorTest {

  @Mock private IdempotencyService idempotencyService;

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @InjectMocks private IdempotencyInterceptor interceptor;

  private static final String HEADER_NAME = "X-Request-ID";
  private static final String REQUEST_ID = "test-uuid-123";

  @Test
  @DisplayName("Pre handle: should pass when metod us GET")
  void preHandle_shouldPass_whenMethodIsGet() throws Exception {
    when(request.getMethod()).thenReturn(HttpMethod.GET.name());

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    verifyNoInteractions(idempotencyService);
  }

  @Test
  @DisplayName("Pre handle: should pass when header is missing")
  void preHandle_shouldPass_whenHeaderIsMissing() throws Exception {
    when(request.getMethod()).thenReturn(HttpMethod.POST.name());
    when(request.getHeader(HEADER_NAME)).thenReturn(null);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
    verifyNoInteractions(idempotencyService);
  }

  @Test
  @DisplayName("Pre handle: should block and return 200 when request is already processed")
  void preHandle_shouldBlockAndReturn200_whenRequestIsAlreadyProcessed() throws Exception {
    when(request.getMethod()).thenReturn(HttpMethod.POST.name());
    when(request.getHeader(HEADER_NAME)).thenReturn(REQUEST_ID);
    when(idempotencyService.isProcessed(REQUEST_ID)).thenReturn(true);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertFalse(result);
    verify(response).setStatus(HttpServletResponse.SC_OK);
  }

  @Test
  @DisplayName("Pre Handle: should pass when request is not processed yet")
  void preHandle_shouldPass_whenRequestIsNotProcessedYet() throws Exception {
    when(request.getMethod()).thenReturn(HttpMethod.POST.name());
    when(request.getHeader(HEADER_NAME)).thenReturn(REQUEST_ID);
    when(idempotencyService.isProcessed(REQUEST_ID)).thenReturn(false);

    boolean result = interceptor.preHandle(request, response, new Object());

    assertTrue(result);
  }

  @Test
  @DisplayName("After completion: should mark as processed when status is 2xx and no exception")
  void afterCompletion_shouldMarkAsProcessed_whenStatusIs2xxAndNoException() throws Exception {
    when(request.getMethod()).thenReturn(HttpMethod.POST.name());
    when(request.getHeader(HEADER_NAME)).thenReturn(REQUEST_ID);
    when(response.getStatus()).thenReturn(HttpServletResponse.SC_CREATED); // 201

    interceptor.afterCompletion(request, response, new Object(), null);

    verify(idempotencyService).markAsProcessed(REQUEST_ID);
  }

  @Test
  @DisplayName("After completion: should not mark as processed when exception occurred")
  void afterCompletion_shouldNotMarkAsProcessed_whenExceptionOccurred() throws Exception {
    when(request.getMethod()).thenReturn(HttpMethod.POST.name());
    when(request.getHeader(HEADER_NAME)).thenReturn(REQUEST_ID);
    when(response.getStatus()).thenReturn(HttpServletResponse.SC_OK);

    interceptor.afterCompletion(request, response, new Object(), new RuntimeException("Error"));

    verify(idempotencyService, never()).markAsProcessed(anyString());
  }

  @Test
  @DisplayName("After completion: should not mark as processed when status is 4xx")
  void afterCompletion_shouldNotMarkAsProcessed_whenStatusIs4xx() throws Exception {
    when(request.getMethod()).thenReturn(HttpMethod.POST.name());
    when(request.getHeader(HEADER_NAME)).thenReturn(REQUEST_ID);
    when(response.getStatus()).thenReturn(HttpServletResponse.SC_BAD_REQUEST);

    interceptor.afterCompletion(request, response, new Object(), null);

    verify(idempotencyService, never()).markAsProcessed(anyString());
  }
}
