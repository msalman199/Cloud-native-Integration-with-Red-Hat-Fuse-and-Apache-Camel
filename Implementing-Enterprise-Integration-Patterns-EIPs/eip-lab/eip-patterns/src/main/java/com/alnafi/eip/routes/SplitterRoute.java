package com.alnafi.eip.routes;

import com.alnafi.eip.model.BatchOrder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

@Component
public class SplitterRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("direct:processBatchOrder")
            .routeId("batch-order-splitter")
            .log("Received batch order: ${body}")
            .unmarshal().json(JsonLibrary.Jackson, BatchOrder.class)
            .log("Processing batch: ${body.batchId} with ${body.orders.size()} orders")
            .split(simple("${body.orders}"))
                .streaming()
                .log("Processing individual order from batch: ${body}")
                .marshal().json(JsonLibrary.Jackson)
                .to("direct:processOrder")
                .log("Individual order processed: ${body}")
            .end()
            .log("Batch processing completed");
    }
}
