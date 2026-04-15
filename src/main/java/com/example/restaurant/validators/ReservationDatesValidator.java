package com.example.restaurant.validators;

import com.example.restaurant.annotations.ValidDates;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.OffsetDateTime;

@SuppressWarnings("PMD.LawOfDemeter")
public class ReservationDatesValidator implements ConstraintValidator<ValidDates, ITimeFramedRequest> {

    @Override
    public boolean isValid(ITimeFramedRequest request, ConstraintValidatorContext context) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            return true;
        }

        OffsetDateTime now = OffsetDateTime.now();
        boolean isValid = true;

        context.disableDefaultConstraintViolation();

        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().isEqual(request.getEndTime())) {
            context.buildConstraintViolationWithTemplate("Start time must be before end time").addConstraintViolation();
            isValid = false;
        } else {
            if (request.getStartTime().isBefore(now.plusMinutes(30))) {
                context.buildConstraintViolationWithTemplate("Reservations must be made at least 30 minutes in advance").addConstraintViolation();
                isValid = false;
            }

            if (request.getStartTime().isAfter(now.plusDays(60))) {
                context.buildConstraintViolationWithTemplate("Reservations can only be made up to 60 days in advance").addConstraintViolation();
                isValid = false;
            }
        }

        return isValid;
    }
}
