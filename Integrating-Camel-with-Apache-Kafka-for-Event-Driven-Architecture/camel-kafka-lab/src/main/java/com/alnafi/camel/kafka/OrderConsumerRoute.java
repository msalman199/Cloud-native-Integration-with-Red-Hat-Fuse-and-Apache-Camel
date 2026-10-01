package com.alnafi.camel.kafka;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

public class OrderConsumerRoute extends RouteBuilder {
    
    @Override
    public void configure() throws Exception {
        
        // Route to consume orders from Kafka and process them
        from("kafka:order-events?brokers=localhost:9092&groupId=order-processor-group")
            .routeId("order-consumer-route")
            .log("Received order from Kafka: ${body}")
            .unmarshal().json(JsonLibrary.Jackson, Order.class)
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                
                // Simulate order processing logic
                log.info("Processing order: {}", order);
                
                // Calculate total amount
                double totalAmount = order.getPrice() * order.getQuantity();
                
                // Apply business logic - discount for large orders
                if (totalAmount > 500) {
                    order.setPrice(order.getPrice() * 0.9); // 10% discount
                    log.info("Applied 10% discount for large order");
                }
                
                // Update order status
                order.setStatus("PROCESSED");
                
                exchange.getIn().setBody(order);
                exchange.getIn().setHeader("processedOrderId", order.getOrderId());
            })
            .log("Order processed: ${body}")
            .marshal().json(JsonLibrary.Jackson)
            .to("kafka:processed-orders?brokers=localhost:9092")
            .log("Processed order sent to processed-orders topic");
    }
}
