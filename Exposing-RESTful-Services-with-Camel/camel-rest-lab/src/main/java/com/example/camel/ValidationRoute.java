package com.example.camel;

import org.apache.camel.builder.RouteBuilder;

public class ValidationRoute extends RouteBuilder {
    
    @Override
    public void configure() throws Exception {
        
        // Validation route for user creation
        from("direct:validateUser")
            .choice()
                .when(simple("${body.name} == null || ${body.name} == ''"))
                    .setHeader("CamelHttpResponseCode", constant(400))
                    .setBody(constant("{\"error\":\"Name is required\"}"))
                    .stop()
                .when(simple("${body.email} == null || ${body.email} == ''"))
                    .setHeader("CamelHttpResponseCode", constant(400))
                    .setBody(constant("{\"error\":\"Email is required\"}"))
                    .stop()
                .when(simple("${body.age} == null || ${body.age} < 0"))
                    .setHeader("CamelHttpResponseCode", constant(400))
                    .setBody(constant("{\"error\":\"Valid age is required\"}"))
                    .stop()
            .end()
            .log("User validation passed");
    }
}
