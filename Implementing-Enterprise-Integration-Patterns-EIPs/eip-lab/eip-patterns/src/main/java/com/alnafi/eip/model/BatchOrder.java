package com.alnafi.eip.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class BatchOrder {
    @JsonProperty("batchId")
    private String batchId;

    @JsonProperty("orders")
    private List<Order> orders;

    public BatchOrder() {}

    public BatchOrder(String batchId, List<Order> orders) {
        this.batchId = batchId;
        this.orders = orders;
    }

    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }

    public List<Order> getOrders() { return orders; }
    public void setOrders(List<Order> orders) { this.orders = orders; }

    @Override
    public String toString() {
        return String.format("BatchOrder{batchId='%s', orderCount=%d}",
                           batchId, orders != null ? orders.size() : 0);
    }
}
