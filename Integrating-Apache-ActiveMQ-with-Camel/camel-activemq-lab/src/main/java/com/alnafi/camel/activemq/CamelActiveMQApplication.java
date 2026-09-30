package com.alnafi.camel.activemq;

import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application class for Camel ActiveMQ integration
 */
public class CamelActiveMQApplication {

    private static final Logger logger = LoggerFactory.getLogger(CamelActiveMQApplication.class);

    public static void main(String[] args) throws Exception {
        logger.info("Starting Camel ActiveMQ Integration Application...");

        Main main = new Main();

        main.addRouteBuilder(new OrderProcessingRoute());
        main.addRouteBuilder(new ErrorHandlingRoute());

        main.configure().setDurationMaxMessages(100);
        main.configure().setDurationMaxSeconds(300);

        logger.info("Camel context starting...");
        main.run(args);

        logger.info("Camel ActiveMQ Integration Application stopped.");
    }
}
