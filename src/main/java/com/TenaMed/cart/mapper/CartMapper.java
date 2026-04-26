package com.TenaMed.cart.mapper;

import com.TenaMed.cart.dto.response.CartItemResponse;
import com.TenaMed.cart.dto.response.CartResponse;
import com.TenaMed.cart.entity.Cart;
import com.TenaMed.cart.entity.CartItem;
import com.TenaMed.medicine.repository.ProductRepository;
import com.TenaMed.pharmacy.dto.request.CreateOrderFromCartRequest;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.repository.PharmacyRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
public class CartMapper {

        private final ProductRepository productRepository;
        private final PharmacyRepository pharmacyRepository;

        public CartMapper(ProductRepository productRepository, PharmacyRepository pharmacyRepository) {
                this.productRepository = productRepository;
                this.pharmacyRepository = pharmacyRepository;
        }

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
                                .brandName(resolveProductName(item.getProductId()))
                                .pharmacyName(resolvePharmacyName(item.getPharmacyId()))
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .requiresPrescription(item.getRequiresPrescription())
                .prescriptionId(item.getPrescriptionId())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

        private String resolveProductName(UUID productId) {
                return productRepository.findById(productId)
                                .map(product -> product.getBrandName())
                                .orElse(productId.toString());
        }

        private String resolvePharmacyName(UUID pharmacyId) {
                return pharmacyRepository.findById(pharmacyId)
                                .map(this::displayPharmacyName)
                                .orElse(pharmacyId.toString());
        }

        private String displayPharmacyName(Pharmacy pharmacy) {
                if (pharmacy.getLegalName() != null && !pharmacy.getLegalName().isBlank()) {
                        return pharmacy.getLegalName();
                }
                return pharmacy.getName();
        }

    public CreateOrderFromCartRequest toOrderRequest(UUID pharmacyId, List<CartItem> cartItems) {
                List<CreateOrderFromCartRequest.Item> items = cartItems.stream()
                                .map(item -> {
                                        CreateOrderFromCartRequest.Item orderItem = new CreateOrderFromCartRequest.Item();
                                        orderItem.setProductId(item.getProductId());
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
