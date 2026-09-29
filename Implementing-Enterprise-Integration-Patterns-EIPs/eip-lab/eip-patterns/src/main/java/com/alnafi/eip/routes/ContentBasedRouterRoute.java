package com.alnafi.eip.routes;

import com.alnafi.eip.model.Order;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

@Component
public class ContentBasedRouterRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("direct:processOrder")
            .routeId("content-based-router")
            .log("Received order: ${body}")
            .unmarshal().json(JsonLibrary.Jackson, Order.class)
            .choice()
                .when(simple("${body.customerType} == 'PREMIUM'"))
                    .log("Routing PREMIUM customer order: ${body.orderId}")
                    .to("direct:premiumProcessing")
                .when(simple("${body.customerType} == 'STANDARD'"))
                    .log("Routing STANDARD customer order: ${body.orderId}")
                    .to("direct:standardProcessing")
                .when(simple("${body.amount} > 1000"))
                    .log("Routing HIGH-VALUE order: ${body.orderId}")
                    .to("direct:highValueProcessing")
                .otherwise()
                    .log("Routing to REGULAR processing: ${body.orderId}")
                    .to("direct:regularProcessing")
            .end();

        from("direct:premiumProcessing")
            .routeId("premium-processing")
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                order.setPriority("HIGH");
                exchange.getIn().setBody(order);
            })
            .log("Premium order processed: ${body}")
            .to("direct:orderComplete");

        from("direct:standardProcessing")
            .routeId("standard-processing")
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                order.setPriority("MEDIUM");
                exchange.getIn().setBody(order);
            })
            .log("Standard order processed: ${body}")
            .to("direct:orderComplete");

        from("direct:highValueProcessing")
            .routeId("high-value-processing")
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                order.setPriority("URGENT");
                exchange.getIn().setBody(order);
            })
            .log("High-value order processed: ${body}")
            .to("direct:orderComplete");

        from("direct:regularProcessing")
            .routeId("regular-processing")
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                order.setPriority("LOW");
                exchange.getIn().setBody(order);
            })
            .log("Regular order processed: ${body}")
            .to("direct:orderComplete");

        from("direct:orderComplete")
            .routeId("order-completion")
            .log("Order processing completed: ${body}")
            .marshal().json(JsonLibrary.Jackson);
    }
}
