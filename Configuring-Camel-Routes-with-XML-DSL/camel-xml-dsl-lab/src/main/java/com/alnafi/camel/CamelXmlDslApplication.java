package com.alnafi.camel;

import org.apache.camel.CamelContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class CamelXmlDslApplication {

    public static void main(String[] args) throws Exception {
        System.out.println("Starting Camel XML DSL Application...");

        ApplicationContext springContext =
            new ClassPathXmlApplicationContext("camel-context.xml");

        CamelContext camelContext = springContext.getBean("camelContext", CamelContext.class);

        camelContext.start();

        System.out.println("Camel Context started successfully!");
        System.out.println("Routes configured:");
        camelContext.getRoutes().forEach(route ->
            System.out.println("- " + route.getId() + " -> " + route.getEndpoint().getEndpointUri()));

        System.out.println("\nApplication will run for 30 seconds to process files, then shut down.");
        Thread.sleep(30000);

        System.out.println("Shutting down Camel context...");
        camelContext.stop();
        System.out.println("Application stopped.");
    }
}
