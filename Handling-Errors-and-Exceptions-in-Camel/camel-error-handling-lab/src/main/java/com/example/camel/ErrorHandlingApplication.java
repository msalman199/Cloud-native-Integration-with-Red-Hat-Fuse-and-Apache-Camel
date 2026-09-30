package com.example.camel;

import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorHandlingApplication {
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandlingApplication.class);

    public static void main(String[] args) throws Exception {
        Main main = new Main();

        main.configure().addRoutesBuilder(new DeadLetterChannelRoute());

        logger.info("Starting Camel Error Handling Application...");
        logger.info("Monitoring directories:");
        logger.info("  - Input: input/");
        logger.info("  - Success: output/success/");
        logger.info("  - Business errors: output/business-errors/");
        logger.info("  - Dead letter: output/dead-letter/");

        main.run(args);
    }
}
