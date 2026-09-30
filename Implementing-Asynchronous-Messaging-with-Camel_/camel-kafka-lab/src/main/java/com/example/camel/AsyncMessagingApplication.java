package com.example.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsyncMessagingApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncMessagingApplication.class);
    
    public static void main(String[] args) throws Exception {
        logger.info("Starting Camel Kafka Async Messaging Application...");
        
        // Create Camel context
        CamelContext camelContext = new DefaultCamelContext();
        
        try {
            // Add routes to context
            camelContext.addRoutes(new OrderProducerRoute());
            camelContext.addRoutes(new OrderConsumerRoute());
            camelContext.addRoutes(new ErrorTestRoute());

            
            // Start the context
            camelContext.start();
            
            logger.info("Camel context started successfully!");
            logger.info("Application is running. Press Ctrl+C to stop.");
            
            // Add shutdown hook for graceful shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    logger.info("Shutting down Camel context...");
                    camelContext.stop();
                    logger.info("Camel context stopped successfully.");
                } catch (Exception e) {
                    logger.error("Error stopping Camel context", e);
                }
            }));
            
            // Keep the application running
            Thread.currentThread().join();
            
        } catch (Exception e) {
            logger.error("Error starting application", e);
            camelContext.stop();
        }
    }
}
