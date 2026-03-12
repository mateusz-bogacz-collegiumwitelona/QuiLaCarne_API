package com.example.restaurant.validators;

import com.example.restaurant.exceptions.InvalidDateException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.OffsetDateTime;

public class ReservationDatesValidator implements ConstraintValidator<ValidDates, ITimeFramedRequest > {

    @Override
    public boolean isValid(ITimeFramedRequest request, ConstraintValidatorContext context) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            return true;
        }

        OffsetDateTime now = OffsetDateTime.now();

        if (request.getStartTime().isAfter(request.getEndTime()) || request.getStartTime().isEqual(request.getEndTime())) {
            throw new InvalidDateException("Start time must be before end time");
        }

        if (request.getStartTime().isBefore(now.plusMinutes(30))) {
            throw new InvalidDateException("Reservations must be made at least 30 minutes in advance");
        }

        if (request.getStartTime().isAfter(now.plusDays(60))) {
            throw new InvalidDateException("Reservations can only be made up to 60 days in advance");
        }

        return true;
    }


}
