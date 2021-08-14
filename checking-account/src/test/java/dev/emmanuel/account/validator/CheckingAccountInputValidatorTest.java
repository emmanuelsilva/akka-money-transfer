package dev.emmanuel.account.validator;

import dev.emmanuel.account.exception.violation.ViolationException;
import dev.emmanuel.account.persistence.entity.CheckingAccount;
import dev.emmanuel.account.persistence.entity.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class CheckingAccountInputValidatorTest {

    private CheckingAccountInputValidator inputValidator;

    @BeforeEach
    void setUp() {
        this.inputValidator = new CheckingAccountInputValidator();
    }

    @Test
    void shouldDoNotRejectValidInput() {
        var customer = Customer.of(1, "Mock Customer");
        var checkingAccount = CheckingAccount.of("IBAN", "EUR", customer);

        StepVerifier
                .create(this.inputValidator.validate(checkingAccount))
                .expectNext(checkingAccount);
    }

    @Test
    void shouldRejectEmptyIban() {
        var customer = Customer.of(1, "Mock Customer");
        var checkingAccount = CheckingAccount.of("", "EUR", customer);

        StepVerifier
                .create(this.inputValidator.validate(checkingAccount))
                .expectError(ViolationException.class);
    }

    @Test
    void shouldRejectNullCurrency() {
        var customer = Customer.of(1, "Mock Customer");
        var checkingAccount = CheckingAccount.of("DE89101010101010101", null, customer);

        StepVerifier
                .create(this.inputValidator.validate(checkingAccount))
                .expectError(ViolationException.class);
    }

    @Test
    void shouldRejectNullCustomer() {
        var checkingAccount = CheckingAccount.of("DE89101010101010101", "EUR", null);

        StepVerifier
                .create(this.inputValidator.validate(checkingAccount))
                .expectError(ViolationException.class);
    }

    @Test
    void shouldRejectCustomerIdZero() {
        var customer = Customer.of(0, "Mock Customer");
        var checkingAccount = CheckingAccount.of("IBAN", "EUR", customer);

        StepVerifier
                .create(this.inputValidator.validate(checkingAccount))
                .expectError(ViolationException.class);
    }

    @Test
    void shouldRejectNullCustomerName() {
        var customer = Customer.of(0, null);
        var checkingAccount = CheckingAccount.of("IBAN", "EUR", customer);

        StepVerifier
                .create(this.inputValidator.validate(checkingAccount))
                .expectError(ViolationException.class);
    }
}