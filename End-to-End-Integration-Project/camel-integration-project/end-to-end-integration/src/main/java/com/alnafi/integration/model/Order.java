package com.alnafi.integration.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public class Order {
    @JsonProperty("id")
    private Long id;

    @JsonProperty("customer_id")
    private Long customerId;

    @JsonProperty("order_number")
    private String orderNumber;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    @JsonProperty("status")
    private String status;

    public Order() {}

    public Order(Long customerId, String orderNumber, BigDecimal totalAmount, String status) {
        this.customerId = customerId;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Order{id=" + id + ", customerId=" + customerId + ", orderNumber='" + orderNumber
                + "', totalAmount=" + totalAmount + ", status='" + status + "'}";
    }
}
