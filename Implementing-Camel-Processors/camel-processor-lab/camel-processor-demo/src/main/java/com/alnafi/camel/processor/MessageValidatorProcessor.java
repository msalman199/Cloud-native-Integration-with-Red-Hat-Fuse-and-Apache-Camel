package com.alnafi.camel.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom processor that validates message content and adds validation results
 */
public class MessageValidatorProcessor implements Processor {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageValidatorProcessor.class);
    private final int minLength;
    private final int maxLength;
    
    public MessageValidatorProcessor(int minLength, int maxLength) {
        this.minLength = minLength;
        this.maxLength = maxLength;
    }
    
    @Override
    public void process(Exchange exchange) throws Exception {
        String message = exchange.getIn().getBody(String.class);
        
        logger.info("Validating message: {}", message);
        
        // Perform validation checks
        ValidationResult result = validateMessage(message);
        
        // Add validation headers
        exchange.getIn().setHeader("ValidationStatus", result.isValid() ? "VALID" : "INVALID");
        exchange.getIn().setHeader("ValidationErrors", result.getErrors());
        exchange.getIn().setHeader("MessageWordCount", countWords(message));
        
        // Modify message based on validation
        if (result.isValid()) {
            String validatedMessage = "[VALIDATED] " + message + " [VALIDATION_PASSED]";
            exchange.getIn().setBody(validatedMessage);
            logger.info("Message validation passed");
        } else {
            String errorMessage = "[VALIDATION_FAILED] " + message + " [ERRORS: " + result.getErrors() + "]";
            exchange.getIn().setBody(errorMessage);
            logger.warn("Message validation failed: {}", result.getErrors());
        }
    }
    
    private ValidationResult validateMessage(String message) {
        ValidationResult result = new ValidationResult();
        
        if (message == null || message.trim().isEmpty()) {
            result.addError("Message is null or empty");
            return result;
        }
        
        if (message.length() < minLength) {
            result.addError("Message too short (minimum " + minLength + " characters)");
        }
        
        if (message.length() > maxLength) {
            result.addError("Message too long (maximum " + maxLength + " characters)");
        }
        
        // Check for prohibited content
        if (message.toLowerCase().contains("error") || message.toLowerCase().contains("fail")) {
            result.addError("Message contains prohibited keywords");
        }
        
        return result;
    }
    
    private int countWords(String message) {
        if (message == null || message.trim().isEmpty()) {
            return 0;
        }
        return message.trim().split("\\s+").length;
    }
    
    // Inner class for validation results
    private static class ValidationResult {
        private boolean valid = true;
        private StringBuilder errors = new StringBuilder();
        
        public void addError(String error) {
            if (errors.length() > 0) {
                errors.append("; ");
            }
            errors.append(error);
            valid = false;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getErrors() {
            return errors.toString();
        }
    }
}
