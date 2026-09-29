package com.alnafi.camel.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PerformanceTestProcessor implements Processor {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceTestProcessor.class);
    private long totalProcessingTime = 0;
    private int messageCount = 0;
    
    @Override
    public void process(Exchange exchange) throws Exception {
        long startTime = System.currentTimeMillis();
        
        // Simulate processing work
        String message = exchange.getIn().getBody(String.class);
        String processedMessage = "PERFORMANCE_TEST: " + message + " [Processed at: " + startTime + "]";
        
        // Add some processing delay to simulate real work
        Thread.sleep(10);
        
        exchange.getIn().setBody(processedMessage);
        
        long endTime = System.currentTimeMillis();
        long processingTime = endTime - startTime;
        
        synchronized (this) {
            totalProcessingTime += processingTime;
            messageCount++;
            
            if (messageCount % 10 == 0) {
                double averageTime = (double) totalProcessingTime / messageCount;
                logger.info("Performance Stats - Messages: {}, Average Time: {:.2f}ms", 
                           messageCount, averageTime);
            }
        }
        
        exchange.getIn().setHeader("ProcessingTime", processingTime);
        exchange.getIn().setHeader("MessageNumber", messageCount);
    }
}
