package dev.emmanuel.account.service;

import dev.emmanuel.account.event.AccountEvent;
import dev.emmanuel.account.exception.CheckingAccountAlreadyOpened;
import dev.emmanuel.account.exception.violation.ViolationException;
import dev.emmanuel.account.persistence.entity.CheckingAccount;
import dev.emmanuel.account.persistence.repository.CheckingAccountRepository;
import dev.emmanuel.account.validator.CheckingAccountInputValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.concurrent.ListenableFuture;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenCheckingAccountService {

    private final KafkaTemplate<Long, AccountEvent> kafkaTemplate;
    private final CheckingAccountRepository checkingAccountRepository;

    @Transactional
    public Mono<CheckingAccount> open(CheckingAccount checkingAccount) {
        CheckingAccountInputValidator inputValidator = new CheckingAccountInputValidator();

        return inputValidator
                .validate(checkingAccount)
                .flatMap(input -> checkingAccountRepository.findByCustomerId(input.getCustomer().getId()))
                .flatMap(existent -> Mono.<CheckingAccount>error(CheckingAccountAlreadyOpened::new))
                .switchIfEmpty(Mono.defer(() -> checkingAccountRepository.save(checkingAccount)))
                .doOnSuccess(this::publishOpenedCheckingAccountEvent)
                .doOnError(CheckingAccountAlreadyOpened.class, this::handleAccountAlreadyOpened)
                .doOnError(ViolationException.class, this::handleInputViolation);
    }

    private ListenableFuture publishOpenedCheckingAccountEvent(CheckingAccount openedCheckingAccount) {
        var event = AccountEvent.of("opened", openedCheckingAccount);
        return kafkaTemplate.send("checking_account_event", openedCheckingAccount.getId(), event);
    }

    private void handleAccountAlreadyOpened(CheckingAccountAlreadyOpened ex) {
        log.error("Failure to open account", ex);
    }

    private void handleInputViolation(ViolationException ex) {
        log.error("Failure to open account, the input is invalid.", ex);
    }

}
