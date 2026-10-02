package com.alnafi.integration.routes;

import com.alnafi.integration.model.Customer;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

public class CustomerIntegrationRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        onException(Exception.class)
            .handled(true)
            .log("Error processing customer request: ${exception.message}")
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
            .setBody(constant("{\"error\": \"Internal server error\"}"))
            .to("direct:logError");

        rest("/customers")
            .consumes("application/json")
            .produces("application/json")

            .get()
                .description("Get all customers")
                .to("direct:getAllCustomers")

            .get("/{id}")
                .description("Get customer by ID")
                .to("direct:getCustomerById")

            .post()
                .description("Create new customer")
                .type(Customer.class)
                .to("direct:createCustomer");

        from("direct:getAllCustomers")
            .routeId("getAllCustomers")
            .log("Fetching all customers")
            .setBody(simple("SELECT * FROM customers ORDER BY created_at DESC"))
            .to("jdbc:dataSource")
            .marshal().json(JsonLibrary.Jackson)
            .to("direct:logSuccess");

        from("direct:getCustomerById")
            .routeId("getCustomerById")
            .log("Fetching customer with ID: ${header.id}")
            .setBody(simple("SELECT * FROM customers WHERE id = ${header.id}"))
            .to("jdbc:dataSource")
            .choice()
                .when(simple("${body.size} == 0"))
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
                    .setBody(constant("{\"error\": \"Customer not found\"}"))
                .otherwise()
                    .marshal().json(JsonLibrary.Jackson)
                    .to("direct:logSuccess")
            .end();

        from("direct:createCustomer")
            .routeId("createCustomer")
            .log("Creating new customer: ${body}")
            .validate(simple("${body.name} != null && ${body.email} != null"))
            .setProperty("customerData", body())
            .setBody(simple("INSERT INTO customers (name, email, phone) VALUES "
                + "('${exchangeProperty.customerData.name}', '${exchangeProperty.customerData.email}', "
                + "'${exchangeProperty.customerData.phone}') RETURNING *"))
            .to("jdbc:dataSource")
            .marshal().json(JsonLibrary.Jackson)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201))
            .log("Customer created successfully")
            .to("jms:queue:customer.created")
            .to("direct:logSuccess");

        from("jms:queue:customer.created")
            .routeId("customerCreatedProcessor")
            .log("Processing customer created event: ${body}")
            .to("direct:notifyExternalSystem")
            .to("direct:logSuccess");
    }
}
