package com.example.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import java.util.concurrent.atomic.AtomicInteger;

public class PerformanceTestRoute extends RouteBuilder {
    
    private final AtomicInteger messageCounter = new AtomicInteger(0);
    
    @Override
    public void configure() throws Exception {
        
        // High-frequency message producer for performance testing
        from("timer://performanceTest?period=100&delay=30000")
            .routeId("performance-test-producer")
            .process(exchange -> {
                int count = messageCounter.incrementAndGet();
                Order perfOrder = new Order("PERF-" + count, "PERF-CUST", "PerfProduct", 1, 10.0);
                exchange.getIn().setBody(perfOrder);
                exchange.getIn().setHeader("kafka.KEY", "perf-key-" + (count % 10));
            })
            .marshal().json(JsonLibrary.Jackson)
            .to("kafka:orders?brokers=localhost:9092")
            .filter(simple("${exchangeProperty.CamelTimerCounter} % 100 == 0"))
            .log("Performance test: sent ${exchangeProperty.CamelTimerCounter} messages");
            
        // Performance monitoring route
        from("timer://performanceMonitor?period=10000&delay=35000")
            .routeId("performance-monitor")
            .process(exchange -> {
                int totalMessages = messageCounter.get();
                log.info("Performance Stats - Total messages generated: {}", totalMessages);
            });
    }
}

