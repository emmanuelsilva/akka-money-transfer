package dev.emmanuel.account.controller;

import dev.emmanuel.account.controller.dto.CheckingAccountDto;
import dev.emmanuel.account.controller.dto.ErrorResponse;
import dev.emmanuel.account.exception.CheckingAccountAlreadyOpened;
import dev.emmanuel.account.exception.violation.ViolationException;
import dev.emmanuel.account.persistence.entity.CheckingAccount;
import dev.emmanuel.account.service.OpenCheckingAccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.BodyInserters.fromValue;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.badRequest;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Configuration
@RequiredArgsConstructor
public class OpenCheckingAccountRoute {

    private final OpenCheckingAccountService openCheckingAccountService;

    @Bean
    RouterFunction<ServerResponse> openAccount() {
        return route(POST("/checking-accounts"), openAccountHandler());
    }

    private HandlerFunction<ServerResponse> openAccountHandler() {
        return request -> request
                .bodyToMono(CheckingAccountDto.class)
                .flatMap(this::toPersistenceModel)
                .flatMap(openCheckingAccountService::open)
                .flatMap(openedAccount -> ok().body(fromValue(openedAccount)))
                .onErrorResume(CheckingAccountAlreadyOpened.class, ex -> badRequest().bodyValue(ErrorResponse.from(ex)))
                .onErrorResume(ViolationException.class, ex -> badRequest().bodyValue(ErrorResponse.from(ex)));
    }

    private Mono<CheckingAccount> toPersistenceModel(CheckingAccountDto dto) {
        return Mono.just(CheckingAccount.from(dto));
    }

}
