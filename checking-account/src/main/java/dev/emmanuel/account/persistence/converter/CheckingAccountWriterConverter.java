package dev.emmanuel.account.persistence.converter;

import dev.emmanuel.account.persistence.entity.CheckingAccount;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.mapping.OutboundRow;
import org.springframework.r2dbc.core.Parameter;
import org.springframework.stereotype.Component;

@Component
@WritingConverter
public class CheckingAccountWriterConverter implements Converter<CheckingAccount, OutboundRow> {

    @Override
    public OutboundRow convert(CheckingAccount checkingAccount) {
        OutboundRow row = new OutboundRow();

        row.put("id", Parameter.fromOrEmpty(checkingAccount.getId(), Long.class));
        row.put("version", Parameter.fromOrEmpty(checkingAccount.getVersion(), Long.class));
        row.put("iban", Parameter.from(checkingAccount.getIban()));
        row.put("currency", Parameter.from(checkingAccount.getCurrency()));
        row.put("customer_id", Parameter.from(checkingAccount.getCustomer().getId()));
        row.put("customer_name", Parameter.from(checkingAccount.getCustomer().getName()));

        return row;
    }
}
