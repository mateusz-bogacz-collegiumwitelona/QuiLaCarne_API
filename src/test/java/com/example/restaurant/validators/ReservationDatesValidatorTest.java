package com.example.restaurant.validators;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationDatesValidatorTest {

    private ReservationDatesValidator validator;

    @Mock
    private ConstraintValidatorContext context;
    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder builder;

    @BeforeEach
    void setUp() {
        validator = new ReservationDatesValidator();
        lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        lenient().when(builder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    @DisplayName("isValid: Should return true if start or end date is null")
    void isValid_ShouldReturnTrue_WhenDatesAreNull() {
        assertTrue(validator.isValid(createRequest(null, OffsetDateTime.now()), context));
        assertTrue(validator.isValid(createRequest(OffsetDateTime.now(), null), context));
        assertTrue(validator.isValid(createRequest(null, null), context));
    }

    @Test
    @DisplayName("isValid: Should return true for valid dates (e.g. tomorrow)")
    void isValid_ShouldReturnTrue_WhenDatesAreValid() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(1);
        OffsetDateTime end = start.plusHours(2);

        ITimeFramedRequest request = createRequest(start, end);

        assertTrue(validator.isValid(request, context));
        verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    }

    @Test
    @DisplayName("isValid: Should return false when start time is after end time")
    void isValid_ShouldReturnFalse_WhenStartIsAfterEnd() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(2);
        OffsetDateTime end = start.minusHours(1);

        assertFalse(validator.isValid(createRequest(start, end), context));
        verify(context).buildConstraintViolationWithTemplate(
                "Start time must be before end time"
        );
    }

    @Test
    @DisplayName("isValid: Should return false when start time is less than 30 minutes from now")
    void isValid_ShouldReturnFalse_WhenStartTimeIsTooSoon() {
        OffsetDateTime start = OffsetDateTime.now().plusMinutes(15);
        OffsetDateTime end = start.plusHours(1);

        assertFalse(validator.isValid(createRequest(start, end), context));
        verify(context).buildConstraintViolationWithTemplate(
                "Reservations must be made at least 30 minutes in advance"
        );
    }

    @Test
    @DisplayName("isValid: Should return false when start time is more than 60 days in advance")
    void isValid_ShouldReturnFalse_WhenStartTimeIsTooFar() {
        OffsetDateTime start = OffsetDateTime.now().plusDays(61);
        OffsetDateTime end = start.plusHours(1);

        assertFalse(validator.isValid(createRequest(start, end), context));
        verify(context).buildConstraintViolationWithTemplate(
                "Reservations can only be made up to 60 days in advance"
        );
    }

    private ITimeFramedRequest createRequest(OffsetDateTime start, OffsetDateTime end) {
        return new ITimeFramedRequest() {
            @Override
            public OffsetDateTime getStartTime() {
                return start;
            }

            @Override
            public OffsetDateTime getEndTime() {
                return end;
            }
        };
    }
}