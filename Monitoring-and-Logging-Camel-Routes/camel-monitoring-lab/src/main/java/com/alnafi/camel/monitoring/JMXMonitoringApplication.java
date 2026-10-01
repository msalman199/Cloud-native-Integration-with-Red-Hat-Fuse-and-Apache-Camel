package com.alnafi.camel.monitoring;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Set;

public class JMXMonitoringApplication {

    private static final Logger logger = LoggerFactory.getLogger(JMXMonitoringApplication.class);

    public static void main(String[] args) throws Exception {
        logger.info("Starting Camel JMX Monitoring Application...");

        Main main = new Main();

        // Configure JMX settings: enable local platform MBean registration
        // and expose a remote JMX RMI connector on port 1099 for external tools.
        main.configure().setJmxEnabled(true);
        main.configure().setJmxCreateConnector(true);
        main.configure().setJmxConnectorPort(1099);

        main.addRouteBuilder(new MonitoredFileRoute());
        main.addRouteBuilder(new PerformanceTestRoute());
        main.addRouteBuilder(new JMXMetricsRoute());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                logger.info("Shutting down Camel application...");
                main.stop();
            } catch (Exception e) {
                logger.error("Error during shutdown", e);
            }
        }));

        main.run(args);
    }

    static class MonitoredFileRoute extends RouteBuilder {
        private static final Logger routeLogger = LoggerFactory.getLogger(MonitoredFileRoute.class);

        @Override
        public void configure() throws Exception {

            from("timer://setupDirs?period=1000&repeatCount=1")
                .process(exchange -> {
                    java.io.File[] dirs = {
                        new java.io.File("data/input"),
                        new java.io.File("data/output"),
                        new java.io.File("data/processed"),
                        new java.io.File("data/error")
                    };

                    for (java.io.File dir : dirs) {
                        dir.mkdirs();
                    }
                    routeLogger.info("Setup directories completed");
                });

            from("file://data/input?delay=3000&move=processed")
                .routeId("monitored-file-route")
                .log("Starting file processing: ${header.CamelFileName}")
                .process(new TimedProcessor())
                .choice()
                    .when(header("processingTime").isGreaterThan(300))
                        .log("SLOW processing detected: ${header.CamelFileName} took ${header.processingTime}ms")
                        .to("file://data/output?fileName=slow-${header.CamelFileName}")
                    .otherwise()
                        .log("FAST processing: ${header.CamelFileName} took ${header.processingTime}ms")
                        .to("file://data/output?fileName=fast-${header.CamelFileName}")
                .end()
                .log("Completed file processing: ${header.CamelFileName}");
        }
    }

    static class PerformanceTestRoute extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            from("timer://performanceTest?period=8000")
                .routeId("performance-test-route")
                .process(exchange -> {
                    StringBuilder content = new StringBuilder();
                    int size = 100 + (int) (Math.random() * 2000);

                    for (int i = 0; i < size; i++) {
                        content.append((char) ('A' + (i % 26)));
                    }

                    String fileName = "perf-test-" + System.currentTimeMillis() + ".txt";
                    exchange.getIn().setBody(content.toString());
                    exchange.getIn().setHeader(Exchange.FILE_NAME, fileName);
                })
                .to("file://data/input");
        }
    }

    static class JMXMetricsRoute extends RouteBuilder {
        @Override
        public void configure() throws Exception {
            from("timer://jmxMetrics?period=15000&delay=10000")
                .routeId("jmx-metrics-route")
                .process(new JMXMetricsProcessor());
        }
    }

    static class TimedProcessor implements Processor {
        private static final Logger processorLogger = LoggerFactory.getLogger(TimedProcessor.class);

        @Override
        public void process(Exchange exchange) throws Exception {
            long startTime = System.currentTimeMillis();

            String fileName = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
            String body = exchange.getIn().getBody(String.class);

            int processingDelay = 50 + (int) (Math.random() * 400);
            Thread.sleep(processingDelay);

            String processedContent = "PROCESSED AT: " + new java.util.Date() + "\n" +
                    "ORIGINAL SIZE: " + (body != null ? body.length() : 0) + " bytes\n" +
                    "CONTENT:\n" + body;

            exchange.getIn().setBody(processedContent);

            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            exchange.getIn().setHeader("processingTime", processingTime);

            processorLogger.info("Processed {} in {}ms", fileName, processingTime);
        }
    }

    static class JMXMetricsProcessor implements Processor {
        private static final Logger metricsLogger = LoggerFactory.getLogger("jmx.metrics");

        @Override
        public void process(Exchange exchange) throws Exception {
            try {
                MBeanServerConnection server = ManagementFactory.getPlatformMBeanServer();

                ObjectName camelContextName = new ObjectName("org.apache.camel:context=*,type=context,name=*");
                Set<ObjectName> contextNames = server.queryNames(camelContextName, null);

                for (ObjectName contextName : contextNames) {
                    String contextId = (String) server.getAttribute(contextName, "CamelId");
                    Long exchangesCompleted = (Long) server.getAttribute(contextName, "ExchangesCompleted");
                    Long exchangesFailed = (Long) server.getAttribute(contextName, "ExchangesFailed");
                    Long exchangesTotal = (Long) server.getAttribute(contextName, "ExchangesTotal");

                    metricsLogger.info("Context: {} - Completed: {}, Failed: {}, Total: {}",
                            contextId, exchangesCompleted, exchangesFailed, exchangesTotal);
                }

                ObjectName routeName = new ObjectName("org.apache.camel:context=*,type=routes,name=*");
                Set<ObjectName> routeNames = server.queryNames(routeName, null);

                for (ObjectName route : routeNames) {
                    String routeId = (String) server.getAttribute(route, "RouteId");
                    Long exchangesCompleted = (Long) server.getAttribute(route, "ExchangesCompleted");
                    Long exchangesFailed = (Long) server.getAttribute(route, "ExchangesFailed");
                    String state = (String) server.getAttribute(route, "State");

                    if (exchangesCompleted > 0 || exchangesFailed > 0) {
                        metricsLogger.info("Route: {} - State: {}, Completed: {}, Failed: {}",
                                routeId, state, exchangesCompleted, exchangesFailed);
                    }
                }

            } catch (Exception e) {
                metricsLogger.error("Error collecting JMX metrics", e);
            }
        }
    }
}
