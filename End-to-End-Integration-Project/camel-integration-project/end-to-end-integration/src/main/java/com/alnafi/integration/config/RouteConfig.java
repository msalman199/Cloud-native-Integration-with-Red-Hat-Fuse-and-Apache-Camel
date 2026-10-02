package com.alnafi.integration.config;

import com.alnafi.integration.routes.CustomerIntegrationRoute;
import com.alnafi.integration.routes.LoggingRoute;
import com.alnafi.integration.routes.OrderIntegrationRoute;
import org.apache.camel.component.jdbc.JdbcComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class RouteConfig {

    @Bean
    public JdbcComponent jdbcComponent(DataSource dataSource) {
        JdbcComponent jdbc = new JdbcComponent();
        jdbc.setDataSource(dataSource);
        return jdbc;
    }

    @Bean
    public LoggingRoute loggingRoute() {
        return new LoggingRoute();
    }

    @Bean
    public CustomerIntegrationRoute customerIntegrationRoute() {
        return new CustomerIntegrationRoute();
    }

    @Bean
    public OrderIntegrationRoute orderIntegrationRoute() {
        return new OrderIntegrationRoute();
    }
}
