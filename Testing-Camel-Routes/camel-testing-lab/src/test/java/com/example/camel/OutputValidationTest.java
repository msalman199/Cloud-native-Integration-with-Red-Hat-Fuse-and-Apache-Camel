package com.example.camel;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OutputValidationTest extends CamelTestSupport {

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("direct:orderTransformation")
                    .routeId("order-transformation-route")
                    .log("Transforming order: ${body}")
                    .process(exchange -> {
                        String orderData = exchange.getIn().getBody(String.class);
                        String[] parts = orderData.split(",");

                        if (parts.length >= 3) {
                            String orderId = parts[0].trim();
                            String product = parts[1].trim();
                            String quantity = parts[2].trim();

                            String transformedOrder = String.format(
                                "{\"orderId\":\"%s\",\"product\":\"%s\",\"quantity\":%s,\"status\":\"PROCESSED\"}",
                                orderId, product, quantity
                            );

                            exchange.getIn().setBody(transformedOrder);
                            exchange.getIn().setHeader("OrderId", orderId);
                            exchange.getIn().setHeader("Product", product);
                            exchange.getIn().setHeader("Quantity", Integer.parseInt(quantity));
                        }
                    })
                    .to("mock:transformedOrder");
            }
        };
    }

    @Test
    @DisplayName("Test Order Transformation Output Format")
    public void testOrderTransformationOutputFormat() throws Exception {
        MockEndpoint mockEndpoint = getMockEndpoint("mock:transformedOrder");
        mockEndpoint.expectedMessageCount(1);

        String expectedJson = "{\"orderId\":\"ORD-001\",\"product\":\"Laptop\",\"quantity\":2,\"status\":\"PROCESSED\"}";
        mockEndpoint.expectedBodiesReceived(expectedJson);
        mockEndpoint.expectedHeaderReceived("OrderId", "ORD-001");
        mockEndpoint.expectedHeaderReceived("Product", "Laptop");
        mockEndpoint.expectedHeaderReceived("Quantity", 2);

        template.sendBody("direct:orderTransformation", "ORD-001, Laptop, 2");

        mockEndpoint.assertIsSatisfied();
    }

    @Test
    @DisplayName("Test Multiple Orders Output Validation")
    public void testMultipleOrdersOutputValidation() throws Exception {
        MockEndpoint mockEndpoint = getMockEndpoint("mock:transformedOrder");
        mockEndpoint.expectedMessageCount(3);

        template.sendBody("direct:orderTransformation", "ORD-001, Laptop, 1");
        template.sendBody("direct:orderTransformation", "ORD-002, Mouse, 5");
        template.sendBody("direct:orderTransformation", "ORD-003, Keyboard, 2");

        mockEndpoint.assertIsSatisfied();

        List<Exchange> exchanges = mockEndpoint.getReceivedExchanges();

        String firstBody = exchanges.get(0).getIn().getBody(String.class);
        assertTrue(firstBody.contains("\"orderId\":\"ORD-001\""));
        assertTrue(firstBody.contains("\"product\":\"Laptop\""));
        assertTrue(firstBody.contains("\"quantity\":1"));

        String secondBody = exchanges.get(1).getIn().getBody(String.class);
        assertTrue(secondBody.contains("\"orderId\":\"ORD-002\""));
        assertTrue(secondBody.contains("\"product\":\"Mouse\""));
        assertTrue(secondBody.contains("\"quantity\":5"));
    }

    @Test
    @DisplayName("Test Output Headers Validation")
    public void testOutputHeadersValidation() throws Exception {
        MockEndpoint mockEndpoint = getMockEndpoint("mock:transformedOrder");
        mockEndpoint.expectedMessageCount(1);
        mockEndpoint.expectedHeaderReceived("OrderId", "ORD-999");
        mockEndpoint.expectedHeaderReceived("Product", "Monitor");
        mockEndpoint.expectedHeaderReceived("Quantity", 3);

        template.sendBody("direct:orderTransformation", "ORD-999, Monitor, 3");

        mockEndpoint.assertIsSatisfied();

        Exchange receivedExchange = mockEndpoint.getReceivedExchanges().get(0);
        assertEquals("ORD-999", receivedExchange.getIn().getHeader("OrderId", String.class));
        assertEquals("Monitor", receivedExchange.getIn().getHeader("Product", String.class));
        assertEquals(Integer.valueOf(3), receivedExchange.getIn().getHeader("Quantity", Integer.class));
    }
}
