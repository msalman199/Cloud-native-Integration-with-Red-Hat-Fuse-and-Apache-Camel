package com.alnafi.integration.routes;

import org.apache.camel.builder.RouteBuilder;

public class LoggingRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("direct:logSuccess")
            .routeId("logSuccess")
            .setBody(simple("INSERT INTO integration_logs (route_id, message_id, status, error_message) "
                + "VALUES ('${routeId}', '${exchangeId}', 'SUCCESS', NULL)"))
            .to("jdbc:dataSource")
            .log("Logged success for route ${exchangeProperty.CamelToEndpoint}");

        from("direct:logError")
            .routeId("logError")
            .setBody(simple("INSERT INTO integration_logs (route_id, message_id, status, error_message) "
                + "VALUES ('${routeId}', '${exchangeId}', 'ERROR', '${exception.message}')"))
            .to("jdbc:dataSource")
            .log("Logged error for route ${routeId}: ${exception.message}");
    }
}
