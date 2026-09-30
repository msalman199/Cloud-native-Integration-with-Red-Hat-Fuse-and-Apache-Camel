package com.example.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class AdvancedRouteTest extends CamelTestSupport {

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:dataProcessor")
                    .routeId("data-processing-route")
                    .log("Received data: ${body}")
                    .process(exchange -> {
                        String body = exchange.getIn().getBody(String.class);
                        String processedData = body.toUpperCase() + "_PROCESSED";
                        exchange.getIn().setBody(processedData);
                        exchange.getIn().setHeader("ProcessedAt", System.currentTimeMillis());
                    })
                    .to("mock:result");
            }
        };
    }

    @BeforeEach
    public void setupMockEndpoints() {
        getMockEndpoint("mock:result").reset();
    }

    @Test
    @DisplayName("Test Data Processing with Custom Processor")
    public void testDataProcessingWithProcessor() throws Exception {
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(1);
        mockResult.expectedBodiesReceived("HELLO WORLD_PROCESSED");

        template.sendBody("direct:dataProcessor", "hello world");

        mockResult.assertIsSatisfied(5000);
    }

    @Test
    @DisplayName("Test Multiple Messages Processing")
    public void testMultipleMessagesProcessing() throws Exception {
        MockEndpoint mockResult = getMockEndpoint("mock:result");
        mockResult.expectedMessageCount(3);
        mockResult.expectedBodiesReceivedInAnyOrder(
            "MESSAGE1_PROCESSED",
            "MESSAGE2_PROCESSED",
            "MESSAGE3_PROCESSED"
        );

        template.sendBody("direct:dataProcessor", "message1");
        template.sendBody("direct:dataProcessor", "message2");
        template.sendBody("direct:dataProcessor", "message3");

        mockResult.assertIsSatisfied();
    }
}
