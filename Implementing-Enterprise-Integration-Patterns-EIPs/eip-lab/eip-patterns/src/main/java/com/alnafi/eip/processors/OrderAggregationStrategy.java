package com.alnafi.eip.processors;

import com.alnafi.eip.model.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class OrderAggregationStrategy implements AggregationStrategy {

    private static final Logger logger = LoggerFactory.getLogger(OrderAggregationStrategy.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        try {
            String orderJson = newExchange.getIn().getBody(String.class);
            Order newOrder = objectMapper.readValue(orderJson, Order.class);

            if (oldExchange == null) {
                List<Order> orders = new ArrayList<>();
                orders.add(newOrder);

                newExchange.getIn().setBody(orders);
                newExchange.getIn().setHeader("aggregatedCount", 1);
                newExchange.getIn().setHeader("totalAmount", newOrder.getAmount());

                logger.info("Starting aggregation with order: {}", newOrder.getOrderId());
                return newExchange;
            } else {
                @SuppressWarnings("unchecked")
                List<Order> orders = oldExchange.getIn().getBody(List.class);
                orders.add(newOrder);

                int count = oldExchange.getIn().getHeader("aggregatedCount", Integer.class) + 1;
                double totalAmount = oldExchange.getIn().getHeader("totalAmount", Double.class) + newOrder.getAmount();

                oldExchange.getIn().setBody(orders);
                oldExchange.getIn().setHeader("aggregatedCount", count);
                oldExchange.getIn().setHeader("totalAmount", totalAmount);

                logger.info("Aggregated {} orders, total amount so far: {}", count, totalAmount);
                return oldExchange;
            }
        } catch (Exception e) {
            logger.error("Error during aggregation", e);
            throw new RuntimeException("Aggregation failed", e);
        }
    }
}
