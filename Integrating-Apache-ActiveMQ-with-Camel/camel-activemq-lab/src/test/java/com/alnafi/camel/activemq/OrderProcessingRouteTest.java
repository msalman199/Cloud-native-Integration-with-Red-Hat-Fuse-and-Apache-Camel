package com.alnafi.camel.activemq;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class OrderProcessingRouteTest extends CamelTestSupport {

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // Redelivery policy mirrors ErrorHandlingRoute, but points at an
                // in-memory mock endpoint instead of a live JMS dead letter queue
                errorHandler(deadLetterChannel("mock:dlq")
                    .maximumRedeliveries(2)
                    .redeliveryDelay(0));

                from("direct:start")
                    .routeId("test-order-processing-route")
                    .process(new OrderProcessor())
                    .to("mock:result");

                from("direct:error")
                    .routeId("test-error-route")
                    .choice()
                        .when(body().contains("ERROR"))
                            .throwException(new RuntimeException("Simulated processing error"))
                        .otherwise()
                            .to("mock:success");
            }
        };
    }

    @Test
    public void testOrderProcessing() throws Exception {
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);

        template.sendBody("direct:start", "ORDER_ID:1|CUSTOMER:Test|PRODUCT:Laptop|QUANTITY:1|PRICE:$999.99");

        mockResult.assertIsSatisfied();

        String processedBody = mockResult.getReceivedExchanges().get(0).getIn().getBody(String.class);
        assertTrue(processedBody.contains("PROCESSED_ORDER"),
            "Processed body should contain the PROCESSED_ORDER marker");
        assertTrue(processedBody.contains("ORDER_ID:1"),
            "Processed body should retain the original order content");
    }

    @Test
    public void testOrderProcessorSetsHeaders() throws Exception {
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        mockResult.expectedHeaderReceived("OrderStatus", "PROCESSED");

        template.sendBody("direct:start", "ORDER_ID:2|CUSTOMER:Test2|PRODUCT:Tablet|QUANTITY:2|PRICE:$450.00");

        mockResult.assertIsSatisfied();
    }

    @Test
    public void testSuccessfulOrderDoesNotHitDlq() throws Exception {
        MockEndpoint mockSuccess = getMockEndpoint("mock:success");
        MockEndpoint mockDlq = getMockEndpoint("mock:dlq");
        mockSuccess.expectedMessageCount(1);
        mockDlq.expectedMessageCount(0);

        template.sendBody("direct:error", "NORMAL_ORDER:Everything is fine");

        mockSuccess.assertIsSatisfied();
        mockDlq.assertIsSatisfied();
    }

    @Test
    public void testFailingOrderIsRedeliveredThenSentToDlq() throws Exception {
        MockEndpoint mockDlq = getMockEndpoint("mock:dlq");
        MockEndpoint mockSuccess = getMockEndpoint("mock:success");
        mockDlq.expectedMessageCount(1);
        mockSuccess.expectedMessageCount(0);

        template.sendBody("direct:error", "ERROR_ORDER:This will fail every time");

        mockDlq.assertIsSatisfied();
        mockSuccess.assertIsSatisfied();
    }
}
