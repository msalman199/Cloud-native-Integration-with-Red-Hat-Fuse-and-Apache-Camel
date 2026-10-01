package com.alnafi.camel.monitoring;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonitoringCamelApplication {

    private static final Logger logger = LoggerFactory.getLogger(MonitoringCamelApplication.class);
    private static final Logger performanceLogger = LoggerFactory.getLogger("route.performance");

    public static void main(String[] args) throws Exception {
        logger.info("Starting Camel Monitoring Application...");

        Main main = new Main();

        main.addRouteBuilder(new FileProcessingRoute());
        main.addRouteBuilder(new TimerRoute());
        main.addRouteBuilder(new ErrorHandlingRoute());

        main.configure().setJmxEnabled(true);

        main.run(args);
    }

    static class FileProcessingRoute extends RouteBuilder {
        private static final Logger routeLogger = LoggerFactory.getLogger(FileProcessingRoute.class);

        @Override
        public void configure() throws Exception {

            from("timer://createDirs?period=1000&repeatCount=1")
                .process(exchange -> {
                    java.io.File inputDir = new java.io.File("data/input");
                    java.io.File outputDir = new java.io.File("data/output");
                    java.io.File errorDir = new java.io.File("data/error");

                    inputDir.mkdirs();
                    outputDir.mkdirs();
                    errorDir.mkdirs();

                    routeLogger.info("Created directories for file processing");
                });

            from("file://data/input?delay=5000&move=processed")
                .routeId("file-processing-route")
                .log("Processing file: ${header.CamelFileName}")
                .process(new FileProcessor())
                .choice()
                    .when(header("fileSize").isGreaterThan(1000))
                        .log("Large file detected: ${header.CamelFileName} (${header.fileSize} bytes)")
                        .to("file://data/output?fileName=large-${header.CamelFileName}")
                    .otherwise()
                        .log("Small file processed: ${header.CamelFileName}")
                        .to("file://data/output?fileName=small-${header.CamelFileName}")
                .end()
                .log("File processing completed for: ${header.CamelFileName}");
        }
    }

    static class TimerRoute extends RouteBuilder {
        private static final Logger timerLogger = LoggerFactory.getLogger(TimerRoute.class);

        @Override
        public void configure() throws Exception {
            from("timer://testDataGenerator?period=15000")
                .routeId("test-data-generator")
                .process(exchange -> {
                    String fileName = "test-file-" + System.currentTimeMillis() + ".txt";
                    String content = "Test data generated at: " + new java.util.Date() + "\n";
                    content += "Random number: " + Math.random() + "\n";

                    exchange.getIn().setBody(content);
                    exchange.getIn().setHeader(Exchange.FILE_NAME, fileName);

                    timerLogger.info("Generated test file: {}", fileName);
                })
                .to("file://data/input");
        }
    }

    static class ErrorHandlingRoute extends RouteBuilder {
        private static final Logger errorLogger = LoggerFactory.getLogger(ErrorHandlingRoute.class);

        @Override
        public void configure() throws Exception {

            errorHandler(deadLetterChannel("file://data/error")
                .maximumRedeliveries(3)
                .redeliveryDelay(2000)
                .logRetryAttempted(true)
                .logStackTrace(true));

            from("timer://errorSimulator?period=20000")
                .routeId("error-simulation-route")
                .process(exchange -> {
                    double random = Math.random();
                    if (random < 0.3) {
                        errorLogger.error("Simulated error occurred with random value: {}", random);
                        throw new RuntimeException("Simulated processing error");
                    }
                    errorLogger.info("Processing completed successfully with random value: {}", random);
                })
                .log("Error simulation route completed successfully");
        }
    }

    static class FileProcessor implements Processor {
        private static final Logger processorLogger = LoggerFactory.getLogger(FileProcessor.class);

        @Override
        public void process(Exchange exchange) throws Exception {
            long startTime = System.currentTimeMillis();

            String fileName = exchange.getIn().getHeader(Exchange.FILE_NAME, String.class);
            String body = exchange.getIn().getBody(String.class);

            processorLogger.debug("Starting to process file: {}", fileName);

            Thread.sleep(100 + (int) (Math.random() * 500));

            int fileSize = body != null ? body.length() : 0;
            exchange.getIn().setHeader("fileSize", fileSize);

            String processedContent = "PROCESSED: " + body + "\nProcessed at: " + new java.util.Date();
            exchange.getIn().setBody(processedContent);

            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            performanceLogger.info("File: {}, Size: {} bytes, Processing Time: {} ms",
                fileName, fileSize, processingTime);

            processorLogger.info("Completed processing file: {} in {} ms", fileName, processingTime);
        }
    }
}
