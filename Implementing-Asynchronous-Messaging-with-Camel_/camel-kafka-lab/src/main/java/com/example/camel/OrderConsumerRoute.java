package com.example.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

public class OrderConsumerRoute extends RouteBuilder {
    
    @Override
    public void configure() throws Exception {
        
        // Configure error handling
        errorHandler(deadLetterChannel("direct:orderError")
            .maximumRedeliveries(3)
            .redeliveryDelay(2000)
            .retryAttemptedLogLevel(org.apache.camel.LoggingLevel.WARN));
        
        // Route to consume orders from Kafka asynchronously
        from("kafka:orders?brokers=localhost:9092&groupId=order-processing-group&autoOffsetReset=earliest")
            .routeId("order-consumer-route")
            .log("Received order from Kafka: ${body}")
            .unmarshal().json(JsonLibrary.Jackson, Order.class)
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                log.info("Processing order: {}", order);
                
                // Simulate order processing time
                Thread.sleep(1000);
                
                // Update order status
                order.setStatus("PROCESSED");
                exchange.getIn().setBody(order);
                
                log.info("Order processed successfully: {}", order);
            })
            .marshal().json(JsonLibrary.Jackson)
            .log("Order processing completed: ${body}")
            // Send processed order to notification route
            .to("direct:orderProcessed");
            
        // Route to consume notifications
        from("kafka:notifications?brokers=localhost:9092&groupId=notification-group&autoOffsetReset=earliest")
            .routeId("notification-consumer-route")
            .log("Received notification: ${body}")
            .process(exchange -> {
                String notification = exchange.getIn().getBody(String.class);
                log.info("Processing notification: {}", notification);
                
                // Here you could send email, SMS, or update a database
                // For this lab, we'll just log the notification
                System.out.println("=== NOTIFICATION PROCESSED ===");
                System.out.println(notification);
                System.out.println("===============================");
            });
            
        // Error handling route
        from("direct:orderError")
            .routeId("order-error-handler")
            .log("Error processing order: ${exception.message}")
            .process(exchange -> {
                Exception exception = exchange.getProperty(org.apache.camel.Exchange.EXCEPTION_CAUGHT, Exception.class);
                String errorMessage = String.format(
                    "{\"error\":\"Order processing failed\",\"message\":\"%s\",\"timestamp\":\"%s\"}",
                    exception.getMessage(),
                    java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                );
                exchange.getIn().setBody(errorMessage);
                log.error("Order processing error: {}", errorMessage);
            });
    }
}
