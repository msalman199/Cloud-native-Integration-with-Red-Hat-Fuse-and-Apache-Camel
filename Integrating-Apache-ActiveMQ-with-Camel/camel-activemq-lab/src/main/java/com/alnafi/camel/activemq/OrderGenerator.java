package com.alnafi.camel.activemq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates sample order messages for testing
 */
public class OrderGenerator {

    private static final Logger logger = LoggerFactory.getLogger(OrderGenerator.class);
    private static final AtomicInteger orderCounter = new AtomicInteger(1);
    private static final Random random = new Random();

    private static final String[] PRODUCTS = {
        "Laptop", "Smartphone", "Tablet", "Headphones", "Camera",
        "Watch", "Keyboard", "Mouse", "Monitor", "Printer"
    };

    private static final String[] CUSTOMERS = {
        "John Doe", "Jane Smith", "Bob Johnson", "Alice Brown", "Charlie Wilson",
        "Diana Davis", "Eve Miller", "Frank Garcia", "Grace Lee", "Henry Taylor"
    };

    public String generateOrder() {
        int orderId = orderCounter.getAndIncrement();
        String product = PRODUCTS[random.nextInt(PRODUCTS.length)];
        String customer = CUSTOMERS[random.nextInt(CUSTOMERS.length)];
        int quantity = random.nextInt(5) + 1;
        double price = Math.round((random.nextDouble() * 1000 + 50) * 100.0) / 100.0;

        String order = String.format(
            "ORDER_ID:%d|CUSTOMER:%s|PRODUCT:%s|QUANTITY:%d|PRICE:$%.2f",
            orderId, customer, product, quantity, price
        );

        logger.info("Generated new order: {}", order);
        return order;
    }
}
