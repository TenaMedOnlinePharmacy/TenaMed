package com.TenaMed.medicine.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface ProductImageService {

    String resolveProductImage(UUID productId, UUID pharmacyId);

    Map<UUID, String> resolveProductImages(Collection<UUID> productIds, UUID pharmacyId);

    void saveDefaultImage(UUID productId, String imageUrl);

    void savePharmacyImage(UUID productId, UUID pharmacyId, String imageUrl);

    String uploadPharmacyImage(UUID productId, UUID pharmacyId, MultipartFile image);

    String uploadDefaultImage(UUID productId, MultipartFile image);
}
