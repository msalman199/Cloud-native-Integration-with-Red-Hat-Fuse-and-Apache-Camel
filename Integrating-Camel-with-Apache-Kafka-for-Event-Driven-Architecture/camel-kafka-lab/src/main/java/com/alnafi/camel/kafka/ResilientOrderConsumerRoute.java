package com.alnafi.camel.kafka;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

public class ResilientOrderConsumerRoute extends RouteBuilder {
    
    @Override
    public void configure() throws Exception {
        
        // Global error handler
        errorHandler(deadLetterChannel("direct:error-handler")
            .maximumRedeliveries(3)
            .redeliveryDelay(2000)
            .retryAttemptedLogLevel(org.apache.camel.LoggingLevel.WARN));
        
        // Main consumer route with error handling
        from("kafka:order-events?brokers=localhost:9092&groupId=resilient-order-processor")
            .routeId("resilient-order-consumer")
            .log("Received order: ${body}")
            .doTry()
                .unmarshal().json(JsonLibrary.Jackson, Order.class)
                .process(exchange -> {
                    Order order = exchange.getIn().getBody(Order.class);
                    
                    // Simulate potential processing error
                    if (order.getOrderId().contains("ERROR")) {
                        throw new RuntimeException("Simulated processing error for order: " + order.getOrderId());
                    }
                    
                    // Normal processing
                    double totalAmount = order.getPrice() * order.getQuantity();
                    if (totalAmount > 500) {
                        order.setPrice(order.getPrice() * 0.9);
                    }
                    order.setStatus("PROCESSED");
                    
                    exchange.getIn().setBody(order);
                })
                .marshal().json(JsonLibrary.Jackson)
                .to("kafka:processed-orders?brokers=localhost:9092")
                .log("Successfully processed order: ${header.orderId}")
            .doCatch(Exception.class)
                .log("Error processing order: ${exception.message}")
                .to("direct:error-handler")
            .end();
        
        // Error handling route
        from("direct:error-handler")
            .routeId("error-handler")
            .log("Processing failed order in error handler: ${body}")
            .process(exchange -> {
                // Log error details
                Exception exception = exchange.getProperty(org.apache.camel.Exchange.EXCEPTION_CAUGHT, Exception.class);
                log.error("Order processing failed: {}", exception.getMessage());
                
                // Could send to dead letter queue or alert system
                exchange.getIn().setHeader("errorReason", exception.getMessage());
            })
            .to("kafka:failed-orders?brokers=localhost:9092")
            .log("Failed order sent to failed-orders topic");
    }
}
