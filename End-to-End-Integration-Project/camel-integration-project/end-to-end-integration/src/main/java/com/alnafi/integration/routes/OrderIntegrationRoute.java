package com.alnafi.integration.routes;

import com.alnafi.integration.model.Order;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

public class OrderIntegrationRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        onException(Exception.class)
            .handled(true)
            .log("Error processing order request: ${exception.message}")
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
            .setBody(constant("{\"error\": \"Internal server error\"}"))
            .to("direct:logError");

        rest("/orders")
            .consumes("application/json")
            .produces("application/json")

            .get()
                .description("Get all orders")
                .to("direct:getAllOrders")

            .get("/{id}")
                .description("Get order by ID")
                .to("direct:getOrderById")

            .post()
                .description("Create new order")
                .type(Order.class)
                .to("direct:createOrder")

            .put("/{id}/status")
                .description("Update order status")
                .to("direct:updateOrderStatus");

        from("direct:getAllOrders")
            .routeId("getAllOrders")
            .log("Fetching all orders")
            .setBody(simple("SELECT o.*, c.name as customer_name FROM orders o "
                + "LEFT JOIN customers c ON o.customer_id = c.id ORDER BY o.created_at DESC"))
            .to("jdbc:dataSource")
            .marshal().json(JsonLibrary.Jackson)
            .to("direct:logSuccess");

        from("direct:getOrderById")
            .routeId("getOrderById")
            .log("Fetching order with ID: ${header.id}")
            .setBody(simple("SELECT o.*, c.name as customer_name FROM orders o "
                + "LEFT JOIN customers c ON o.customer_id = c.id WHERE o.id = ${header.id}"))
            .to("jdbc:dataSource")
            .choice()
                .when(simple("${body.size} == 0"))
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
                    .setBody(constant("{\"error\": \"Order not found\"}"))
                .otherwise()
                    .marshal().json(JsonLibrary.Jackson)
                    .to("direct:logSuccess")
            .end();

        from("direct:createOrder")
            .routeId("createOrder")
            .log("Creating new order: ${body}")
            .validate(simple("${body.customerId} != null && ${body.totalAmount} != null"))
            .setProperty("orderData", body())
            .setBody(simple("INSERT INTO orders (customer_id, order_number, total_amount, status) VALUES "
                + "(${exchangeProperty.orderData.customerId}, '${exchangeProperty.orderData.orderNumber}', "
                + "${exchangeProperty.orderData.totalAmount}, '${exchangeProperty.orderData.status}') RETURNING *"))
            .to("jdbc:dataSource")
            .marshal().json(JsonLibrary.Jackson)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(201))
            .log("Order created successfully")
            .to("jms:queue:order.created")
            .to("direct:logSuccess");

        from("direct:updateOrderStatus")
            .routeId("updateOrderStatus")
            .log("Updating order status for ID: ${header.id}")
            .setProperty("newStatus", jsonpath("$.status"))
            .setBody(simple("UPDATE orders SET status='${exchangeProperty.newStatus}', "
                + "updated_at=CURRENT_TIMESTAMP WHERE id=${header.id} RETURNING *"))
            .to("jdbc:dataSource")
            .choice()
                .when(simple("${body.size} == 0"))
                    .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
                    .setBody(constant("{\"error\": \"Order not found\"}"))
                .otherwise()
                    .marshal().json(JsonLibrary.Jackson)
                    .log("Order status updated successfully")
                    .to("jms:queue:order.status.updated")
                    .to("direct:logSuccess")
            .end();

        // Full order processing workflow, triggered after order creation
        from("jms:queue:order.created")
            .routeId("orderCreatedProcessor")
            .log("Processing new order: ${body}")
            .to("direct:processPayment")
            .to("direct:updateInventory")
            .to("direct:notifyExternalSystem")
            .to("direct:logSuccess");

        from("jms:queue:order.status.updated")
            .routeId("orderStatusProcessor")
            .log("Processing order status update: ${body}")
            .choice()
                .when(jsonpath("$[0].status == 'COMPLETED'"))
                    .to("direct:sendOrderConfirmation")
                .when(jsonpath("$[0].status == 'CANCELLED'"))
                    .to("direct:processRefund")
            .end()
            .to("direct:logSuccess");

        // Simulated payment authorization step with a short processing delay
        from("direct:processPayment")
            .routeId("processPayment")
            .log("Authorizing payment for order")
            .delay(1000)
            .setBody(simple("Payment authorized for order"))
            .log("Payment processed successfully");

        // Simulated inventory decrement step
        from("direct:updateInventory")
            .routeId("updateInventory")
            .log("Reserving inventory for order")
            .delay(500)
            .setBody(simple("Inventory reserved for order"))
            .log("Inventory updated successfully");

        // Notifies the external partner system via the mock API
        from("direct:notifyExternalSystem")
            .routeId("notifyExternalSystem")
            .log("Notifying external system")
            .setHeader(Exchange.HTTP_METHOD, constant("GET"))
            .toD("{{external.api.base-url}}/external/status")
            .log("External system responded: ${body}");

        // Sends an order confirmation once status becomes COMPLETED
        from("direct:sendOrderConfirmation")
            .routeId("sendOrderConfirmation")
            .log("Sending order confirmation to customer")
            .setBody(simple("Order confirmation sent"))
            .log("Order confirmation processed");

        // Handles refund processing once status becomes CANCELLED
        from("direct:processRefund")
            .routeId("processRefund")
            .log("Processing refund for cancelled order")
            .delay(500)
            .setBody(simple("Refund processed for order"))
            .log("Refund processing complete");
    }
}
