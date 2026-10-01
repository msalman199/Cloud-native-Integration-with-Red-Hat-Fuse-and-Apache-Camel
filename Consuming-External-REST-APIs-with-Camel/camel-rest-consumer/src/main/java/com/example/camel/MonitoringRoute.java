package com.example.camel;

import org.apache.camel.builder.RouteBuilder;

public class MonitoringRoute extends RouteBuilder {
    
    @Override
    public void configure() throws Exception {
        
        // Health check route
        from("timer://healthCheck?period=60000")
            .routeId("health-check")
            .log("Performing health check...")
            .to("https://jsonplaceholder.typicode.com/posts/1")
            .choice()
                .when(header("CamelHttpResponseCode").isEqualTo(200))
                    .log("✓ API Health Check: PASSED")
                    .setBody(constant("API Status: HEALTHY\n"))
                .otherwise()
                    .log("✗ API Health Check: FAILED")
                    .setBody(constant("API Status: UNHEALTHY\n"))
            .end()
            .setBody(simple("${body}Timestamp: ${date:now:yyyy-MM-dd HH:mm:ss}\n"))
            .to("file:src/main/resources/output?fileName=health-check.log&fileExist=Append");
            
        // Performance monitoring
        from("timer://performanceMonitor?period=30000")
            .routeId("performance-monitor")
            .log("Monitoring API performance...")
            .setHeader("startTime", simple("${date:now:currentTimeMillis}"))
            .to("https://jsonplaceholder.typicode.com/posts?_limit=1")
            .setHeader("endTime", simple("${date:now:currentTimeMillis}"))
            .setHeader("responseTime", simple("${header.endTime} - ${header.startTime}"))
            .log("API Response Time: ${header.responseTime}ms")
            .setBody(simple("Response Time: ${header.responseTime}ms at ${date:now:yyyy-MM-dd HH:mm:ss}\n"))
            .to("file:src/main/resources/output?fileName=performance.log&fileExist=Append");
    }
}
