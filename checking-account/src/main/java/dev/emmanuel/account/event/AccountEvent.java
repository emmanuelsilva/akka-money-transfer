package dev.emmanuel.account.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import dev.emmanuel.account.persistence.entity.CheckingAccount;
import lombok.Value;

import java.time.LocalDateTime;

@Value
public class AccountEvent {

    private final String type;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private final LocalDateTime timestamp;

    private final CheckingAccount checkingAccount;

    public static AccountEvent of(String type, CheckingAccount checkingAccount) {
        return new AccountEvent(type, LocalDateTime.now(), checkingAccount);
    }

}
