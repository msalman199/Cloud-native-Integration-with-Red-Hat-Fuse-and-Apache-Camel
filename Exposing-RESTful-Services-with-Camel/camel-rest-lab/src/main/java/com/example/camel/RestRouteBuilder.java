package com.example.camel;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;

public class RestRouteBuilder extends RouteBuilder {
    
    @Override
    public void configure() throws Exception {
        
        // Configure REST configuration
        restConfiguration()
            .component("jetty")
            .host("0.0.0.0")
            .port(8080)
            .bindingMode(RestBindingMode.json)
            .dataFormatProperty("prettyPrint", "true")
            .enableCORS(true)
            .corsHeaderProperty("Access-Control-Allow-Origin", "*")
            .corsHeaderProperty("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS")
            .corsHeaderProperty("Access-Control-Allow-Headers", "Content-Type, Authorization");
        
        // Exception handling
        onException(Exception.class)
            .handled(true)
            .setHeader("Content-Type", constant("application/json"))
            .setBody(constant("{\"error\":\"Internal server error\"}"))
            .setHeader("CamelHttpResponseCode", constant(500));
        
        // REST DSL definition
        rest("/api/users")
            .description("User REST service")
            .consumes("application/json")
            .produces("application/json")
            
            // GET all users
            .get()
                .description("Get all users")
                .responseMessage().code(200).message("All users retrieved successfully").endResponseMessage()
                .to("direct:getAllUsers")
            
            // GET user by ID
            .get("/{id}")
                .description("Get user by ID")
                .param().name("id").type(RestParamType.path).description("User ID").dataType("integer").endParam()
                .responseMessage().code(200).message("User found").endResponseMessage()
                .responseMessage().code(404).message("User not found").endResponseMessage()
                .to("direct:getUserById")
            
            // POST create new user
            .post()
                .description("Create new user")
                .type(User.class)
                .responseMessage().code(201).message("User created successfully").endResponseMessage()
                .responseMessage().code(400).message("Invalid user data").endResponseMessage()
                .to("direct:createUser")
            
            // PUT update user
            .put("/{id}")
                .description("Update user")
                .param().name("id").type(RestParamType.path).description("User ID").dataType("integer").endParam()
                .type(User.class)
                .responseMessage().code(200).message("User updated successfully").endResponseMessage()
                .responseMessage().code(404).message("User not found").endResponseMessage()
                .responseMessage().code(400).message("Invalid user data").endResponseMessage()
                .to("direct:updateUser")
            
            // DELETE user
            .delete("/{id}")
                .description("Delete user")
                .param().name("id").type(RestParamType.path).description("User ID").dataType("integer").endParam()
                .responseMessage().code(200).message("User deleted successfully").endResponseMessage()
                .responseMessage().code(404).message("User not found").endResponseMessage()
                .to("direct:deleteUser");
        
        // Route implementations
        from("direct:getAllUsers")
            .log("Getting all users")
            .bean(UserService.class, "getAllUsers")
            .setHeader("Content-Type", constant("application/json"));
        
        from("direct:getUserById")
            .log("Getting user by ID: ${header.id}")
            .bean(UserService.class, "getUserById")
            .choice()
                .when(body().isNull())
                    .setHeader("CamelHttpResponseCode", constant(404))
                    .setBody(constant("{\"error\":\"User not found\"}"))
                .otherwise()
                    .setHeader("CamelHttpResponseCode", constant(200))
            .end()
            .setHeader("Content-Type", constant("application/json"));
        
        from("direct:createUser")
            .log("Creating new user: ${body}")
            .bean(UserService.class, "createUser")
            .setHeader("Content-Type", constant("application/json"));
        
        from("direct:updateUser")
            .log("Updating user ID: ${header.id}")
            .bean(UserService.class, "updateUser")
            .choice()
                .when(body().isNull())
                    .setBody(constant("{\"error\":\"User not found or invalid data\"}"))
                .otherwise()
                    .log("User updated successfully")
            .end()
            .setHeader("Content-Type", constant("application/json"));
        
        from("direct:deleteUser")
            .log("Deleting user ID: ${header.id}")
            .bean(UserService.class, "deleteUser")
            .setHeader("Content-Type", constant("application/json"));
    }
}
