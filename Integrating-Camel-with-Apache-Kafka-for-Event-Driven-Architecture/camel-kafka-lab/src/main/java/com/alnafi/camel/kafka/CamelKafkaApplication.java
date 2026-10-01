package com.alnafi.camel.kafka;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;

public class CamelKafkaApplication {
    
    public static void main(String[] args) throws Exception {
        
        System.out.println("Starting Camel-Kafka Integration Application...");
        
        // Create Camel Context
        CamelContext camelContext = new DefaultCamelContext();
        
        try {
            // Add routes to context
            camelContext.addRoutes(new OrderProducerRoute());
            camelContext.addRoutes(new OrderConsumerRoute());
            
            // Start the context
            camelContext.start();
            
            System.out.println("Camel Context started successfully!");
            System.out.println("Order Producer Route: Generating orders every 5 seconds");
            System.out.println("Order Consumer Route: Processing orders from Kafka");
            System.out.println("Press Ctrl+C to stop the application");
            
            // Keep the application running
            Thread.sleep(Long.MAX_VALUE);
            
        } catch (Exception e) {
            System.err.println("Error starting Camel Context: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Stop the context
            camelContext.stop();
            System.out.println("Camel Context stopped");
        }
    }
}
