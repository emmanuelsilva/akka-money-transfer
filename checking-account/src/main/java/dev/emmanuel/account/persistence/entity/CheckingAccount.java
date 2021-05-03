package dev.emmanuel.account.persistence.entity;

import dev.emmanuel.account.controller.dto.CheckingAccountDto;
import lombok.Value;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Immutable;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Currency;

@Value
@Immutable
@Table("checking_accounts")
public class CheckingAccount {

    @Id
    private final Long id;

    @Version
    private final Long version;

    /**
     * International Bank Account Number
     * 2 letters CountryCode + 2 digits checksum + BBAN
     * DE89 3704 0044 0532 0130 00 (Sample for Germany)
     */
    private final String iban;

    private final String currency;

    private final Customer customer;

    public CheckingAccount withId(long id) {
        return new CheckingAccount(id, null, this.iban, this.currency, this.customer);
    }

    public static CheckingAccount of(String iban, String currency, Customer customer) {
        return new CheckingAccount(null, null, iban, currency, customer);
    }

    public static CheckingAccount from(CheckingAccountDto dto) {
        var customer = Customer.of(dto.getCustomer().getId(), dto.getCustomer().getName());
        return new CheckingAccount(null, null, dto.getIban(), dto.getCurrencyCode(), customer);
    }

}
