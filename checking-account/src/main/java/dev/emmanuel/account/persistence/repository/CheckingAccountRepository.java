package dev.emmanuel.account.persistence.repository;

import dev.emmanuel.account.persistence.entity.CheckingAccount;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface CheckingAccountRepository extends ReactiveCrudRepository<CheckingAccount, Long> {

    @Query("select ca.* from checking_accounts ca where ca.customer_id = :customerId")
    Mono<CheckingAccount> findByCustomerId(long customerId);
}
