package com.example.camel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * Initializes both XA databases with the shared schema and sample data.
 */
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    private DataSource ordersDataSource;
    private DataSource inventoryDataSource;

    public void setOrdersDataSource(DataSource ordersDataSource) {
        this.ordersDataSource = ordersDataSource;
    }

    public void setInventoryDataSource(DataSource inventoryDataSource) {
        this.inventoryDataSource = inventoryDataSource;
    }

    public void initialize() throws Exception {
        logger.info("Initializing databases...");
        initializeDatabase(ordersDataSource, "Orders");
        initializeDatabase(inventoryDataSource, "Inventory");
        logger.info("Database initialization completed successfully");
    }

    private void initializeDatabase(DataSource dataSource, String dbName) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("schema.sql"));
            logger.info("{} database initialized successfully", dbName);
        }
    }
}
