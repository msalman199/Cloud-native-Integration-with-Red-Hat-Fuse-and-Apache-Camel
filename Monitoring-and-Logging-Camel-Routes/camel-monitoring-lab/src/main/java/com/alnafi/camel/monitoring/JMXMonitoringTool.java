package com.alnafi.camel.monitoring;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.Set;

public class JMXMonitoringTool {

    public static void main(String[] args) throws Exception {
        String jmxUrl = "service:jmx:rmi:///jndi/rmi://localhost:1099/jmxrmi";

        System.out.println("Connecting to JMX endpoint: " + jmxUrl);

        JMXServiceURL url = new JMXServiceURL(jmxUrl);
        try (JMXConnector connector = JMXConnectorFactory.connect(url)) {
            MBeanServerConnection server = connector.getMBeanServerConnection();
            monitorCamelRoutes(server);
        } catch (Exception e) {
            System.out.println("Connection failed. Make sure the JMXMonitoringApplication is running");
            System.out.println("with JMX enabled on port 1099. Error: " + e.getMessage());
        }
    }

    private static void monitorCamelRoutes(MBeanServerConnection server) throws Exception {
        System.out.println("=== Camel Route Monitoring ===");

        // Monitor Camel Context
        ObjectName camelContextPattern = new ObjectName("org.apache.camel:context=*,type=context,name=*");
        Set<ObjectName> contexts = server.queryNames(camelContextPattern, null);

        System.out.println("\n--- Camel Contexts ---");
        for (ObjectName context : contexts) {
            String contextId = (String) server.getAttribute(context, "CamelId");
            String state = (String) server.getAttribute(context, "State");
            Long totalExchanges = (Long) server.getAttribute(context, "ExchangesTotal");
            Long completedExchanges = (Long) server.getAttribute(context, "ExchangesCompleted");
            Long failedExchanges = (Long) server.getAttribute(context, "ExchangesFailed");

            System.out.printf("Context: %s, State: %s%n", contextId, state);
            System.out.printf("  Total: %d, Completed: %d, Failed: %d%n",
                    totalExchanges, completedExchanges, failedExchanges);
        }

        // Monitor individual routes
        ObjectName routePattern = new ObjectName("org.apache.camel:context=*,type=routes,name=*");
        Set<ObjectName> routes = server.queryNames(routePattern, null);

        System.out.println("\n--- Camel Routes ---");
        for (ObjectName route : routes) {
            String routeId = (String) server.getAttribute(route, "RouteId");
            String state = (String) server.getAttribute(route, "State");
            Long exchangesCompleted = (Long) server.getAttribute(route, "ExchangesCompleted");
            Long exchangesFailed = (Long) server.getAttribute(route, "ExchangesFailed");
            Long meanProcessingTime = (Long) server.getAttribute(route, "MeanProcessingTime");

            System.out.printf("RouteId: %s%n", routeId);
            System.out.printf("  State: %s%n", state);
            System.out.printf("  ExchangesCompleted: %d%n", exchangesCompleted);
            System.out.printf("  ExchangesFailed: %d%n", exchangesFailed);
            System.out.printf("  MeanProcessingTime: %d ms%n", meanProcessingTime);
            System.out.println();
        }

        System.out.println("=== Monitoring snapshot complete ===");
    }
}
