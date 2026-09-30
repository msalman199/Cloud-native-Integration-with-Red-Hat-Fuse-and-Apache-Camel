package com.example.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class InputSimulationTest extends CamelTestSupport {

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

                from("direct:validateCustomer")
                    .routeId("customer-validation-route")
                    .log("Validating customer: ${body}")
                    .choice()
                        .when(header("customerId").isNull())
                            .setHeader("ValidationResult", constant("FAILED"))
                            .setHeader("ErrorMessage", constant("Customer ID is required"))
                            .to("mock:validationFailed")
                        .when(simple("${header.customerId} < 1000"))
                            .setHeader("ValidationResult", constant("FAILED"))
                            .setHeader("ErrorMessage", constant("Invalid customer ID"))
                            .to("mock:validationFailed")
                        .otherwise()
                            .setHeader("ValidationResult", constant("SUCCESS"))
                            .process(exchange -> {
                                String customerId = exchange.getIn().getHeader("customerId", String.class);
                                exchange.getIn().setBody("Customer " + customerId + " validated successfully");
                            })
                            .to("mock:validationSuccess")
                    .end();
            }
        };
    }

    @Test
    @DisplayName("Test Valid Customer Input")
    public void testValidCustomerInput() throws Exception {
        MockEndpoint successEndpoint = getMockEndpoint("mock:validationSuccess");
        MockEndpoint failedEndpoint = getMockEndpoint("mock:validationFailed");

        successEndpoint.expectedMessageCount(1);
        successEndpoint.expectedHeaderReceived("ValidationResult", "SUCCESS");
        failedEndpoint.expectedMessageCount(0);

        Map<String, Object> headers = new HashMap<>();
        headers.put("customerId", "12345");

        template.sendBodyAndHeaders("direct:validateCustomer", "John Doe", headers);

        successEndpoint.assertIsSatisfied();
        failedEndpoint.assertIsSatisfied();
    }

    @Test
    @DisplayName("Test Invalid Customer Input - Missing ID")
    public void testInvalidCustomerInputMissingId() throws Exception {
        MockEndpoint successEndpoint = getMockEndpoint("mock:validationSuccess");
        MockEndpoint failedEndpoint = getMockEndpoint("mock:validationFailed");

        successEndpoint.expectedMessageCount(0);
        failedEndpoint.expectedMessageCount(1);
        failedEndpoint.expectedHeaderReceived("ValidationResult", "FAILED");
        failedEndpoint.expectedHeaderReceived("ErrorMessage", "Customer ID is required");

        template.sendBody("direct:validateCustomer", "Jane Doe");

        successEndpoint.assertIsSatisfied();
        failedEndpoint.assertIsSatisfied();
    }

    @Test
    @DisplayName("Test Invalid Customer Input - Invalid ID")
    public void testInvalidCustomerInputInvalidId() throws Exception {
        MockEndpoint successEndpoint = getMockEndpoint("mock:validationSuccess");
        MockEndpoint failedEndpoint = getMockEndpoint("mock:validationFailed");

        successEndpoint.expectedMessageCount(0);
        failedEndpoint.expectedMessageCount(1);
        failedEndpoint.expectedHeaderReceived("ValidationResult", "FAILED");
        failedEndpoint.expectedHeaderReceived("ErrorMessage", "Invalid customer ID");

        Map<String, Object> headers = new HashMap<>();
        headers.put("customerId", "500");

        template.sendBodyAndHeaders("direct:validateCustomer", "Bob Smith", headers);

        successEndpoint.assertIsSatisfied();
        failedEndpoint.assertIsSatisfied();
    }
}
