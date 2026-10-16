package com.project.shift.shop.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ORDERS")
@SequenceGenerator(
        name = "SEQ_ORDERS_GEN",
        sequenceName = "SEQ_ORDERS",
        allocationSize = 1
)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ORDERS_GEN")
    @Column(name = "ORDER_ID")
    private Long orderId;

    @Column(name = "SENDER_ID", nullable = false)
    private Long senderId;

    @Column(name = "RECEIVER_ID", nullable = false)
    private Long receiverId;

    @Column(name = "ORDER_DATE", nullable = false)
    private LocalDateTime orderDate;

    @Column(name = "TOTAL_PRICE", nullable = false)
    private Integer totalPrice;

    @Column(name = "POINT_USED", nullable = false)
    private Integer pointUsed = 0;

    @Column(name = "REMAIN_POINTS", nullable = false)
    private Integer remainPoints = 0;

    @Column(name = "CASH_USED", nullable = false)
    private Integer cashUsed = 0;

    @Column(name = "ORDER_STATUS", nullable = false, columnDefinition = "VARCHAR2(2)")
    private String orderStatus = "P";
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Version
    @Column(name = "VERSION")
    private Long version;

    public void addItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
    }

    // Getter & Setter
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
    public Integer getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Integer totalPrice) { this.totalPrice = totalPrice; }
    public Integer getPointUsed() { return pointUsed; }
    public void setPointUsed(Integer pointUsed) { this.pointUsed = pointUsed; }
    public Integer getRemainPoints() { return remainPoints; }
    public void setRemainPoints(Integer remainPoints) { this.remainPoints = remainPoints; }
    public Integer getCashUsed() { return cashUsed; }
    public void setCashUsed(Integer cashUsed) { this.cashUsed = cashUsed; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }
    public List<OrderItem> getOrderItems() { return orderItems; }
    public void setOrderItems(List<OrderItem> orderItems) { this.orderItems = orderItems; }
}
