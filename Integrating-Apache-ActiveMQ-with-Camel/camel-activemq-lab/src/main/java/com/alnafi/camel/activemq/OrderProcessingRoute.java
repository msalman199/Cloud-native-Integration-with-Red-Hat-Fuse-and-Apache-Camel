package com.alnafi.camel.activemq;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;

/**
 * Camel route that integrates with ActiveMQ for order processing
 */
public class OrderProcessingRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        configureActiveMQConnection();

        // Route 1: Order Generation Route
        from("timer:orderGenerator?period=5000")
            .routeId("order-generator-route")
            .log("Starting order generation...")
            .bean(OrderGenerator.class, "generateOrder")
            .log("Generated order: ${body}")
            .to("jms:queue:orders.incoming")
            .log("Order sent to ActiveMQ queue: orders.incoming");

        // Route 2: Order Processing Route
        from("jms:queue:orders.incoming")
            .routeId("order-processing-route")
            .log("Received order from queue: ${body}")
            .process(new OrderProcessor())
            .log("Order processed: ${body}")
            .to("jms:queue:orders.processed")
            .log("Processed order sent to queue: orders.processed");

        // Route 3: Order Fulfillment Route
        from("jms:queue:orders.processed")
            .routeId("order-fulfillment-route")
            .log("Fulfilling processed order: ${body}")
            .delay(2000)
            .transform(simple("FULFILLED: ${body}"))
            .log("Order fulfilled: ${body}")
            .to("jms:queue:orders.fulfilled");

        // Route 4: Order Notification Route
        from("jms:queue:orders.fulfilled")
            .routeId("order-notification-route")
            .log("Sending notification for fulfilled order: ${body}")
            .transform(simple("NOTIFICATION: Order completed - ${body}"))
            .log("Notification sent: ${body}");

        // Route 5: Dead Letter Queue Handler
        from("jms:queue:orders.dlq")
            .routeId("dead-letter-handler-route")
            .log("Processing failed order from DLQ: ${body}")
            .log("Failed order headers: ${headers}")
            .transform(simple("FAILED_ORDER_LOGGED: ${body}"));
    }

    private void configureActiveMQConnection() {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
        connectionFactory.setBrokerURL("tcp://localhost:61616");
        connectionFactory.setUserName("admin");
        connectionFactory.setPassword("admin");

        PooledConnectionFactory pooledConnectionFactory = new PooledConnectionFactory();
        pooledConnectionFactory.setConnectionFactory(connectionFactory);
        pooledConnectionFactory.setMaxConnections(10);

        JmsComponent jmsComponent = new JmsComponent();
        jmsComponent.setConnectionFactory(pooledConnectionFactory);

        getContext().addComponent("jms", jmsComponent);
    }
}
