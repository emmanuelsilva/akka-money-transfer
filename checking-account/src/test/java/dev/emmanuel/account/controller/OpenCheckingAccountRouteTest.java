package dev.emmanuel.account.controller;

import dev.emmanuel.account.controller.dto.CheckingAccountDto;
import dev.emmanuel.account.controller.dto.CustomerDto;
import dev.emmanuel.account.exception.CheckingAccountAlreadyOpened;
import dev.emmanuel.account.exception.violation.Violation;
import dev.emmanuel.account.exception.violation.ViolationException;
import dev.emmanuel.account.persistence.entity.CheckingAccount;
import dev.emmanuel.account.service.OpenCheckingAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = OpenCheckingAccountRoute.class)
@WebFluxTest
class OpenCheckingAccountRouteTest {

    private static final String CHECKING_ACCOUNT_ENDPOINT = "/checking-accounts";

    @MockBean
    private OpenCheckingAccountService openCheckingAccountService;

    @Autowired
    private OpenCheckingAccountRoute route;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        this.webTestClient = WebTestClient
                .bindToRouterFunction(route.openAccount())
                .build();
    }

    @Test
    void shouldOpenAccount() {
        var customerDto = new CustomerDto(1L, "Mock User");
        var accountDto = new CheckingAccountDto("IBAN", "EUR", customerDto);

        var account = CheckingAccount.from(accountDto);
        when(openCheckingAccountService.open(account)).thenReturn(Mono.just(account.withId(50L)));

        webTestClient
                .post()
                .uri(CHECKING_ACCOUNT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(accountDto), CheckingAccount.class)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(50L)
                .jsonPath("$.iban").isEqualTo("IBAN")
                .jsonPath("$.currency").isEqualTo("EUR")
                .jsonPath("$.customer.id").isEqualTo(1L)
                .jsonPath("$.customer.name").isEqualTo("Mock User");
    }

    @Test
    void shouldReturnBadRequestWhenAccountIsAlreadyOpened() {
        var customerDTO = new CustomerDto(1L, "Mock User");
        var accountDTO = new CheckingAccountDto(null, "EUR", customerDTO);

        when(openCheckingAccountService.open(any(CheckingAccount.class)))
                .thenReturn(Mono.error(CheckingAccountAlreadyOpened::new));

        webTestClient
                .post()
                .uri(CHECKING_ACCOUNT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(accountDTO), CheckingAccount.class)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Account already opened");
    }

    @Test
    void shouldReturnBandRequestWhenViolationExceptionHappens() {
        var customerDTO = new CustomerDto(1L, "Mock User");
        var accountDTO = new CheckingAccountDto(null, "EUR", customerDTO);

        ViolationException violationException = new ViolationException(
                "Invalid input",
                Arrays.asList(new Violation("myProperty", "The property is invalid"))
        );

        when(openCheckingAccountService.open(any(CheckingAccount.class)))
                .thenThrow(violationException);

        webTestClient
                .post()
                .uri(CHECKING_ACCOUNT_ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(accountDTO), CheckingAccount.class)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.message").isEqualTo("Invalid input")
                .jsonPath("$.violations").exists()
                .jsonPath("$.violations[0].property").isEqualTo("myProperty")
                .jsonPath("$.violations[0].message").isEqualTo("The property is invalid");
    }
}