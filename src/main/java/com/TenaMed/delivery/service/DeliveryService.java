package com.TenaMed.delivery.service;

import com.TenaMed.delivery.entity.Delivery;
import com.TenaMed.pharmacy.entity.Order;

import java.util.UUID;

public interface DeliveryService {

    Delivery createDelivery(Order order);

    Delivery dispatchDelivery(UUID deliveryId);

    Delivery markDelivered(UUID deliveryId);

    Delivery markFailed(UUID deliveryId, String reason);
}
