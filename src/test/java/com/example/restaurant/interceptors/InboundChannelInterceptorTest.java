package com.example.restaurant.interceptors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.restaurant.services.JwtServices;
import java.util.Collections;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class InboundChannelInterceptorTest {

  @Mock private JwtServices jwtServices;

  @Mock private UserDetailsService userDetailsService;

  @Mock private MessageChannel channel;

  @InjectMocks private InboundChannelInterceptor interceptor;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  @DisplayName("pre send: should set user when command is connected and token is valid")
  void preSend_shouldSetUser_whenCommandIsConnectAndTokenIsValid() {
    String token = "valid.jwt.token";
    String username = "testuser";

    UserDetails mockUserDetails = mock(UserDetails.class);
    when(mockUserDetails.getAuthorities()).thenReturn(Collections.emptyList());

    when(jwtServices.extractUsername(token)).thenReturn(username);
    when(userDetailsService.loadUserByUsername(username)).thenReturn(mockUserDetails);
    when(jwtServices.isTokenValid(token, mockUserDetails)).thenReturn(true);

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.setNativeHeader("Authorization", "Bearer " + token);
    Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    Message<?> resultMessage =
        Objects.requireNonNull(
            interceptor.preSend(message, channel), "ResultMessage must not be null");

    StompHeaderAccessor resultAccessor =
        Objects.requireNonNull(
            StompHeaderAccessor.getAccessor(resultMessage, StompHeaderAccessor.class),
            "ResultAccessor must not be null");

    assertNotNull(resultAccessor.getUser());
    assertInstanceOf(UsernamePasswordAuthenticationToken.class, resultAccessor.getUser());
  }

  @Test
  @DisplayName("pre send: should not set user when command is not connected")
  void preSend_shouldNotSetUser_whenCommandIsNotConnect() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
    accessor.setNativeHeader("Authorization", "Bearer some.token");
    Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    Message<?> resultMessage =
        Objects.requireNonNull(
            interceptor.preSend(message, channel), "ResultMessage must not be null");

    StompHeaderAccessor resultAccessor =
        Objects.requireNonNull(
            StompHeaderAccessor.getAccessor(resultMessage, StompHeaderAccessor.class),
            "ResultAccessor must not be null");

    assertNull(resultAccessor.getUser());
    verifyNoInteractions(jwtServices, userDetailsService);
  }

  @Test
  @DisplayName("pre send: should not set user when auth header is missing")
  void preSend_shouldNotSetUser_whenAuthorizationHeaderIsMissing() {
    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    Message<?> resultMessage =
        Objects.requireNonNull(
            interceptor.preSend(message, channel), "ResultMessage must not be null");

    StompHeaderAccessor resultAccessor =
        Objects.requireNonNull(
            StompHeaderAccessor.getAccessor(resultMessage, StompHeaderAccessor.class),
            "ResultAccessor must not be null");

    assertNull(resultAccessor.getUser());
    verifyNoInteractions(jwtServices, userDetailsService);
  }

  @Test
  @DisplayName("pre send: should not set user when token is invalid")
  void preSend_shouldNotSetUser_whenTokenIsInvalid() {
    String token = "invalid.jwt.token";
    String username = "testuser";

    UserDetails mockUserDetails = mock(UserDetails.class);

    when(jwtServices.extractUsername(token)).thenReturn(username);
    when(userDetailsService.loadUserByUsername(username)).thenReturn(mockUserDetails);
    when(jwtServices.isTokenValid(token, mockUserDetails)).thenReturn(false);

    StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
    accessor.setNativeHeader("Authorization", "Bearer " + token);
    Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

    Message<?> resultMessage =
        Objects.requireNonNull(
            interceptor.preSend(message, channel), "ResultMessage must not be null");

    StompHeaderAccessor resultAccessor =
        Objects.requireNonNull(
            StompHeaderAccessor.getAccessor(resultMessage, StompHeaderAccessor.class),
            "ResultAccessor must not be null");

    assertNull(resultAccessor.getUser());
  }
}
