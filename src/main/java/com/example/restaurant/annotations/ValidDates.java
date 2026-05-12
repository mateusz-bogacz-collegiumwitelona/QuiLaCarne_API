package com.example.restaurant.annotations;

import com.example.restaurant.validators.reservation.ReservationDatesValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = {ReservationDatesValidator.class})
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDates {
  String message() default "Invalid date";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
