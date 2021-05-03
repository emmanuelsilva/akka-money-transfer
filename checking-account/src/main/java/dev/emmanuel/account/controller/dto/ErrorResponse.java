package dev.emmanuel.account.controller.dto;

import dev.emmanuel.account.exception.violation.Violation;
import dev.emmanuel.account.exception.violation.ViolationException;
import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
public class ErrorResponse {

    private final String message;
    private final List<Violation> violations;

    public static ErrorResponse from(Exception ex) {
        return new ErrorResponse(ex.getMessage(), Collections.emptyList());
    }

    public static ErrorResponse from(ViolationException violationException) {
        return new ErrorResponse(violationException.getMessage(), violationException.getViolations());
    }

}
