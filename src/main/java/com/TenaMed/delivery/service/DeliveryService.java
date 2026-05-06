package com.TenaMed.delivery.service;

import com.TenaMed.delivery.entity.Delivery;
import com.TenaMed.delivery.enums.DeliveryStatus;
import com.TenaMed.pharmacy.entity.Order;

import java.util.List;
import java.util.UUID;

public interface DeliveryService {

    Delivery createDelivery(Order order);

    Delivery dispatchDelivery(UUID deliveryId);

    Delivery markDelivered(UUID deliveryId);

    Delivery markFailed(UUID deliveryId, String reason);

    List<Delivery> getDeliveriesByStatus(DeliveryStatus status);
}
