package com.TenaMed.medicine.service.impl;

import com.TenaMed.medicine.entity.Product;
import com.TenaMed.medicine.entity.ProductImage;
import com.TenaMed.medicine.repository.ProductImageRepository;
import com.TenaMed.medicine.repository.ProductRepository;
import com.TenaMed.medicine.service.ProductImageService;
import com.TenaMed.ocr.service.SupabaseStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductImageServiceImpl implements ProductImageService {

    private static final String PRODUCT_IMAGE_FOLDER = "products";

    private final ProductImageRepository productImageRepository;
    private final ProductRepository productRepository;
    private final SupabaseStorageService supabaseStorageService;

    public ProductImageServiceImpl(ProductImageRepository productImageRepository,
                                   ProductRepository productRepository,
                                   SupabaseStorageService supabaseStorageService) {
        this.productImageRepository = productImageRepository;
        this.productRepository = productRepository;
        this.supabaseStorageService = supabaseStorageService;
    }

    @Override
    @Transactional(readOnly = true)
    public String resolveProductImage(UUID productId, UUID pharmacyId) {
        if (productId == null) {
            return null;
        }

        // Step 1: Pharmacy-specific image
        if (pharmacyId != null) {
            String pharmacyImage = productImageRepository
                .findByProductIdAndPharmacyIdAndIsPrimaryTrue(productId, pharmacyId)
                .map(ProductImage::getImageUrl)
                .orElse(null);
            if (pharmacyImage != null) {
                return pharmacyImage;
            }
        }

        // Step 2: Default/global image (pharmacy_id IS NULL)
        return productImageRepository
            .findDefaultByProductId(productId)
            .map(ProductImage::getImageUrl)
            .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> resolveProductImages(Collection<UUID> productIds, UUID pharmacyId) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, String> result = new HashMap<>();

        // Bulk-load pharmacy-specific images if pharmacyId is provided
        Map<UUID, String> pharmacyImages = Map.of();
        if (pharmacyId != null) {
            pharmacyImages = productImageRepository
                .findByProductIdsAndPharmacyId(productIds, pharmacyId)
                .stream()
                .collect(Collectors.toMap(
                    pi -> pi.getProduct().getId(),
                    ProductImage::getImageUrl,
                    (a, b) -> a
                ));
        }

        // Bulk-load default images
        Map<UUID, String> defaultImages = productImageRepository
            .findDefaultsByProductIds(productIds)
            .stream()
            .collect(Collectors.toMap(
                pi -> pi.getProduct().getId(),
                ProductImage::getImageUrl,
                (a, b) -> a
            ));

        for (UUID productId : productIds) {
            String image = pharmacyImages.get(productId);
            if (image == null) {
                image = defaultImages.get(productId);
            }
            if (image != null) {
                result.put(productId, image);
            }
        }

        return result;
    }

    @Override
    public void saveDefaultImage(UUID productId, String imageUrl) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        productImageRepository.findDefaultByProductId(productId)
            .ifPresentOrElse(
                existing -> {
                    existing.setImageUrl(imageUrl);
                    productImageRepository.save(existing);
                },
                () -> {
                    ProductImage image = new ProductImage();
                    image.setProduct(product);
                    image.setPharmacyId(null);
                    image.setImageUrl(imageUrl);
                    image.setPrimary(true);
                    productImageRepository.save(image);
                }
            );
    }

    @Override
    public void savePharmacyImage(UUID productId, UUID pharmacyId, String imageUrl) {
        if (pharmacyId == null) {
            saveDefaultImage(productId, imageUrl);
            return;
        }

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        // Upsert: update existing or create new
        productImageRepository.findByProductIdAndPharmacyIdAndIsPrimaryTrue(productId, pharmacyId)
            .ifPresentOrElse(
                existing -> {
                    existing.setImageUrl(imageUrl);
                    productImageRepository.save(existing);
                },
                () -> {
                    ProductImage image = new ProductImage();
                    image.setProduct(product);
                    image.setPharmacyId(pharmacyId);
                    image.setImageUrl(imageUrl);
                    image.setPrimary(true);
                    productImageRepository.save(image);
                }
            );
    }

    @Override
    public String uploadPharmacyImage(UUID productId, UUID pharmacyId, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        String objectPath = supabaseStorageService.uploadAndGetObjectPath(image, PRODUCT_IMAGE_FOLDER);
        savePharmacyImage(productId, pharmacyId, objectPath);
        return objectPath;
    }

    @Override
    public String uploadDefaultImage(UUID productId, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        String objectPath = supabaseStorageService.uploadAndGetObjectPath(image, PRODUCT_IMAGE_FOLDER);
        saveDefaultImage(productId, objectPath);
        return objectPath;
    }
}
