package com.TenaMed.pharmacy.service.impl;

import com.TenaMed.inventory.entity.Inventory;
import com.TenaMed.inventory.repository.InventoryRepository;
import com.TenaMed.pharmacy.dto.request.ProductRatingRequest;
import com.TenaMed.pharmacy.dto.response.ProductRatingResponse;
import com.TenaMed.pharmacy.dto.response.ProductRatingSummaryResponse;
import com.TenaMed.pharmacy.dto.response.ProductRatingUpsertResponse;
import com.TenaMed.pharmacy.entity.OrderItem;
import com.TenaMed.pharmacy.entity.ProductRating;
import com.TenaMed.pharmacy.enums.OrderStatus;
import com.TenaMed.pharmacy.exception.PharmacyValidationException;
import com.TenaMed.pharmacy.exception.ProductRatingAuthorizationException;
import com.TenaMed.pharmacy.exception.ProductRatingNotFoundException;
import com.TenaMed.pharmacy.repository.OrderItemRepository;
import com.TenaMed.pharmacy.repository.ProductRatingRepository;
import com.TenaMed.pharmacy.service.ProductRatingService;
import com.TenaMed.user.entity.User;
import com.TenaMed.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ProductRatingServiceImpl implements ProductRatingService {

    private static final EnumSet<OrderStatus> ELIGIBLE_STATUSES = EnumSet.of(OrderStatus.COMPLETED);

    private final ProductRatingRepository productRatingRepository;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    public ProductRatingServiceImpl(ProductRatingRepository productRatingRepository,
                                    InventoryRepository inventoryRepository,
                                    UserRepository userRepository,
                                    OrderItemRepository orderItemRepository) {
        this.productRatingRepository = productRatingRepository;
        this.inventoryRepository = inventoryRepository;
        this.userRepository = userRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public ProductRatingUpsertResponse createOrUpdateRating(UUID customerId, ProductRatingRequest request) {
        if (customerId == null) {
            throw new PharmacyValidationException("Customer id is required");
        }
        if (request == null || request.getInventoryId() == null) {
            throw new PharmacyValidationException("Inventory id is required");
        }

        Inventory inventory = inventoryRepository.findById(request.getInventoryId())
            .orElseThrow(() -> new PharmacyValidationException("Inventory not found: " + request.getInventoryId()));

        User user = userRepository.findById(customerId)
            .orElseThrow(() -> new PharmacyValidationException("User not found: " + customerId));

        boolean purchased = orderItemRepository.existsByOrderCustomerIdAndInventoryIdAndOrderStatusIn(
            customerId,
            inventory.getId(),
            ELIGIBLE_STATUSES
        );
        if (!purchased) {
            throw new PharmacyValidationException("You can only rate items from completed orders");
        }

        ProductRating rating = productRatingRepository
            .findByUserIdAndInventoryId(customerId, inventory.getId())
            .orElseGet(ProductRating::new);

        rating.setInventory(inventory);
        rating.setUser(user);
        rating.setRating(request.getRating());
        rating.setReviewText(request.getReviewText());

        resolveLatestEligibleOrder(customerId, inventory.getId())
            .map(OrderItem::getOrder)
            .ifPresent(rating::setOrder);

        ProductRating savedRating = productRatingRepository.save(rating);
        RatingAggregate aggregate = updateAggregates(inventory);

        return ProductRatingUpsertResponse.builder()
            .rating(toResponse(savedRating))
            .averageRating(aggregate.averageRating())
            .ratingCount(aggregate.ratingCount())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductRatingSummaryResponse getRatingsForInventory(UUID inventoryId) {
        if (inventoryId == null) {
            throw new PharmacyValidationException("Inventory id is required");
        }

        Inventory inventory = inventoryRepository.findById(inventoryId)
            .orElseThrow(() -> new PharmacyValidationException("Inventory not found: " + inventoryId));

        List<ProductRatingResponse> ratings = productRatingRepository.findByInventoryIdOrderByCreatedAtDesc(inventoryId)
            .stream()
            .map(this::toResponse)
            .toList();

        Double averageRating = inventory.getAverageRating() == null ? 0.0 : inventory.getAverageRating();
        Integer ratingCount = inventory.getRatingCount() == null ? 0 : inventory.getRatingCount();

        return ProductRatingSummaryResponse.builder()
            .inventoryId(inventoryId)
            .averageRating(averageRating)
            .ratingCount(ratingCount)
            .ratings(ratings)
            .build();
    }

    @Override
    public void deleteRating(UUID ratingId, UUID actorUserId, boolean isAdmin) {
        if (ratingId == null) {
            throw new PharmacyValidationException("Rating id is required");
        }
        if (actorUserId == null) {
            throw new PharmacyValidationException("User id is required");
        }

        ProductRating rating = productRatingRepository.findById(ratingId)
            .orElseThrow(() -> new ProductRatingNotFoundException(ratingId));

        if (!isAdmin && !rating.getUser().getId().equals(actorUserId)) {
            throw new ProductRatingAuthorizationException("You are not allowed to delete this rating");
        }

        Inventory inventory = rating.getInventory();
        productRatingRepository.delete(rating);
        updateAggregates(inventory);
    }

    private Optional<OrderItem> resolveLatestEligibleOrder(UUID customerId, UUID inventoryId) {
        return orderItemRepository.findTopByOrderCustomerIdAndInventoryIdAndOrderStatusInOrderByOrderCreatedAtDesc(
            customerId,
            inventoryId,
            ELIGIBLE_STATUSES
        );
    }

    private RatingAggregate updateAggregates(Inventory inventory) {
        Object[] aggregate = productRatingRepository.getAggregateByInventoryId(inventory.getId());
        double averageRating = 0.0;
        int ratingCount = 0;
        if (aggregate != null && aggregate.length >= 2) {
            if (aggregate[0] instanceof Number avgValue) {
                averageRating = avgValue.doubleValue();
            }
            if (aggregate[1] instanceof Number countValue) {
                ratingCount = countValue.intValue();
            }
        }
        inventory.setAverageRating(averageRating);
        inventory.setRatingCount(ratingCount);
        inventoryRepository.save(inventory);
        return new RatingAggregate(averageRating, ratingCount);
    }

    private ProductRatingResponse toResponse(ProductRating rating) {
        return ProductRatingResponse.builder()
            .id(rating.getId())
            .inventoryId(rating.getInventory() != null ? rating.getInventory().getId() : null)
            .userId(rating.getUser() != null ? rating.getUser().getId() : null)
            .orderId(rating.getOrder() != null ? rating.getOrder().getId() : null)
            .rating(rating.getRating())
            .reviewText(rating.getReviewText())
            .createdAt(rating.getCreatedAt())
            .updatedAt(rating.getUpdatedAt())
            .build();
    }

    private record RatingAggregate(double averageRating, int ratingCount) {
    }
}
