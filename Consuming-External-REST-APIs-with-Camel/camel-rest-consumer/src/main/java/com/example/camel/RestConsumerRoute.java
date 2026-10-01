package com.example.camel;

import com.example.camel.model.Post;
import com.example.camel.model.User;
import com.example.camel.processor.PostProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

public class RestConsumerRoute extends RouteBuilder {
    
    @Override
    public void configure() throws Exception {
        
        // Configure error handling
        errorHandler(defaultErrorHandler()
            .maximumRedeliveries(3)
            .redeliveryDelay(2000)
            .retryAttemptedLogLevel(org.apache.camel.LoggingLevel.WARN));
        
        // Route 1: Fetch and process single post
        from("timer://fetchPost?period=30000&repeatCount=3")
            .routeId("fetch-single-post")
            .log("Fetching single post...")
            .to("https://jsonplaceholder.typicode.com/posts/1")
            .unmarshal().json(JsonLibrary.Jackson, Post.class)
            .process(new PostProcessor())
            .log("Processed Post: ${body}")
            .log("Processing timestamp: ${header.ProcessedAt}");
            
        // Route 2: Fetch multiple posts and transform
        from("timer://fetchPosts?period=45000&repeatCount=2")
            .routeId("fetch-multiple-posts")
            .log("Fetching multiple posts...")
            .to("https://jsonplaceholder.typicode.com/posts?_limit=5")
            .unmarshal().json(JsonLibrary.Jackson, Post[].class)
            .split(body())
            .process(new PostProcessor())
            .choice()
                .when(simple("${body.userId} == 1"))
                    .log("Post by User 1: ${body.title}")
                .otherwise()
                    .log("Post by Other User: ${body.title}");
                    
        // Route 3: Fetch user data and combine with posts
        from("timer://fetchUserAndPosts?period=60000&repeatCount=2")
            .routeId("fetch-user-posts")
            .log("Fetching user data...")
            .setHeader("userId", constant(1))
            .to("https://jsonplaceholder.typicode.com/users/1")
            .unmarshal().json(JsonLibrary.Jackson, User.class)
            .log("User: ${body.name} (${body.email})")
            .setHeader("userName", simple("${body.name}"))
            .setHeader("userEmail", simple("${body.email}"))
            .log("Now fetching posts for user...")
            .to("https://jsonplaceholder.typicode.com/posts?userId=1&_limit=3")
            .unmarshal().json(JsonLibrary.Jackson, Post[].class)
            .split(body())
            .process(new PostProcessor())
            .log("Post by ${header.userName}: ${body.title}");
            
        // Route 4: Error handling demonstration
        from("timer://errorDemo?period=90000&repeatCount=1")
            .routeId("error-handling-demo")
            .log("Demonstrating error handling...")
            .to("https://jsonplaceholder.typicode.com/posts/999999") // Non-existent post
            .log("This should not be reached due to 404 error");
    }
}
