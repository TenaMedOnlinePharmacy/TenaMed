package com.TenaMed.delivery.service.impl;

import com.TenaMed.delivery.entity.Delivery;
import com.TenaMed.delivery.enums.DeliveryStatus;
import com.TenaMed.delivery.repository.DeliveryRepository;
import com.TenaMed.delivery.service.DeliveryService;
import com.TenaMed.pharmacy.entity.Order;
import com.TenaMed.pharmacy.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final OrderRepository orderRepository;

    public DeliveryServiceImpl(DeliveryRepository deliveryRepository, OrderRepository orderRepository) {
        this.deliveryRepository = deliveryRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public Delivery createDelivery(UUID orderId, String deliveryAddress) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        Delivery delivery = deliveryRepository.findByOrderId(orderId)
            .orElseGet(() -> {
                Delivery created = new Delivery();
                created.setOrder(order);
                created.setStatus(DeliveryStatus.READY_FOR_DELIVERY);
                return created;
            });

        delivery.setDeliveryAddress(deliveryAddress);
        Delivery saved = deliveryRepository.save(delivery);
        order.setDeliveryId(saved.getId());
        orderRepository.save(order);
        return saved;
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
