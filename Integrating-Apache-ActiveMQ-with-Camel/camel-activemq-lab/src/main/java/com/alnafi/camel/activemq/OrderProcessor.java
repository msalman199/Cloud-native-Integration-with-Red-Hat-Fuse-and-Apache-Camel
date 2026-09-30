package com.alnafi.camel.activemq;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Order processor that handles incoming order messages
 */
public class OrderProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(OrderProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        String orderMessage = exchange.getIn().getBody(String.class);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        logger.info("Processing order at {}: {}", timestamp, orderMessage);

        String processedOrder = processOrder(orderMessage, timestamp);

        exchange.getIn().setBody(processedOrder);
        exchange.getIn().setHeader("ProcessedAt", timestamp);
        exchange.getIn().setHeader("OrderStatus", "PROCESSED");

        logger.info("Order processed successfully: {}", processedOrder);
    }

    private String processOrder(String orderMessage, String timestamp) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return String.format("PROCESSED_ORDER[%s] - %s", timestamp, orderMessage);
    }
}
