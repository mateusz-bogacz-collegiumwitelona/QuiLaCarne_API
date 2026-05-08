package com.example.restaurant.services;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.example.restaurant.services.queue.EmailQueueProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailServicesTest {

  @Mock private EmailQueueProducer _emailQueue;

  @InjectMocks private EmailServices _emailServices;

  @Test
  @DisplayName("sendActivationEmail: Should enqueue email when data is valid")
  void sendActivationEmail_ShouldEnqueue_WhenValid() {
    _emailServices.sendActivationEmail("test@example.com", "user1", "token123");

    verify(_emailQueue, times(1))
        .enqueueEmail(
            argThat(
                job ->
                    job.to().equals("test@example.com")
                        && job.template().equals("emails/activation")
                        && job.variables().get("username").equals("user1")));
  }

  @Test
  @DisplayName("Validation: Should throw exception for invalid email format")
  void validateInputs_ShouldThrow_WhenEmailInvalid() {
    assertThrows(
        IllegalArgumentException.class,
        () -> _emailServices.sendActivationEmail("invalid-email", "user", "token"));
    verifyNoInteractions(_emailQueue);
  }

  @Test
  @DisplayName("Validation: Should throw exception when required parameter is blank")
  void validateInputs_ShouldThrow_WhenParamIsBlank() {
    assertThrows(
        IllegalArgumentException.class,
        () -> _emailServices.sendActivationEmail("test@example.com", "", "token"));
  }
}
