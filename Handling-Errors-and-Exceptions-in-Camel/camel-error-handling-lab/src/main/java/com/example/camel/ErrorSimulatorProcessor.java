package com.example.camel;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorSimulatorProcessor implements Processor {
    private static final Logger logger = LoggerFactory.getLogger(ErrorSimulatorProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class);
        int redeliveryCount = exchange.getIn().getHeader(Exchange.REDELIVERY_COUNTER, 0, Integer.class);

        logger.info("Processing message (redelivery {}): {}", redeliveryCount, body);

        if (body.contains("business-error")) {
            logger.error("Simulating business exception for message: {}", body);
            throw new BusinessException("Business rule violation: invalid customer data");
        }

        if (body.contains("technical-error")) {
            logger.error("Simulating technical exception (redelivery {}): {}", redeliveryCount, body);
            throw new TechnicalException("Database connection failed");
        }

        if (body.contains("retry-success")) {
            // Fails on the first two attempts (initial + 1 redelivery),
            // succeeds once CamelRedeliveryCounter reaches 2.
            if (redeliveryCount < 2) {
                logger.error("Simulating temporary failure (redelivery {}): {}", redeliveryCount, body);
                throw new TechnicalException("Temporary service unavailable");
            }
            logger.info("Processing succeeded after {} redeliveries: {}", redeliveryCount, body);
        }

        logger.info("Successfully processed message: {}", body);
        exchange.getIn().setBody("Processed: " + body);
    }
}
