package dev.emmanuel.account.validator;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.ConstraintViolations;
import am.ik.yavi.core.Validator;
import dev.emmanuel.account.exception.violation.Violation;
import dev.emmanuel.account.exception.violation.ViolationException;
import dev.emmanuel.account.persistence.entity.CheckingAccount;
import dev.emmanuel.account.persistence.entity.Customer;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

public class CheckingAccountInputValidator {

    private final Validator<CheckingAccount> validator;

    public CheckingAccountInputValidator() {
        this.validator = this.buildInputCheckingAccountValidator();
    }

    private Validator<CheckingAccount> buildInputCheckingAccountValidator() {
        Validator<Customer> customerValidator = ValidatorBuilder.<Customer>of()
                .constraint(Customer::getId, "id", c -> c.greaterThan(0L).message("customer id is required"))
                .constraint(Customer::getName, "name", c -> c.notBlank().message("customer name is required"))
                .build();

        Validator<CheckingAccount> checkingAccountValidator = ValidatorBuilder.<CheckingAccount>of()
                .constraint(CheckingAccount::getIban, "iban", c -> c.notBlank().message("IBAN is required"))
                ._object(CheckingAccount::getCurrency, "currency", c -> c.notNull().message("currency is required"))
                ._object(CheckingAccount::getCustomer, "customer", c -> c.notNull().message("customer must not be null"))
                .nest(CheckingAccount::getCustomer, "customer", customerValidator)
                .build();

        return checkingAccountValidator;
    }

    public Mono<CheckingAccount> validate(CheckingAccount checkingAccount) {
        ConstraintViolations accountViolations = validator.validate(checkingAccount);

        if (!accountViolations.isValid()) {
            var violations = accountViolations
                    .violations()
                    .stream()
                    .map(v -> new Violation(v.name(), v.message()))
                    .collect(Collectors.toList());

            return Mono.error(new ViolationException("Invalid input", violations));
        }

        return Mono.just(checkingAccount);
    }

}
