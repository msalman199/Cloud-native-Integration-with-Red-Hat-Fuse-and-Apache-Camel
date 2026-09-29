package com.alnafi.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.ServiceStatus;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;

public class RouteValidator {

    public static void main(String[] args) throws Exception {
        System.out.println("=== Camel Route Validation Utility ===\n");

        ApplicationContext springContext =
            new ClassPathXmlApplicationContext("camel-context.xml");

        CamelContext camelContext = springContext.getBean("camelContext", CamelContext.class);
        camelContext.start();

        List<Route> routes = camelContext.getRoutes();
        System.out.println("Total routes configured: " + routes.size());

        for (Route route : routes) {
            System.out.println("\nRoute ID: " + route.getId());
            System.out.println("Consumer Endpoint: " + route.getConsumer().getEndpoint().getEndpointUri());
            ServiceStatus status = camelContext.getRouteController().getRouteStatus(route.getId());
            System.out.println("Status: " + status);
            if (status == ServiceStatus.Started) {
                System.out.println("Validation: PASSED - route is active");
            } else {
                System.out.println("Validation: FAILED - route is not active");
            }
        }

        camelContext.stop();
        System.out.println("\nValidation completed.");
    }
}
