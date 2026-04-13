package com.TenaMed.cart.service.impl;

import com.TenaMed.cart.dto.request.AddCartItemRequest;
import com.TenaMed.cart.dto.request.UpdateCartItemQuantityRequest;
import com.TenaMed.cart.dto.response.CartResponse;
import com.TenaMed.cart.dto.response.CheckoutCartResponse;
import com.TenaMed.cart.entity.Cart;
import com.TenaMed.cart.entity.CartItem;
import com.TenaMed.cart.entity.CartStatus;
import com.TenaMed.cart.exception.CartItemNotFoundException;
import com.TenaMed.cart.exception.CartValidationException;
import com.TenaMed.cart.mapper.CartMapper;
import com.TenaMed.cart.repository.CartItemRepository;
import com.TenaMed.cart.repository.CartRepository;
import com.TenaMed.cart.service.CartService;
import com.TenaMed.inventory.service.InventoryService;
import com.TenaMed.medicine.dto.MedicineResponseDto;
import com.TenaMed.medicine.service.MedicineService;
import com.TenaMed.pharmacy.dto.request.CreateOrderFromCartRequest;
import com.TenaMed.pharmacy.dto.response.OrderResponse;
import com.TenaMed.pharmacy.service.OrderService;
import com.TenaMed.prescription.service.PrescriptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    private static final int CART_EXPIRY_HOURS = 24;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MedicineService medicineService;
    private final InventoryService inventoryService;
    private final PrescriptionService prescriptionService;
    private final OrderService orderService;
    private final CartMapper cartMapper;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           MedicineService medicineService,
                           InventoryService inventoryService,
                           PrescriptionService prescriptionService,
                           OrderService orderService,
                           CartMapper cartMapper) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.medicineService = medicineService;
        this.inventoryService = inventoryService;
        this.prescriptionService = prescriptionService;
        this.orderService = orderService;
        this.cartMapper = cartMapper;
    }

    @Override
    @Transactional
    public CartResponse addItem(UUID userId, AddCartItemRequest request) {
        validateUserId(userId);
        validatePharmacyId(request.getPharmacyId());
        validateQuantity(request.getQuantity());

        Cart cart = getOrCreateActiveCart(userId);

        MedicineResponseDto medicine = medicineService.getMedicineById(request.getMedicineId());
        BigDecimal unitPrice = inventoryService.resolveUnitPrice(request.getMedicineId());
        if (medicine == null || unitPrice == null) {
            throw new CartValidationException("Medicine details not found");
        }

        ensureStockAvailable(request.getPharmacyId(), request.getMedicineId(), request.getQuantity());

        if (medicine.isRequiresPrescription()) {
            if (request.getPrescriptionId() == null) {
                throw new CartValidationException("Prescription is required for this medicine");
            }
            if (!prescriptionService.isPrescriptionValid(request.getPrescriptionId())) {
                throw new CartValidationException("Invalid prescription");
            }
        }

        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndMedicineIdAndPharmacyId(
                cart.getId(),
                request.getMedicineId(),
                request.getPharmacyId()
        );

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            if (!isSamePrescription(item.getPrescriptionId(), request.getPrescriptionId())) {
                throw new CartValidationException("Cannot change prescription for an existing cart item");
            }
            int updatedQty = item.getQuantity() + request.getQuantity();
            ensureStockAvailable(item.getPharmacyId(), item.getMedicineId(), updatedQty);
            item.setQuantity(updatedQty);
            item.setUnitPrice(unitPrice);
            item.calculateTotalPrice();
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setMedicineId(request.getMedicineId());
            item.setPharmacyId(request.getPharmacyId());
            item.setQuantity(request.getQuantity());
            item.setUnitPrice(unitPrice);
            item.setRequiresPrescription(medicine.isRequiresPrescription());
            item.setPrescriptionId(request.getPrescriptionId());
            item.calculateTotalPrice();
            cart.getItems().add(item);
        }

        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse updateItemQuantity(UUID userId, UUID itemId, UpdateCartItemQuantityRequest request) {
        validateUserId(userId);
        validateQuantity(request.getQuantity());

        Cart cart = getActiveCart(userId);
        CartItem item = cart.getItems().stream()
                .filter(cartItem -> cartItem.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new CartItemNotFoundException(itemId));

        ensureStockAvailable(item.getPharmacyId(), item.getMedicineId(), request.getQuantity());
        item.setQuantity(request.getQuantity());
        item.calculateTotalPrice();

        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse removeItem(UUID userId, UUID itemId) {
        validateUserId(userId);

        Cart cart = getActiveCart(userId);
        CartItem item = cart.getItems().stream()
                .filter(cartItem -> cartItem.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new CartItemNotFoundException(itemId));

        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        validateUserId(userId);

        return cartRepository.findWithItemsByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .map(cartMapper::toResponse)
                .orElseGet(() -> {
                    Cart empty = new Cart();
                    empty.setUserId(userId);
                    empty.setStatus(CartStatus.ACTIVE);
                    empty.setExpiresAt(LocalDateTime.now().plusHours(CART_EXPIRY_HOURS));
                    return cartMapper.toResponse(empty);
                });
    }

    @Override
    @Transactional
    public CartResponse clearCart(UUID userId) {
        validateUserId(userId);

        Cart cart = getActiveCart(userId);
        cart.getItems().clear();
        return cartMapper.toResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CheckoutCartResponse checkout(UUID userId) {
        validateUserId(userId);

        Cart cart = getActiveCart(userId);
        if (cart.getItems().isEmpty()) {
            throw new CartValidationException("Cart is empty");
        }

        Map<UUID, List<CartItem>> itemsByPharmacy = cart.getItems().stream()
                .collect(Collectors.groupingBy(CartItem::getPharmacyId));

        List<UUID> orderIds = new ArrayList<>();

        for (Map.Entry<UUID, List<CartItem>> pharmacyGroup : itemsByPharmacy.entrySet()) {
            UUID pharmacyId = pharmacyGroup.getKey();
            List<CartItem> items = pharmacyGroup.getValue();

            for (CartItem item : items) {
                ensureStockAvailable(pharmacyId, item.getMedicineId(), item.getQuantity());
                if (Boolean.TRUE.equals(item.getRequiresPrescription())) {
                    if (item.getPrescriptionId() == null) {
                        throw new CartValidationException("Prescription is required for checkout");
                    }
                    if (!prescriptionService.isPrescriptionValid(item.getPrescriptionId())) {
                        throw new CartValidationException("Invalid prescription during checkout");
                    }
                }
            }

            CreateOrderFromCartRequest orderRequest = cartMapper.toOrderRequest(pharmacyId, items);
            OrderResponse orderResponse = orderService.createOrderFromCart(userId, orderRequest);
            if (orderResponse == null || orderResponse.getId() == null) {
                throw new CartValidationException("Failed to create order from cart for pharmacy: " + pharmacyId);
            }
            orderIds.add(orderResponse.getId());
        }

        cart.setStatus(CartStatus.CHECKED_OUT);
        cart.setExpiresAt(LocalDateTime.now());
        cartRepository.save(cart);

        return CheckoutCartResponse.builder()
                .orderIds(orderIds)
                .message("Cart checked out successfully")
                .status("SUCCESS")
                .build();
    }

    private Cart getOrCreateActiveCart(UUID userId) {
        Optional<Cart> existing = cartRepository.findWithItemsByUserIdAndStatus(userId, CartStatus.ACTIVE);
        if (existing.isPresent()) {
            Cart cart = existing.get();
            if (isExpired(cart)) {
                cart.setStatus(CartStatus.EXPIRED);
                cartRepository.save(cart);
                return createNewCart(userId);
            }
            return cart;
        }
        return createNewCart(userId);
    }

    private Cart getActiveCart(UUID userId) {
        Cart cart = cartRepository.findWithItemsByUserIdAndStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(() -> new CartValidationException("Active cart not found"));
        return expireIfNeeded(cart);
    }

    private Cart expireIfNeeded(Cart cart) {
        if (isExpired(cart)) {
            cart.setStatus(CartStatus.EXPIRED);
            cartRepository.save(cart);
            throw new CartValidationException("Active cart has expired");
        }
        return cart;
    }

    private boolean isExpired(Cart cart) {
        return cart.getExpiresAt() != null && cart.getExpiresAt().isBefore(LocalDateTime.now());
    }

    private Cart createNewCart(UUID userId) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        cart.setStatus(CartStatus.ACTIVE);
        cart.setExpiresAt(LocalDateTime.now().plusHours(CART_EXPIRY_HOURS));
        return cartRepository.save(cart);
    }

    private void validateUserId(UUID userId) {
        if (userId == null) {
            throw new CartValidationException("userId is required");
        }
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new CartValidationException("Quantity must be greater than zero");
        }
    }

    private void validatePharmacyId(UUID pharmacyId) {
        if (pharmacyId == null) {
            throw new CartValidationException("pharmacyId is required");
        }
    }

    private boolean isSamePrescription(UUID existingPrescriptionId, UUID requestPrescriptionId) {
        if (existingPrescriptionId == null && requestPrescriptionId == null) {
            return true;
        }
        if (existingPrescriptionId == null || requestPrescriptionId == null) {
            return false;
        }
        return existingPrescriptionId.equals(requestPrescriptionId);
    }

    private void ensureStockAvailable(UUID pharmacyId, UUID medicineId, int quantity) {
        if (!inventoryService.checkAvailability(pharmacyId, medicineId, quantity)) {
            throw new CartValidationException("Insufficient stock for medicine: " + medicineId + " at pharmacy: " + pharmacyId);
        }
    }
}
