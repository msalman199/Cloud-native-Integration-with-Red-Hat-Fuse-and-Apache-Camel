package com.alnafi.camel.activemq;

import org.apache.camel.builder.RouteBuilder;

/**
 * Route for handling errors and implementing reliability patterns
 */
public class ErrorHandlingRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        errorHandler(deadLetterChannel("jms:queue:orders.dlq")
            .maximumRedeliveries(3)
            .redeliveryDelay(2000)
            .retryAttemptedLogLevel(org.apache.camel.LoggingLevel.WARN)
            .retriesExhaustedLogLevel(org.apache.camel.LoggingLevel.ERROR));

        from("jms:queue:orders.test.error")
            .routeId("error-test-route")
            .log("Processing potentially failing order: ${body}")
            .choice()
                .when(body().contains("ERROR"))
                    .log("Simulating processing error for: ${body}")
                    .throwException(new RuntimeException("Simulated processing error"))
                .otherwise()
                    .log("Order processed successfully: ${body}")
                    .to("jms:queue:orders.test.success");

        from("jms:queue:orders.dlq")
            .routeId("dlq-monitor-route")
            .log("Message arrived in Dead Letter Queue: ${body}")
            .log("DLQ Headers: ${headers}")
            .log("Exception: ${header.CamelExceptionCaught}")
            .transform(simple("DLQ_PROCESSED: ${body} - Failed after ${header.JMSXDeliveryCount} attempts"));
    }
}
