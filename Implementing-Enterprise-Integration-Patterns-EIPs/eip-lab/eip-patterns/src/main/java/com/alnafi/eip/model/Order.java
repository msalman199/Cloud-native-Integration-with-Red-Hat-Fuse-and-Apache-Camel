package com.alnafi.eip.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Order {
    @JsonProperty("orderId")
    private String orderId;

    @JsonProperty("customerType")
    private String customerType;

    @JsonProperty("amount")
    private double amount;

    @JsonProperty("priority")
    private String priority;

    @JsonProperty("items")
    private String[] items;

    public Order() {}

    public Order(String orderId, String customerType, double amount, String priority, String[] items) {
        this.orderId = orderId;
        this.customerType = customerType;
        this.amount = amount;
        this.priority = priority;
        this.items = items;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getCustomerType() { return customerType; }
    public void setCustomerType(String customerType) { this.customerType = customerType; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String[] getItems() { return items; }
    public void setItems(String[] items) { this.items = items; }

    @Override
    public String toString() {
        return String.format("Order{orderId='%s', customerType='%s', amount=%.2f, priority='%s'}",
                           orderId, customerType, amount, priority);
    }
}
