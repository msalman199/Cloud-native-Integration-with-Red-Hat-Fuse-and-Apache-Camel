package com.example.camel;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ErrorHandlingTest extends CamelTestSupport {

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                errorHandler(deadLetterChannel("mock:deadLetter")
                    .maximumRedeliveries(2)
                    .redeliveryDelay(50)
                    .logRetryAttempted(true)
                    .logExhausted(true));

                onException(BusinessException.class)
                    .handled(true)
                    .maximumRedeliveries(0)
                    .log(LoggingLevel.ERROR, "Business exception: ${exception.message}")
                    .to("mock:businessError");

                from("direct:start")
                    .process(new ErrorSimulatorProcessor())
                    .to("mock:success");
            }
        };
    }

    @Test
    public void testNormalProcessing() throws Exception {
        MockEndpoint mockSuccess = getMockEndpoint("mock:success");
        MockEndpoint mockDeadLetter = getMockEndpoint("mock:deadLetter");
        MockEndpoint mockBusinessError = getMockEndpoint("mock:businessError");

        mockSuccess.expectedMessageCount(1);
        mockDeadLetter.expectedMessageCount(0);
        mockBusinessError.expectedMessageCount(0);

        template.sendBody("direct:start", "Normal message");

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testBusinessExceptionSkipsRetries() throws Exception {
        MockEndpoint mockSuccess = getMockEndpoint("mock:success");
        MockEndpoint mockDeadLetter = getMockEndpoint("mock:deadLetter");
        MockEndpoint mockBusinessError = getMockEndpoint("mock:businessError");

        mockSuccess.expectedMessageCount(0);
        mockDeadLetter.expectedMessageCount(0);
        mockBusinessError.expectedMessageCount(1);

        template.sendBody("direct:start", "Message with business-error");

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testTechnicalExceptionExhaustsRetriesToDlq() throws Exception {
        MockEndpoint mockSuccess = getMockEndpoint("mock:success");
        MockEndpoint mockDeadLetter = getMockEndpoint("mock:deadLetter");

        mockSuccess.expectedMessageCount(0);
        mockDeadLetter.expectedMessageCount(1);

        template.sendBody("direct:start", "Message with technical-error");

        assertMockEndpointsSatisfied();

        Exchange deadLetterExchange = mockDeadLetter.getReceivedExchanges().get(0);
        Integer redeliveryCounter = deadLetterExchange.getIn().getHeader(Exchange.REDELIVERY_COUNTER, Integer.class);
        assertNotNull(redeliveryCounter, "Expected CamelRedeliveryCounter header to be set");
        assertTrue(redeliveryCounter >= 2, "Expected at least 2 redeliveries before DLQ");
    }

    @Test
    public void testRetrySucceedsBeforeExhaustion() throws Exception {
        MockEndpoint mockSuccess = getMockEndpoint("mock:success");
        MockEndpoint mockDeadLetter = getMockEndpoint("mock:deadLetter");

        mockSuccess.expectedMessageCount(1);
        mockDeadLetter.expectedMessageCount(0);

        template.sendBody("direct:start", "Message with retry-success");

        assertMockEndpointsSatisfied();
    }
}
