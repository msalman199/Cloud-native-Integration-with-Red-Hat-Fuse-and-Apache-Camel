package com.alnafi.camel.processor;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MessageValidatorProcessorTest {
    
    private MessageValidatorProcessor processor;
    private Exchange exchange;
    
    @BeforeEach
    void setUp() {
        processor = new MessageValidatorProcessor(10, 100);
        exchange = new DefaultExchange(new DefaultCamelContext());
    }
    
    @Test
    void testValidMessageProcessing() throws Exception {
        // Given
        String validMessage = "This is a valid message for testing";
        exchange.getIn().setBody(validMessage);
        
        // When
        processor.process(exchange);
        
        // Then
        assertEquals("VALID", exchange.getIn().getHeader("ValidationStatus"));
        assertTrue(exchange.getIn().getBody(String.class).contains("[VALIDATED]"));
        assertTrue(exchange.getIn().getBody(String.class).contains("[VALIDATION_PASSED]"));
    }
    
    @Test
    void testInvalidShortMessage() throws Exception {
        // Given
        String shortMessage = "Short";
        exchange.getIn().setBody(shortMessage);
        
        // When
        processor.process(exchange);
        
        // Then
        assertEquals("INVALID", exchange.getIn().getHeader("ValidationStatus"));
        assertTrue(exchange.getIn().getBody(String.class).contains("[VALIDATION_FAILED]"));
        assertTrue(exchange.getIn().getHeader("ValidationErrors").toString().contains("too short"));
    }
    
    @Test
    void testInvalidLongMessage() throws Exception {
        // Given
        String longMessage = "This is a very long message that exceeds the maximum length limit and should trigger validation errors in our processor";
        exchange.getIn().setBody(longMessage);
        
        // When
        processor.process(exchange);
        
        // Then
        assertEquals("INVALID", exchange.getIn().getHeader("ValidationStatus"));
        assertTrue(exchange.getIn().getHeader("ValidationErrors").toString().contains("too long"));
    }
    
    @Test
    void testProhibitedKeywords() throws Exception {
        // Given
        String messageWithError = "This message contains an error";
        exchange.getIn().setBody(messageWithError);
        
        // When
        processor.process(exchange);
        
        // Then
        assertEquals("INVALID", exchange.getIn().getHeader("ValidationStatus"));
        assertTrue(exchange.getIn().getHeader("ValidationErrors").toString().contains("prohibited keywords"));
    }
    
    @Test
    void testWordCount() throws Exception {
        // Given
        String message = "This is a test message";
        exchange.getIn().setBody(message);
        
        // When
        processor.process(exchange);
        
        // Then
        assertEquals(5, exchange.getIn().getHeader("MessageWordCount"));
    }
}
