package com.example.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class XATransactionApplication {

    private static final Logger logger = LoggerFactory.getLogger(XATransactionApplication.class);

    public static void main(String[] args) throws Exception {
        logger.info("Starting Camel XA Transaction Application...");

        ClassPathXmlApplicationContext springContext =
            new ClassPathXmlApplicationContext("applicationContext.xml");

        CamelContext camelContext = springContext.getBean("camelContext", CamelContext.class);
        ProducerTemplate producerTemplate = camelContext.createProducerTemplate();

        // Scenario 1: successful order, should commit to both databases
        Map<String, Object> headers1 = new HashMap<>();
        headers1.put("customerName", "Alice Brown");
        headers1.put("productName", "Laptop");
        headers1.put("quantity", 2);
        headers1.put("simulateFailure", false);
        producerTemplate.sendBodyAndHeaders("direct:processOrder", "order-success", headers1);

        // Scenario 2: forced failure, both the order insert and inventory update must roll back
        Map<String, Object> headers2 = new HashMap<>();
        headers2.put("customerName", "Bob Johnson");
        headers2.put("productName", "Mouse");
        headers2.put("quantity", 3);
        headers2.put("simulateFailure", true);
        producerTemplate.sendBodyAndHeaders("direct:processOrder", "order-failure", headers2);

        // Verify final state in both databases
        DataSource ordersDataSource = springContext.getBean("ordersXADataSource", DataSource.class);
        DataSource inventoryDataSource = springContext.getBean("inventoryXADataSource", DataSource.class);

        printResults(ordersDataSource, "SELECT id, customer_name, product_name, quantity, status FROM orders", "ORDERS TABLE");
        printResults(inventoryDataSource, "SELECT product_name, available_quantity FROM inventory", "INVENTORY TABLE");

        camelContext.stop();
        springContext.close();
        logger.info("Application shutdown complete.");
    }

    private static void printResults(DataSource dataSource, String query, String label) throws Exception {
        logger.info("---- {} ----", label);
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {

            int columnCount = resultSet.getMetaData().getColumnCount();
            while (resultSet.next()) {
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= columnCount; i++) {
                    row.append(resultSet.getMetaData().getColumnName(i))
                       .append("=")
                       .append(resultSet.getString(i))
                       .append(" ");
                }
                logger.info(row.toString());
            }
        }
    }
}
