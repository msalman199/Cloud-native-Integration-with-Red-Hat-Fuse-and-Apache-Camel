package com.example.camel;

import com.example.camel.model.Post;
import com.example.camel.model.User;
import com.example.camel.processor.PostProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

public class AdvancedRestConsumerRoute extends RouteBuilder {
    
    @Override
    public void configure() throws Exception {
        
        // Load properties
        getContext().getPropertiesComponent().setLocation("classpath:config/api-config.properties");
        
        // Global error handler
        errorHandler(defaultErrorHandler()
            .maximumRedeliveries(3)
            .redeliveryDelay(2000)
            .onRedelivery(exchange -> {
                log.warn("Retrying API call. Attempt: {}", 
                    exchange.getIn().getHeader("CamelRedeliveryCounter"));
            }));
        
        // Route 1: File-triggered API consumption
        from("file:src/main/resources/input?noop=true&include=.*\\.txt")
            .routeId("file-triggered-api-call")
            .log("Processing file: ${header.CamelFileName}")
            .convertBodyTo(String.class)
            .split(body().tokenize("\n"))
            .filter(simple("${body} != ''"))
            .setHeader("postId", body())
            .log("Fetching post ID: ${header.postId}")
            .toD("{{api.base.url}}{{api.posts.endpoint}}/${header.postId}")
            .unmarshal().json(JsonLibrary.Jackson, Post.class)
            .process(new PostProcessor())
            .marshal().json(JsonLibrary.Jackson)
            .to("file:{{output.directory}}?fileName=processed-post-${header.postId}.json");
            
        // Route 2: Scheduled comprehensive data collection
        from("timer://dataCollection?period={{processing.delay}}&repeatCount=2")
            .routeId("comprehensive-data-collection")
            .log("Starting comprehensive data collection...")
            .multicast()
            .parallelProcessing()
            .to("direct:fetchUsers", "direct:fetchPosts", "direct:generateReport");
            
        // Sub-route: Fetch users
        from("direct:fetchUsers")
            .routeId("fetch-users-subroute")
            .log("Fetching users...")
            .to("{{api.base.url}}{{api.users.endpoint}}?_limit=3")
            .unmarshal().json(JsonLibrary.Jackson, User[].class)
            .split(body())
            .log("User: ${body.name} - ${body.email}")
            .marshal().json(JsonLibrary.Jackson)
            .to("file:{{output.directory}}?fileName=user-${body.id}.json");
            
        // Sub-route: Fetch posts
        from("direct:fetchPosts")
            .routeId("fetch-posts-subroute")
            .log("Fetching posts...")
            .to("{{api.base.url}}{{api.posts.endpoint}}?_limit={{processing.batch.size}}")
            .unmarshal().json(JsonLibrary.Jackson, Post[].class)
            .split(body())
            .process(new PostProcessor())
            .marshal().json(JsonLibrary.Jackson)
            .to("file:{{output.directory}}?fileName=post-${body.id}.json");
            
        // Sub-route: Generate report
        from("direct:generateReport")
            .routeId("generate-report-subroute")
            .log("Generating summary report...")
            .setBody(constant("Data Collection Report\n"))
            .setBody(simple("${body}Timestamp: ${date:now:yyyy-MM-dd HH:mm:ss}\n"))
            .setBody(simple("${body}Status: Data collection completed successfully\n"))
            .setBody(simple("${body}Batch Size: {{processing.batch.size}}\n"))
            .to("file:{{output.directory}}?fileName=report-${date:now:yyyyMMdd-HHmmss}.txt");
            
        // Route 3: REST endpoint aggregation
        from("timer://aggregateData?period=120000&repeatCount=1")
            .routeId("aggregate-user-posts")
            .log("Starting user-posts aggregation...")
            .setHeader("userId", constant(1))
            .enrich("{{api.base.url}}{{api.users.endpoint}}/1", (original, resource) -> {
                original.getIn().setHeader("userInfo", resource.getIn().getBody(String.class));
                return original;
            })
            .enrich("{{api.base.url}}{{api.posts.endpoint}}?userId=1&_limit=3", (original, resource) -> {
                original.getIn().setHeader("userPosts", resource.getIn().getBody(String.class));
                return original;
            })
            .setBody(simple("{\n  \"user\": ${header.userInfo},\n  \"posts\": ${header.userPosts}\n}"))
            .to("file:{{output.directory}}?fileName=user-posts-aggregated.json");
    }
}
