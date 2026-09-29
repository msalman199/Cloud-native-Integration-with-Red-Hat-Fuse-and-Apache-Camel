package com.alnafi.camel;

import com.alnafi.camel.route.ProcessorDemoRouteBuilder;
import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application class for Camel Processor demonstration
 */
public class CamelProcessorApplication {
    
    private static final Logger logger = LoggerFactory.getLogger(CamelProcessorApplication.class);
    
    public static void main(String[] args) throws Exception {
        logger.info("Starting Camel Processor Demo Application...");
        
        // Create Camel Main instance
        Main main = new Main();
        
        // Add route builder
        main.addRouteBuilder(new ProcessorDemoRouteBuilder());
        
        // Configure application properties
        main.bind("applicationName", "Camel Processor Lab");
        main.bind("version", "1.0.0");
        
        // Set shutdown timeout
        main.configure().setShutdownTimeout(10);
        
        logger.info("Application configured successfully");
        logger.info("Routes will start processing messages...");
        logger.info("Press Ctrl+C to stop the application");
        
        // Run the application
        main.run(args);
    }
}
