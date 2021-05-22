package dev.emmanuel.account.persistence.converter;

import dev.emmanuel.account.persistence.entity.CheckingAccount;
import dev.emmanuel.account.persistence.entity.Customer;
import io.r2dbc.spi.Row;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

@Component
@ReadingConverter
public class CheckingAccountReaderConverter implements Converter<Row, CheckingAccount> {

    @Override
    public CheckingAccount convert(Row row) {
        var id = row.get("id", Long.class);
        var version = row.get("version", Long.class);
        var iban = row.get("iban", String.class);
        var currencyCode = row.get("currency", String.class);

        Customer customer = readCustomer(row);

        return new CheckingAccount(
                id,
                version,
                iban,
                currencyCode,
                customer
        );
    }

    private Customer readCustomer(Row row) {
        var customerId = row.get("customer_id", Long.class);
        var customerName = row.get("customer_name", String.class);
        return Customer.of(customerId, customerName);
    }
}
