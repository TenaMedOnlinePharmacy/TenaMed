package com.TenaMed.cart.mapper;

import com.TenaMed.cart.dto.response.CartItemResponse;
import com.TenaMed.cart.dto.response.CartResponse;
import com.TenaMed.cart.entity.Cart;
import com.TenaMed.cart.entity.CartItem;
import com.TenaMed.pharmacy.dto.request.CreateOrderFromCartRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        BigDecimal total = cart.getItems().stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .status(cart.getStatus())
                .expiresAt(cart.getExpiresAt())
                .items(itemResponses)
                .cartTotal(total)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    public CartItemResponse toItemResponse(CartItem item) {
        return CartItemResponse.builder()
                .id(item.getId())
                .medicineId(item.getMedicineId())
                .pharmacyId(item.getPharmacyId())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .requiresPrescription(item.getRequiresPrescription())
                .prescriptionId(item.getPrescriptionId())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    public CreateOrderFromCartRequest toOrderRequest(UUID pharmacyId, List<CartItem> cartItems) {
                List<CreateOrderFromCartRequest.Item> items = cartItems.stream()
                                .map(item -> {
                                        CreateOrderFromCartRequest.Item orderItem = new CreateOrderFromCartRequest.Item();
                                        orderItem.setMedicineId(item.getMedicineId());
                                        orderItem.setQuantity(item.getQuantity());
                                        orderItem.setUnitPrice(item.getUnitPrice());
                                        orderItem.setPrescriptionId(item.getPrescriptionId());
                                        return orderItem;
                                })
                                .toList();

                CreateOrderFromCartRequest request = new CreateOrderFromCartRequest();
                                request.setPharmacyId(pharmacyId);
                request.setItems(items);
                return request;
    }
}
