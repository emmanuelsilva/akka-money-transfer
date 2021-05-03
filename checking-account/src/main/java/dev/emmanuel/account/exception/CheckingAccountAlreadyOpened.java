package dev.emmanuel.account.exception;

public class CheckingAccountAlreadyOpened extends RuntimeException {

    public CheckingAccountAlreadyOpened() {
        super("Account already opened");
    }

}
