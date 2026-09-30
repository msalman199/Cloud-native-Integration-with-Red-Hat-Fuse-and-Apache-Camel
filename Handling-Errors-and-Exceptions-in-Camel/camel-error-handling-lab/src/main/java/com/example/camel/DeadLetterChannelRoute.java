package com.example.camel;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeadLetterChannelRoute extends RouteBuilder {
    private static final Logger logger = LoggerFactory.getLogger(DeadLetterChannelRoute.class);

    @Override
    public void configure() throws Exception {

        // Dead Letter Channel: after retries are exhausted, failed exchanges
        // are routed to direct:deadLetterQueue instead of being lost.
        errorHandler(deadLetterChannel("direct:deadLetterQueue")
            .maximumRedeliveries(3)
            .redeliveryDelay(1000)
            .backOffMultiplier(2)
            .maximumRedeliveryDelay(8000)
            .retryAttemptedLogLevel(LoggingLevel.WARN)
            .retriesExhaustedLogLevel(LoggingLevel.ERROR)
            .logRetryAttempted(true)
            .logExhausted(true)
            .logHandled(true));

        // Business exceptions are not transient: never retry, route directly
        // to a dedicated business-error queue for manual review.
        onException(BusinessException.class)
            .handled(true)
            .maximumRedeliveries(0)
            .log(LoggingLevel.ERROR, "Business exception - routing to business error queue: ${exception.message}")
            .to("direct:businessErrorQueue");

        // Main processing route
        from("file:input?noop=true&delay=2000&idempotent=false")
            .routeId("dlq-main-route")
            .log("Processing file: ${header.CamelFileName}")
            .setHeader("OriginalFileName", header("CamelFileName"))
            .setHeader("ProcessingStartTime", simple("${date:now:yyyy-MM-dd HH:mm:ss}"))
            .process(new ErrorSimulatorProcessor())
            .log("Successfully processed: ${header.CamelFileName}")
            .to("file:output/success?fileName=success-${header.OriginalFileName}");

        // Dead Letter Queue handler: reached only after retries are exhausted
        from("direct:deadLetterQueue")
            .routeId("dead-letter-handler")
            .log(LoggingLevel.ERROR,
                 "Message sent to Dead Letter Queue after ${header.CamelRedeliveryCounter} redeliveries")
            .setHeader("ErrorTimestamp", simple("${date:now:yyyy-MM-dd HH:mm:ss}"))
            .setHeader("ErrorReason", simple("${exception.message}"))
            .setHeader("RetryCount", header("CamelRedeliveryCounter"))
            .process(new DeadLetterProcessor())
            .to("file:output/dead-letter?fileName=DLQ-${header.OriginalFileName}-${date:now:yyyyMMdd-HHmmssSSS}.txt")
            .log("Dead letter message saved to file system");

        // Business error queue handler
        from("direct:businessErrorQueue")
            .routeId("business-error-handler")
            .setHeader("ErrorTimestamp", simple("${date:now:yyyy-MM-dd HH:mm:ss}"))
            .process(new BusinessErrorProcessor())
            .to("file:output/business-errors?fileName=BIZ-ERROR-${header.OriginalFileName}-${date:now:yyyyMMdd-HHmmssSSS}.txt")
            .log("Business error logged and saved");
    }

    // Formats and logs messages that exhausted all retries
    public static class DeadLetterProcessor implements Processor {
        private static final Logger logger = LoggerFactory.getLogger(DeadLetterProcessor.class);

        @Override
        public void process(Exchange exchange) throws Exception {
            String originalBody = exchange.getIn().getBody(String.class);
            String fileName = exchange.getIn().getHeader("OriginalFileName", String.class);
            String errorReason = exchange.getIn().getHeader("ErrorReason", String.class);
            Integer retryCount = exchange.getIn().getHeader("RetryCount", Integer.class);
            String errorTimestamp = exchange.getIn().getHeader("ErrorTimestamp", String.class);

            StringBuilder dlqMessage = new StringBuilder();
            dlqMessage.append("=== DEAD LETTER QUEUE MESSAGE ===\n");
            dlqMessage.append("Timestamp: ").append(errorTimestamp).append("\n");
            dlqMessage.append("Original File: ").append(fileName).append("\n");
            dlqMessage.append("Retry Count: ").append(retryCount).append("\n");
            dlqMessage.append("Error Reason: ").append(errorReason).append("\n");
            dlqMessage.append("Original Message: ").append(originalBody).append("\n");
            dlqMessage.append("=== END DLQ MESSAGE ===\n");

            exchange.getIn().setBody(dlqMessage.toString());

            logger.error("Message moved to DLQ - File: {}, Retries: {}, Error: {}",
                        fileName, retryCount, errorReason);
        }
    }

    // Formats and logs business rule violations
    public static class BusinessErrorProcessor implements Processor {
        private static final Logger logger = LoggerFactory.getLogger(BusinessErrorProcessor.class);

        @Override
        public void process(Exchange exchange) throws Exception {
            String originalBody = exchange.getIn().getBody(String.class);
            String fileName = exchange.getIn().getHeader("OriginalFileName", String.class);
            String errorTimestamp = exchange.getIn().getHeader("ErrorTimestamp", String.class);
            Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
            String errorMessageText = (exception != null) ? exception.getMessage() : "unknown";

            StringBuilder errorMessage = new StringBuilder();
            errorMessage.append("=== BUSINESS ERROR MESSAGE ===\n");
            errorMessage.append("Timestamp: ").append(errorTimestamp).append("\n");
            errorMessage.append("Original File: ").append(fileName).append("\n");
            errorMessage.append("Error Type: BUSINESS_EXCEPTION\n");
            errorMessage.append("Error Message: ").append(errorMessageText).append("\n");
            errorMessage.append("Original Message: ").append(originalBody).append("\n");
            errorMessage.append("Action Required: Manual review and correction needed\n");
            errorMessage.append("=== END BUSINESS ERROR MESSAGE ===\n");

            exchange.getIn().setBody(errorMessage.toString());

            logger.warn("Business error logged - File: {}, Error: {}", fileName, errorMessageText);
        }
    }
}
