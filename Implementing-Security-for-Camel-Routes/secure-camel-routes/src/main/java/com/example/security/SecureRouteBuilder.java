package com.example.security;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SecureRouteBuilder extends RouteBuilder {

    @Autowired
    private EncryptionService encryptionService;

    @Override
    public void configure() throws Exception {

        onException(Exception.class)
            .handled(true)
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
            .setBody(constant("{\"error\": \"Request could not be processed\"}"));

        restConfiguration()
            .component("servlet")
            .bindingMode(RestBindingMode.json)
            .dataFormatProperty("prettyPrint", "true");

        rest("/api/public")
            .get("/health")
                .to("direct:health-check");

        rest("/api/user")
            .get("/profile")
                .to("direct:user-profile")
            .post("/encrypt")
                .consumes("text/plain")
                .to("direct:encrypt-data");

        rest("/api/admin")
            .get("/users")
                .to("direct:admin-users")
            .post("/decrypt")
                .consumes("text/plain")
                .to("direct:decrypt-data");

        from("direct:health-check")
            .setBody(constant("{\"status\": \"UP\"}"))
            .log("Health check requested");

        from("direct:user-profile")
            .setBody(constant("{\"username\": \"user\", \"role\": \"USER\"}"))
            .log("User profile requested");

        from("direct:admin-users")
            .setBody(constant("{\"users\": [\"admin\", \"user\"]}"))
            .log("Admin users list requested");

        from("direct:encrypt-data")
            .process(exchange -> {
                String inputData = exchange.getIn().getBody(String.class);
                String encryptedData = encryptionService.encrypt(inputData);
                exchange.getIn().setBody("{\"encryptedData\": \"" + encryptedData + "\"}");
            })
            .log("Data encrypted successfully");

        from("direct:decrypt-data")
            .process(exchange -> {
                String encryptedData = exchange.getIn().getBody(String.class);
                String decryptedData = encryptionService.decrypt(encryptedData);
                exchange.getIn().setBody("{\"decryptedData\": \"" + decryptedData + "\"}");
            })
            .log("Data decrypted successfully");
    }
}
