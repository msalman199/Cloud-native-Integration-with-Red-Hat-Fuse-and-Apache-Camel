package com.alnafi.camel.kafka;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ManualOrderProducer {
    
    public static void main(String[] args) throws Exception {
        CamelContext camelContext = new DefaultCamelContext();
        camelContext.start();
        
        ProducerTemplate producer = camelContext.createProducerTemplate();
        ObjectMapper mapper = new ObjectMapper();
        
        try {
            // Test Case 1: Small order (no discount)
            Order smallOrder = new Order("TEST001", "CUST999", "Headphones", 1, 150.0);
            String smallOrderJson = mapper.writeValueAsString(smallOrder);
            producer.sendBody("kafka:order-events?brokers=localhost:9092", smallOrderJson);
            System.out.println("Sent small order: " + smallOrderJson);
            
            Thread.sleep(2000);
            
            // Test Case 2: Large order (should get discount)
            Order largeOrder = new Order("TEST002", "CUST999", "Laptop", 2, 800.0);
            String largeOrderJson = mapper.writeValueAsString(largeOrder);
            producer.sendBody("kafka:order-events?brokers=localhost:9092", largeOrderJson);
            System.out.println("Sent large order: " + largeOrderJson);
            
            Thread.sleep(2000);
            
        } finally {
            producer.stop();
            camelContext.stop();
        }
    }
}
