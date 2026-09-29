package com.alnafi.camel.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Custom processor that enriches messages with timestamp and metadata
 */
public class MessageEnricherProcessor implements Processor {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageEnricherProcessor.class);
    private final String processorName;
    
    public MessageEnricherProcessor(String processorName) {
        this.processorName = processorName;
    }
    
    @Override
    public void process(Exchange exchange) throws Exception {
        // Get the original message body
        String originalMessage = exchange.getIn().getBody(String.class);
        
        // Log the processing activity
        logger.info("Processing message in {}: {}", processorName, originalMessage);
        
        // Create timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        // Enrich the message with metadata
        String enrichedMessage = createEnrichedMessage(originalMessage, timestamp);
        
        // Set the enriched message as the new body
        exchange.getIn().setBody(enrichedMessage);
        
        // Add custom headers
        exchange.getIn().setHeader("ProcessedBy", processorName);
        exchange.getIn().setHeader("ProcessedAt", timestamp);
        exchange.getIn().setHeader("MessageLength", enrichedMessage.length());
        
        logger.info("Message enriched successfully. New length: {}", enrichedMessage.length());
    }
    
    private String createEnrichedMessage(String originalMessage, String timestamp) {
        StringBuilder enriched = new StringBuilder();
        enriched.append("=== MESSAGE PROCESSED BY ").append(processorName.toUpperCase()).append(" ===\n");
        enriched.append("Timestamp: ").append(timestamp).append("\n");
        enriched.append("Original Content: ").append(originalMessage).append("\n");
        enriched.append("Content Length: ").append(originalMessage.length()).append(" characters\n");
        enriched.append("Status: PROCESSED\n");
        enriched.append("=== END OF PROCESSED MESSAGE ===");
        
        return enriched.toString();
    }
}
