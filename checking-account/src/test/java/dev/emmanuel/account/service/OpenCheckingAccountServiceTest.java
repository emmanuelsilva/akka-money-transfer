package dev.emmanuel.account.service;

import dev.emmanuel.account.event.AccountEvent;
import dev.emmanuel.account.exception.CheckingAccountAlreadyOpened;
import dev.emmanuel.account.persistence.entity.CheckingAccount;
import dev.emmanuel.account.persistence.entity.Customer;
import dev.emmanuel.account.persistence.repository.CheckingAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OpenCheckingAccountServiceTest {

    private KafkaTemplate<Long, AccountEvent> kafkaTemplate;
    private CheckingAccountRepository checkingAccountRepository;
    private OpenCheckingAccountService openCheckingAccountService;

    @BeforeEach
    void setUp() {
        this.kafkaTemplate = mock(KafkaTemplate.class);
        this.checkingAccountRepository = mock(CheckingAccountRepository.class);
        this.openCheckingAccountService = new OpenCheckingAccountService(this.kafkaTemplate, this.checkingAccountRepository);
    }

    @Test
    void shouldOpenNewAccount() {
        Customer customer = Customer.of(1, "Mock");
        CheckingAccount checkingAccount = CheckingAccount.of("IBAN", "EUR", customer);

        var savedAccount = checkingAccount.withId(1L);
        when(checkingAccountRepository.findByCustomerId(eq(customer.getId()))).thenReturn(Mono.empty());
        when(checkingAccountRepository.save(checkingAccount)).thenReturn(Mono.just(savedAccount));

        StepVerifier
            .create(openCheckingAccountService.open(checkingAccount))
            .consumeNextWith(openedAccount -> {
                assertEquals(savedAccount, openedAccount);
                assertThatAccountEventWasSent("opened", openedAccount);
            })
            .verifyComplete();
    }

    @Test
    void shouldRejectAlreadyOpenedAccount() {
        Customer customer = Customer.of(1, "Emmanuel");
        CheckingAccount checkingAccount = CheckingAccount.of("IBAN", "EUR", customer);

        var existentAccount = checkingAccount.withId(50L);
        when(checkingAccountRepository.findByCustomerId(eq(customer.getId()))).thenReturn(Mono.just(existentAccount));

        StepVerifier
                .create(openCheckingAccountService.open(checkingAccount))
                .expectError(CheckingAccountAlreadyOpened.class)
                .verify();

        verify(kafkaTemplate, never()).send(eq("checking_account_event"), any(AccountEvent.class));
    }

    private void assertThatAccountEventWasSent(String expectedEventType, CheckingAccount openedCheckingAccount) {
        var accountEventCaptor = ArgumentCaptor.forClass(AccountEvent.class);

        verify(kafkaTemplate).send(
                eq("checking_account_event"),
                eq(openedCheckingAccount.getId()),
                accountEventCaptor.capture()
        );

        var sentEvent = accountEventCaptor.getValue();
        assertEquals(expectedEventType, sentEvent.getType());
        assertEquals(openedCheckingAccount, sentEvent.getCheckingAccount());
    }
}