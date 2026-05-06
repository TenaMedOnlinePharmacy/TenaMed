package com.TenaMed.delivery.service.impl;

import com.TenaMed.delivery.entity.Delivery;
import com.TenaMed.delivery.enums.DeliveryStatus;
import com.TenaMed.delivery.repository.DeliveryRepository;
import com.TenaMed.delivery.service.DeliveryService;
import com.TenaMed.pharmacy.entity.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;

    public DeliveryServiceImpl(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    @Override
    public Delivery createDelivery(Order order) {
        return deliveryRepository.findByOrderId(order.getId())
            .orElseGet(() -> {
                Delivery delivery = new Delivery();
                delivery.setOrder(order);
                delivery.setStatus(DeliveryStatus.READY_FOR_DELIVERY);
                delivery.setDeliveryAddress(order.getDeliveryAddress());
                return deliveryRepository.save(delivery);
            });
    }

    @Override
    public Delivery dispatchDelivery(UUID deliveryId) {
        Delivery delivery = fetchDelivery(deliveryId);
        delivery.setStatus(DeliveryStatus.OUT_FOR_DELIVERY);
        delivery.setDispatchedAt(LocalDateTime.now());
        return deliveryRepository.save(delivery);
    }

    @Override
    public Delivery markDelivered(UUID deliveryId) {
        Delivery delivery = fetchDelivery(deliveryId);
        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.setDeliveredAt(LocalDateTime.now());
        return deliveryRepository.save(delivery);
    }

    @Override
    public Delivery markFailed(UUID deliveryId, String reason) {
        Delivery delivery = fetchDelivery(deliveryId);
        delivery.setStatus(DeliveryStatus.FAILED);
        delivery.setFailureReason(reason);
        return deliveryRepository.save(delivery);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Delivery> getDeliveriesByStatus(DeliveryStatus status) {
        return deliveryRepository.findWithOrderByStatus(status);
    }

    private Delivery fetchDelivery(UUID deliveryId) {
        return deliveryRepository.findById(deliveryId)
            .orElseThrow(() -> new IllegalArgumentException("Delivery not found with id: " + deliveryId));
    }
}
