package com.alnafi.camel.processor;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageEnricherProcessorTest {
    
    private MessageEnricherProcessor processor;
    private Exchange exchange;
    
    @BeforeEach
    void setUp() {
        processor = new MessageEnricherProcessor("TestProcessor");
        exchange = new DefaultExchange(new DefaultCamelContext());
    }
    
    @Test
    void testProcessorEnrichesMessage() throws Exception {
        // Given
        String originalMessage = "Test message";
        exchange.getIn().setBody(originalMessage);
        
        // When
        processor.process(exchange);
        
        // Then
        String processedMessage = exchange.getIn().getBody(String.class);
        assertNotNull(processedMessage);
        assertTrue(processedMessage.contains("TESTPROCESSOR"));
        assertTrue(processedMessage.contains(originalMessage));
        assertTrue(processedMessage.contains("PROCESSED"));
    }
    
    @Test
    void testProcessorAddsHeaders() throws Exception {
        // Given
        String originalMessage = "Test message";
        exchange.getIn().setBody(originalMessage);
        
        // When
        processor.process(exchange);
        
        // Then
        assertEquals("TestProcessor", exchange.getIn().getHeader("ProcessedBy"));
        assertNotNull(exchange.getIn().getHeader("ProcessedAt"));
        assertTrue((Integer) exchange.getIn().getHeader("MessageLength") > originalMessage.length());
    }
    
    @Test
    void testProcessorHandlesNullMessage() throws Exception {
        // Given
        exchange.getIn().setBody(null);
        
        // When
        processor.process(exchange);
        
        // Then
        String processedMessage = exchange.getIn().getBody(String.class);
        assertNotNull(processedMessage);
        assertTrue(processedMessage.contains("null"));
    }
}
