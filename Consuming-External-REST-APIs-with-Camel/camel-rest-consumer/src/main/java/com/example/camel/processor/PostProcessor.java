package com.example.camel.processor;

import com.example.camel.model.Post;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

public class PostProcessor implements Processor {
    
    @Override
    public void process(Exchange exchange) throws Exception {
        Post post = exchange.getIn().getBody(Post.class);
        
        if (post != null) {
            // Transform the title to uppercase
            post.setTitle(post.getTitle().toUpperCase());
            
            // Truncate body if too long
            if (post.getBody().length() > 50) {
                post.setBody(post.getBody().substring(0, 50) + "...");
            }
            
            // Add a custom header
            exchange.getIn().setHeader("ProcessedAt", System.currentTimeMillis());
            exchange.getIn().setHeader("PostId", post.getId());
            
            // Set the processed post back to the body
            exchange.getIn().setBody(post);
        }
    }
}
