package com.example.reactive.config;

import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;

@Configuration
public class DatabaseInitConfig {
    @Bean
    public ConnectionFactoryInitializer initializer(ConnectionFactory cf) {
        var init = new ConnectionFactoryInitializer();
        init.setConnectionFactory(cf);
        init.setDatabasePopulator(new ResourceDatabasePopulator(new ClassPathResource("schema.sql")));
        return init;
    }
}
