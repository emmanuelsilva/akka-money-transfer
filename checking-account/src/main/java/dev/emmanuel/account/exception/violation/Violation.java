package dev.emmanuel.account.exception.violation;

import lombok.Value;

@Value
public class Violation {

    private final String property;
    private final String message;

}
