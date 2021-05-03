package dev.emmanuel.account.persistence.entity;

import lombok.Value;

@Value(staticConstructor = "of")
public class Customer {

    private final long id;
    private final String name;

}
