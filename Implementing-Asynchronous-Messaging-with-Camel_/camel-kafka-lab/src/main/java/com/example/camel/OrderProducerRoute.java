package com.example.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import java.util.Random;

public class OrderProducerRoute extends RouteBuilder {
    
    private final Random random = new Random();
    private final String[] products = {"Laptop", "Smartphone", "Tablet", "Headphones", "Monitor"};
    private final String[] customers = {"CUST001", "CUST002", "CUST003", "CUST004", "CUST005"};
    
    @Override
    public void configure() throws Exception {
        
        // Route to generate and send orders to Kafka
        from("timer://orderGenerator?period=5000&delay=2000")
            .routeId("order-producer-route")
            .log("Generating new order...")
            .process(exchange -> {
                // Create a random order
                String orderId = "ORD-" + System.currentTimeMillis();
                String customerId = customers[random.nextInt(customers.length)];
                String productName = products[random.nextInt(products.length)];
                int quantity = random.nextInt(5) + 1;
                double price = (random.nextDouble() * 1000) + 100;
                
                Order order = new Order(orderId, customerId, productName, quantity, price);
                exchange.getIn().setBody(order);
                
                // Set Kafka message key for partitioning
                exchange.getIn().setHeader("kafka.KEY", customerId);
                
                log.info("Created order: {}", order);
            })
            .marshal().json(JsonLibrary.Jackson)
            .log("Sending order to Kafka topic 'orders': ${body}")
            .to("kafka:orders?brokers=localhost:9092")
            .log("Order sent successfully");
            
        // Route to handle order processing results
        from("direct:orderProcessed")
            .routeId("order-notification-route")
            .log("Processing order notification: ${body}")
            .process(exchange -> {
                String orderData = exchange.getIn().getBody(String.class);
                String notification = String.format(
                    "{\"type\":\"ORDER_PROCESSED\",\"message\":\"Order has been processed\",\"orderData\":%s,\"timestamp\":\"%s\"}",
                    orderData, 
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                );
                exchange.getIn().setBody(notification);
            })
            .log("Sending notification to Kafka: ${body}")
            .to("kafka:notifications?brokers=localhost:9092")
            .log("Notification sent successfully");
    }
}
