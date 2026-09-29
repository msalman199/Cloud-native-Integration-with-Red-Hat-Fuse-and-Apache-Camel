package com.alnafi.eip.routes;

import com.alnafi.eip.model.Order;
import com.alnafi.eip.processors.OrderAggregationStrategy;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AggregatorRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // Entry point: extract the customer type as a correlation header, then aggregate
        from("direct:processOrderForAggregation")
            .routeId("order-aggregation-sender")
            .log("Sending order for aggregation: ${body}")
            .setHeader("customerId", jsonpath("$.customerType"))
            .to("direct:aggregateOrders");

        // Aggregator: groups messages sharing the same customerId header
        from("direct:aggregateOrders")
            .routeId("order-aggregator")
            .log("Order received for aggregation: ${body}")
            .aggregate(header("customerId"), new OrderAggregationStrategy())
                .completionSize(3)
                .completionTimeout(10000)
                .log("Aggregation completed for customer type: ${header.customerId}")
                .log("Aggregated ${header.aggregatedCount} orders with total amount: ${header.totalAmount}")
                .process(exchange -> {
                    @SuppressWarnings("unchecked")
                    List<Order> orders = exchange.getIn().getBody(List.class);
                    exchange.getIn().setHeader("orderCount", orders.size());

                    StringBuilder summary = new StringBuilder();
                    summary.append("Aggregated Order Summary\n");
                    summary.append("=========================\n");
                    summary.append("Customer Type: ").append(exchange.getIn().getHeader("customerId")).append("\n");
                    summary.append("Total Orders: ").append(orders.size()).append("\n");
                    summary.append("Total Amount: ").append(exchange.getIn().getHeader("totalAmount")).append("\n");
                    summary.append("Orders:\n");

                    for (Order order : orders) {
                        summary.append("  - ").append(order.toString()).append("\n");
                    }

                    exchange.getIn().setBody(summary.toString());
                })
                .log("Final aggregated result:\n${body}")
                .to("direct:persistAggregatedOrders");

        // Persistence: after aggregation completes, write the summary to a file
        // so the result can be verified outside the application logs
        from("direct:persistAggregatedOrders")
            .routeId("aggregated-order-persister")
            .setHeader("CamelFileName", simple("${header.customerId}-${date:now:yyyyMMdd-HHmmssSSS}.txt"))
            .to("file:data/aggregated-orders")
            .log("Aggregated result written to data/aggregated-orders/${header.CamelFileName}");
    }
}
