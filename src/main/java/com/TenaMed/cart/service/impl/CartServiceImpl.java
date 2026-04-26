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
import com.TenaMed.medicine.entity.Medicine;
import com.TenaMed.medicine.repository.MedicineRepository;
import com.TenaMed.medicine.service.MedicineService;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.dto.request.CreateOrderFromCartRequest;
import com.TenaMed.pharmacy.dto.response.OrderResponse;
import com.TenaMed.pharmacy.repository.PharmacyRepository;
import com.TenaMed.pharmacy.service.OrderService;
import com.TenaMed.prescription.service.PrescriptionService;
import com.TenaMed.events.DomainEventService;
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
    private final MedicineRepository medicineRepository;
    private final com.TenaMed.medicine.repository.ProductRepository productRepository;
    private final PharmacyRepository pharmacyRepository;
    private final MedicineService medicineService;
    private final InventoryService inventoryService;
    private final PrescriptionService prescriptionService;
    private final OrderService orderService;
    private final CartMapper cartMapper;
    private final DomainEventService domainEventService;

    public CartServiceImpl(CartRepository cartRepository,
                           CartItemRepository cartItemRepository,
                           MedicineRepository medicineRepository,
                           com.TenaMed.medicine.repository.ProductRepository productRepository,
                           PharmacyRepository pharmacyRepository,
                           MedicineService medicineService,
                           InventoryService inventoryService,
                           PrescriptionService prescriptionService,
                           OrderService orderService,
                           CartMapper cartMapper,
                           DomainEventService domainEventService) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.medicineRepository = medicineRepository;
        this.productRepository = productRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.medicineService = medicineService;
        this.inventoryService = inventoryService;
        this.prescriptionService = prescriptionService;
        this.orderService = orderService;
        this.cartMapper = cartMapper;
        this.domainEventService = domainEventService;
    }

    @Override
    @Transactional
    public void ensureActiveCart(UUID userId) {
        validateUserId(userId);
        getOrCreateActiveCart(userId);
    }

    @Override
    @Transactional
    public CartResponse addItem(UUID userId, AddCartItemRequest request) {
        validateUserId(userId);
        validateQuantity(request.getQuantity());

        UUID productId = request.getProductId();
        if (productId == null) {
            throw new CartValidationException("productId must never be null in Cart operations");
        }
        UUID pharmacyId = resolvePharmacyId(request.getPharmacyName());

        Cart cart = getOrCreateActiveCart(userId);

        com.TenaMed.medicine.entity.Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CartValidationException("Product not found"));
        Medicine medicine = product.getMedicine();

        BigDecimal unitPrice = inventoryService.resolveUnitPrice(productId);
        if (medicine == null || unitPrice == null) {
            throw new CartValidationException("Product details not found");
        }

        ensureStockAvailable(pharmacyId, productId, request.getQuantity());

        if (medicine.isRequiresPrescription()) {
            if (request.getPrescriptionId() == null) {
                throw new CartValidationException("Prescription is required for this medicine");
            }
            if (!prescriptionService.isPrescriptionValid(request.getPrescriptionId())) {
                throw new CartValidationException("Invalid prescription");
            }
        }

        // We assume CartItemRepository was updated to use findByCartIdAndProductIdAndPharmacyId
        // But for now, we'll try to keep the compilation intact. Let's assume we can change the repository.
        Optional<CartItem> existingItem = cartItemRepository.findByCartIdAndProductIdAndPharmacyId(
                cart.getId(),
            productId,
            pharmacyId
        );

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            if (!isSamePrescription(item.getPrescriptionId(), request.getPrescriptionId())) {
                throw new CartValidationException("Cannot change prescription for an existing cart item");
            }
            int updatedQty = item.getQuantity() + request.getQuantity();
            ensureStockAvailable(item.getPharmacyId(), item.getProductId(), updatedQty);
            item.setQuantity(updatedQty);
            item.setUnitPrice(unitPrice);
            item.calculateTotalPrice();
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProductId(productId);
            item.setPharmacyId(pharmacyId);
            item.setQuantity(request.getQuantity());
            item.setUnitPrice(unitPrice);
            item.setRequiresPrescription(medicine.isRequiresPrescription());
            item.setPrescriptionId(request.getPrescriptionId());
            item.calculateTotalPrice();
            cart.getItems().add(item);
        }

        Cart saved = cartRepository.save(cart);
        domainEventService.publish(
            "CART_ITEM_ADDED",
            "CART",
            saved.getId(),
            "USER",
            userId,
            "PLATFORM",
            null,
            Map.of("itemCount", saved.getItems().size())
        );
        return cartMapper.toResponse(saved);
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

        ensureStockAvailable(item.getPharmacyId(), item.getProductId(), request.getQuantity());
        item.setQuantity(request.getQuantity());
        item.calculateTotalPrice();

        Cart saved = cartRepository.save(cart);
        domainEventService.publish(
            "CART_ITEM_UPDATED",
            "CART",
            saved.getId(),
            "USER",
            userId,
            "PLATFORM",
            null,
            Map.of("itemId", itemId.toString(), "quantity", request.getQuantity())
        );
        return cartMapper.toResponse(saved);
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

        Cart saved = cartRepository.save(cart);
        domainEventService.publish(
            "CART_ITEM_REMOVED",
            "CART",
            saved.getId(),
            "USER",
            userId,
            "PLATFORM",
            null,
            Map.of("itemId", itemId.toString())
        );
        return cartMapper.toResponse(saved);
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
        Cart saved = cartRepository.save(cart);
        domainEventService.publish(
            "CART_CLEARED",
            "CART",
            saved.getId(),
            "USER",
            userId,
            "PLATFORM",
            null,
            Map.of()
        );
        return cartMapper.toResponse(saved);
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
                ensureStockAvailable(pharmacyId, item.getProductId(), item.getQuantity());
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
        Cart saved = cartRepository.save(cart);

        domainEventService.publish(
            "CART_CHECKED_OUT",
            "CART",
            saved.getId(),
            "USER",
            userId,
            "PLATFORM",
            null,
            Map.of("orderIds", orderIds)
        );

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
                Cart expired = cartRepository.save(cart);
                domainEventService.publish(
                        "CART_EXPIRED",
                        "CART",
                        expired.getId(),
                        "USER",
                        userId,
                        "PLATFORM",
                        null,
                        Map.of()
                );
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
            Cart expired = cartRepository.save(cart);
            domainEventService.publish(
                    "CART_EXPIRED",
                    "CART",
                    expired.getId(),
                    "USER",
                    expired.getUserId(),
                    "PLATFORM",
                    null,
                    Map.of()
            );
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
        Cart saved = cartRepository.save(cart);
        domainEventService.publish(
                "CART_CREATED",
                "CART",
                saved.getId(),
                "USER",
                userId,
                "PLATFORM",
                null,
                Map.of("status", saved.getStatus().name())
        );
        return saved;
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



    private UUID resolvePharmacyId(String pharmacyName) {
        Pharmacy pharmacy = pharmacyRepository
                .findFirstByNameIgnoreCaseOrLegalNameIgnoreCase(pharmacyName, pharmacyName)
                .orElseThrow(() -> new CartValidationException("Pharmacy not found: " + pharmacyName));
        validatePharmacyId(pharmacy.getId());
        return pharmacy.getId();
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

    private void ensureStockAvailable(UUID pharmacyId, UUID productId, int quantity) {
        if (productId == null) {
            throw new CartValidationException("productId must never be null in Cart operations");
        }
        if (!inventoryService.checkAvailability(pharmacyId, productId, quantity)) {
            org.slf4j.LoggerFactory.getLogger(CartServiceImpl.class).warn("Cart rejection event: Insufficient stock for product {} at pharmacy {} for quantity {}", productId, pharmacyId, quantity);
            throw new CartValidationException("Insufficient stock for product: " + productId + " at pharmacy: " + pharmacyId);
        }
    }
}
