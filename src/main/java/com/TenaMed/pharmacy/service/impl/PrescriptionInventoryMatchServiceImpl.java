package com.TenaMed.pharmacy.service.impl;

import com.TenaMed.Normalization.entity.PrescriptionItem;
import com.TenaMed.Normalization.repository.PrescriptionItemRepository;
import com.TenaMed.inventory.entity.Inventory;
import com.TenaMed.inventory.enums.BatchStatus;
import com.TenaMed.inventory.repository.BatchRepository;
import com.TenaMed.inventory.repository.InventoryRepository;
import com.TenaMed.medicine.dto.MedicinePharmacySearchResponseDto;
import com.TenaMed.medicine.entity.Medicine;
import com.TenaMed.medicine.repository.MedicineRepository;
import com.TenaMed.pharmacy.entity.Pharmacy;
import com.TenaMed.pharmacy.exception.PharmacyValidationException;
import com.TenaMed.pharmacy.repository.PharmacyRepository;
import com.TenaMed.pharmacy.service.PrescriptionInventoryMatchService;
import com.TenaMed.prescription.repository.PrescriptionRepository;
import com.TenaMed.ocr.service.SupabaseStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PrescriptionInventoryMatchServiceImpl implements PrescriptionInventoryMatchService {

    private final PrescriptionRepository prescriptionRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final InventoryRepository inventoryRepository;
    private final MedicineRepository medicineRepository;
    private final com.TenaMed.medicine.repository.ProductRepository productRepository;
    private final PharmacyRepository pharmacyRepository;
    private final BatchRepository batchRepository;
    private final SupabaseStorageService supabaseStorageService;

    public PrescriptionInventoryMatchServiceImpl(PrescriptionRepository prescriptionRepository,
                                                 PrescriptionItemRepository prescriptionItemRepository,
                                                 InventoryRepository inventoryRepository,
                                                 MedicineRepository medicineRepository,
                                                 com.TenaMed.medicine.repository.ProductRepository productRepository,
                                                 PharmacyRepository pharmacyRepository,
                                                 BatchRepository batchRepository,
                                                 SupabaseStorageService supabaseStorageService) {
        this.prescriptionRepository = prescriptionRepository;
        this.prescriptionItemRepository = prescriptionItemRepository;
        this.inventoryRepository = inventoryRepository;
        this.medicineRepository = medicineRepository;
        this.productRepository = productRepository;
        this.pharmacyRepository = pharmacyRepository;
        this.batchRepository = batchRepository;
        this.supabaseStorageService = supabaseStorageService;
    }

    @Override
    public List<MedicinePharmacySearchResponseDto> findInventoryMatchesByPrescription(UUID prescriptionId) {
        boolean exists = prescriptionRepository.findById(prescriptionId).isPresent();
        if (!exists) {
            throw new PharmacyValidationException("Prescription not found: " + prescriptionId);
        }

        List<PrescriptionItem> prescriptionItems = prescriptionItemRepository.findByPrescriptionId(prescriptionId);
        if (prescriptionItems.isEmpty()) {
            return List.of();
        }

        Set<UUID> medicineIds = prescriptionItems.stream()
            .map(item -> item.getMedicine().getId())
            .collect(Collectors.toSet());

        List<com.TenaMed.medicine.entity.Product> products = productRepository.findByMedicineIdIn(medicineIds);
        if (products.isEmpty()) {
            return List.of();
        }

        Set<UUID> productIds = products.stream().map(com.TenaMed.medicine.entity.Product::getId).collect(Collectors.toSet());
        Map<UUID, com.TenaMed.medicine.entity.Product> productById = products.stream().collect(Collectors.toMap(com.TenaMed.medicine.entity.Product::getId, p -> p));

        List<Inventory> inventories = inventoryRepository.findByProductIdIn(productIds);
        if (inventories.isEmpty()) {
            return List.of();
        }

        Set<UUID> inventoryIds = inventories.stream()
            .map(Inventory::getId)
            .collect(Collectors.toSet());

        Map<UUID, PriceCandidate> bestPriceCandidateByInventoryId = new HashMap<>();
        batchRepository.findByInventoryIdIn(inventoryIds).stream()
            .filter(batch -> batch.getQuantity() != null && batch.getQuantity() > 0)
            .filter(batch -> batch.getSellingPrice() != null)
            .forEach(batch -> {
                UUID inventoryId = batch.getInventory().getId();
                PriceCandidate candidate = new PriceCandidate(
                    batch.getSellingPrice(),
                    batch.getStatus() == BatchStatus.ACTIVE
                );
                bestPriceCandidateByInventoryId.merge(
                    inventoryId,
                    candidate,
                    this::chooseBetterPriceCandidate
                );
            });

        Map<UUID, BigDecimal> minPriceByInventoryId = bestPriceCandidateByInventoryId.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().price()));

        if (minPriceByInventoryId.isEmpty()) {
            return List.of();
        }

        Map<UUID, Medicine> medicineById = medicineRepository.findAllById(medicineIds)
            .stream()
            .collect(Collectors.toMap(Medicine::getId, medicine -> medicine));

        Set<UUID> pharmacyIds = inventories.stream()
            .filter(inventory -> minPriceByInventoryId.containsKey(inventory.getId()))
            .map(Inventory::getPharmacyId)
            .collect(Collectors.toSet());

        Map<UUID, Pharmacy> pharmacyById = pharmacyRepository.findAllById(pharmacyIds)
            .stream()
            .collect(Collectors.toMap(Pharmacy::getId, pharmacy -> pharmacy));

        return inventories.stream()
            .map(inventory -> toMedicinePharmacySearchResponse(inventory, productById, medicineById, pharmacyById, minPriceByInventoryId))
            .filter(Objects::nonNull)
            .toList();
    }

    private MedicinePharmacySearchResponseDto toMedicinePharmacySearchResponse(Inventory inventory,
                                                                                Map<UUID, com.TenaMed.medicine.entity.Product> productById,
                                                                                Map<UUID, Medicine> medicineById,
                                                                                Map<UUID, Pharmacy> pharmacyById,
                                                                                Map<UUID, BigDecimal> minPriceByInventoryId) {
        com.TenaMed.medicine.entity.Product product = productById.get(inventory.getProductId());
        if (product == null) return null;
        Medicine medicine = medicineById.get(product.getMedicine().getId());
        Pharmacy pharmacy = pharmacyById.get(inventory.getPharmacyId());
        BigDecimal price = minPriceByInventoryId.get(inventory.getId());

        if (medicine == null || pharmacy == null || price == null) {
            return null;
        }

        return MedicinePharmacySearchResponseDto.builder()
            .productId(product.getId())
            .brandName(product.getBrandName())
            .medicineName(medicine.getName())
            .prescriptionRequired(medicine.isRequiresPrescription())
            .pharmacyLegalName(resolvePharmacyLegalName(pharmacy))
            .price(price)
            .medicineCategory(medicine.getCategory() != null ? medicine.getCategory().getName() : null)
//            .imageUrl(supabaseStorageService.resolveSignedUrl(
//                product.getImageUrl() != null ? product.getImageUrl() : medicine.getImageUrl()
//            ))
            .imageUrl(product.getImageUrl() != null ? product.getImageUrl() : medicine.getImageUrl())
            .indications(medicine.getIndications())
            .contraindications(medicine.getContraindications())
            .sideEffects(medicine.getSideEffects())
            .build();
    }

    private String resolvePharmacyLegalName(Pharmacy pharmacy) {
        if (StringUtils.hasText(pharmacy.getLegalName())) {
            return pharmacy.getLegalName();
        }
        return pharmacy.getName();
    }

    private PriceCandidate chooseBetterPriceCandidate(PriceCandidate current, PriceCandidate candidate) {
        if (current.isActive() && !candidate.isActive()) {
            return current;
        }
        if (!current.isActive() && candidate.isActive()) {
            return candidate;
        }
        return current.price().compareTo(candidate.price()) <= 0 ? current : candidate;
    }

    private record PriceCandidate(BigDecimal price, boolean isActive) {
    }

    @Override
    public List<com.TenaMed.pharmacy.dto.response.PrescriptionProductOptionDto> getProductOptionsForPrescriptionItem(UUID prescriptionItemId) {
        PrescriptionItem item = prescriptionItemRepository.findById(prescriptionItemId)
            .orElseThrow(() -> new PharmacyValidationException("Prescription item not found: " + prescriptionItemId));

        UUID medicineId = item.getMedicine().getId();
        List<com.TenaMed.medicine.entity.Product> products = productRepository.findByMedicineIdIn(Set.of(medicineId));
        if (products.isEmpty()) {
            return List.of();
        }

        Set<UUID> productIds = products.stream().map(com.TenaMed.medicine.entity.Product::getId).collect(Collectors.toSet());
        Map<UUID, com.TenaMed.medicine.entity.Product> productById = products.stream().collect(Collectors.toMap(com.TenaMed.medicine.entity.Product::getId, p -> p));

        List<Inventory> inventories = inventoryRepository.findByProductIdIn(productIds);
        if (inventories.isEmpty()) {
            return List.of();
        }

        Set<UUID> inventoryIds = inventories.stream()
            .map(Inventory::getId)
            .collect(Collectors.toSet());

        // Get total available quantity and best price per inventory
        Map<UUID, Integer> availableQuantityByInventoryId = new HashMap<>();
        Map<UUID, PriceCandidate> bestPriceCandidateByInventoryId = new HashMap<>();

        batchRepository.findByInventoryIdIn(inventoryIds).stream()
            .filter(batch -> batch.getQuantity() != null && batch.getQuantity() > 0)
            .forEach(batch -> {
                UUID inventoryId = batch.getInventory().getId();
                if (batch.getStatus() == BatchStatus.ACTIVE) {
                    availableQuantityByInventoryId.merge(inventoryId, batch.getQuantity(), Integer::sum);
                }

                if (batch.getSellingPrice() != null) {
                    PriceCandidate candidate = new PriceCandidate(
                        batch.getSellingPrice(),
                        batch.getStatus() == BatchStatus.ACTIVE
                    );
                    bestPriceCandidateByInventoryId.merge(
                        inventoryId,
                        candidate,
                        this::chooseBetterPriceCandidate
                    );
                }
            });

        Set<UUID> pharmacyIds = inventories.stream()
            .map(Inventory::getPharmacyId)
            .collect(Collectors.toSet());

        Map<UUID, Pharmacy> pharmacyById = pharmacyRepository.findAllById(pharmacyIds)
            .stream()
            .collect(Collectors.toMap(Pharmacy::getId, pharmacy -> pharmacy));

        return inventories.stream()
            .map(inventory -> {
                com.TenaMed.medicine.entity.Product product = productById.get(inventory.getProductId());
                Pharmacy pharmacy = pharmacyById.get(inventory.getPharmacyId());
                if (product == null || pharmacy == null) return null;

                PriceCandidate priceCandidate = bestPriceCandidateByInventoryId.get(inventory.getId());
                BigDecimal price = priceCandidate != null ? priceCandidate.price() : BigDecimal.ZERO;
                int availableQty = availableQuantityByInventoryId.getOrDefault(inventory.getId(), 0);

                return com.TenaMed.pharmacy.dto.response.PrescriptionProductOptionDto.builder()
                    .productId(product.getId())
                    .brandName(product.getBrandName())
                    .price(price)
                    .available(availableQty > 0)
                    .availableQuantity(availableQty)
                    .pharmacyId(pharmacy.getId())
                    .pharmacyName(resolvePharmacyLegalName(pharmacy))
                    .build();
            })
            .filter(Objects::nonNull)
            .toList();
    }
}