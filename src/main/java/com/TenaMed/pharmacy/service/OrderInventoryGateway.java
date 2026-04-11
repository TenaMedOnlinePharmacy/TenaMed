package com.TenaMed.pharmacy.service;

import com.TenaMed.pharmacy.entity.Order;

public interface OrderInventoryGateway {

    boolean reserveForOrder(Order order);
}