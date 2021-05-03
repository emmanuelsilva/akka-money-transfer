package dev.emmanuel.account.controller.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
@NoArgsConstructor(force = true, access = AccessLevel.PRIVATE)
public class CheckingAccountDto {

    private final String iban;
    private final String currencyCode;
    private final CustomerDto customer;
}
