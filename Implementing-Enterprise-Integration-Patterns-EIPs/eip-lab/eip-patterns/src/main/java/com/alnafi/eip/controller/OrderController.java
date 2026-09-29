package com.alnafi.eip.controller;

import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private ProducerTemplate producerTemplate;

    @PostMapping("/process")
    public ResponseEntity<String> processOrder(@RequestBody String orderJson) {
        try {
            String result = producerTemplate.requestBody("direct:processOrder", orderJson, String.class);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing order: " + e.getMessage());
        }
    }

    @PostMapping("/batch/process")
    public ResponseEntity<String> processBatchOrder(@RequestBody String batchOrderJson) {
        try {
            producerTemplate.sendBody("direct:processBatchOrder", batchOrderJson);
            return ResponseEntity.ok("Batch order processing initiated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing batch order: " + e.getMessage());
        }
    }

    @PostMapping("/aggregate")
    public ResponseEntity<String> processOrderForAggregation(@RequestBody String orderJson) {
        try {
            producerTemplate.sendBody("direct:processOrderForAggregation", orderJson);
            return ResponseEntity.ok("Order sent for aggregation");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error processing order for aggregation: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Order processing service is running");
    }
}
