package com.example.camel;

import org.apache.camel.builder.RouteBuilder;

public class ErrorTestRoute extends RouteBuilder {
    
    @Override
    public void configure() throws Exception {
        
        // Route to test error handling
        from("timer://errorTest?period=15000&delay=10000")
            .routeId("error-test-route")
            .log("Testing error handling...")
            .process(exchange -> {
                // Simulate random errors
                if (Math.random() < 0.3) { // 30% chance of error
                    throw new RuntimeException("Simulated processing error");
                }
                
                Order errorTestOrder = new Order("ERROR-TEST-" + System.currentTimeMillis(), 
                                                "CUST999", "TestProduct", 1, 99.99);
                exchange.getIn().setBody(errorTestOrder);
            })
            .marshal().json()
            .to("kafka:orders?brokers=localhost:9092")
            .log("Error test order sent successfully");
    }
}
