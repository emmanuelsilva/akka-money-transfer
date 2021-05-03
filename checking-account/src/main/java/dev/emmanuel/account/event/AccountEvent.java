package dev.emmanuel.account.event;

import dev.emmanuel.account.persistence.entity.CheckingAccount;
import lombok.Value;

import java.time.Instant;

@Value
public class AccountEvent {

    private final String type;
    private final Instant timestamp;
    private final CheckingAccount checkingAccount;

    public static AccountEvent of(String type, CheckingAccount checkingAccount) {
        return new AccountEvent(type, Instant.now(), checkingAccount);
    }

}
