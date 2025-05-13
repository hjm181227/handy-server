package com.handy.appserver.entity.delivery;

import com.handy.appserver.entity.common.BaseTimeEntity;
import com.handy.appserver.entity.order.Order;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "deliveries")
public class Delivery extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String receiverName;

    @Column
    private String trackingNumber;

    public Delivery(Order order, String address, String phoneNumber, String receiverName) {
        this.order = order;
        this.status = DeliveryStatus.READY;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.receiverName = receiverName;
    }

    public void updateStatus(DeliveryStatus status) {
        this.status = status;
    }

    public void updateTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }
} 