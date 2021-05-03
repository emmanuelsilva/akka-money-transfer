package dev.emmanuel.account.configuration;

import dev.emmanuel.account.persistence.converter.CheckingAccountReaderConverter;
import dev.emmanuel.account.persistence.converter.CheckingAccountWriterConverter;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class R2dbcDatabaseConfiguration extends AbstractR2dbcConfiguration {

    @Value("${spring.r2dbc.url}")
    private String url;

    @Bean
    public ConnectionFactoryInitializer initializeDatabaseConnection(ConnectionFactory connectionFactory) {
        ConnectionFactoryInitializer initializer = new ConnectionFactoryInitializer();
        initializer.setConnectionFactory(connectionFactory);

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("schema.sql"));
        initializer.setDatabasePopulator(populator);

        return initializer;
    }

    @Override
    public ConnectionFactory connectionFactory() {
        ConnectionFactoryOptions connectionFactoryOptions = ConnectionFactoryOptions
                .parse(this.url)
                .mutate()
                .build();

        return ConnectionFactories.find(connectionFactoryOptions);
    }

    @Override
    protected List<Object> getCustomConverters() {
        List<Object> customConverters = new ArrayList<>();

        customConverters.add(new CheckingAccountReaderConverter());
        customConverters.add(new CheckingAccountWriterConverter());

        return customConverters;
    }
}
