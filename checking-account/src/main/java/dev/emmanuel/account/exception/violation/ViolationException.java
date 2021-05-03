package dev.emmanuel.account.exception.violation;

import java.util.List;

public class ViolationException extends RuntimeException {

    private List<Violation> violations;

    public ViolationException(String message, List<Violation> violations) {
        super(message);
        this.violations = violations;
    }

    public List<Violation> getViolations() {
        return violations;
    }
}
