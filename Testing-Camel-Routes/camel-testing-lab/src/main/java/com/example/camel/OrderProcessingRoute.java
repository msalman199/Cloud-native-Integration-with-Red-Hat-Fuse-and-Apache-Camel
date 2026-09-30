package com.example.camel;

import org.apache.camel.builder.RouteBuilder;

public class OrderProcessingRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("direct:processOrder")
            .routeId("order-processing-route")
            .log("Processing order: ${body}")
            .choice()
                .when(simple("${body} contains 'PRIORITY'"))
                    .log("Priority order detected")
                    .setHeader("OrderType", constant("PRIORITY"))
                    .to("direct:priorityQueue")
                .otherwise()
                    .log("Standard order processing")
                    .setHeader("OrderType", constant("STANDARD"))
                    .to("direct:standardQueue")
            .end();

        from("direct:priorityQueue")
            .routeId("priority-queue-route")
            .log("Handling priority order: ${body}")
            .transform(simple("PRIORITY: ${body}"))
            .to("mock:priorityResult");

        from("direct:standardQueue")
            .routeId("standard-queue-route")
            .log("Handling standard order: ${body}")
            .transform(simple("STANDARD: ${body}"))
            .to("mock:standardResult");
    }
}
