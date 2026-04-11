package com.TenaMed.pharmacy.service.impl;

import com.TenaMed.pharmacy.entity.Order;
import com.TenaMed.pharmacy.service.OrderInventoryGateway;
import org.springframework.stereotype.Component;

@Component
public class NoOpOrderInventoryGateway implements OrderInventoryGateway {

    @Override
    public boolean reserveForOrder(Order order) {
        return true;
    }
}