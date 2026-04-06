package com.TenaMed.Normalization.service;

import com.TenaMed.medicine.dto.MedicineResponseDto;
import com.TenaMed.medicine.service.MedicineService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class DatabaseDrugLookupService implements DrugLookupService {

    private final MedicineService medicineService;
    private final long cacheTtlMillis;
    private final AtomicReference<LookupSnapshot> cache = new AtomicReference<>();

    public DatabaseDrugLookupService(MedicineService medicineService,
                                     @Value("${normalization.drug-lookup.cache-ttl-seconds:60}") long cacheTtlSeconds) {
        this.medicineService = medicineService;
        this.cacheTtlMillis = Math.max(0L, cacheTtlSeconds) * 1000L;
    }

    @Override
    public List<String> getStandardDrugNames() {
        return getSnapshot().standardDrugNames();
    }

    @Override
    public Map<String, String> getSynonymMappings() {
        return getSnapshot().synonymMappings();
    }

    private LookupSnapshot getSnapshot() {
        long now = System.currentTimeMillis();
        LookupSnapshot snapshot = cache.get();

        if (snapshot != null && now < snapshot.expiresAtMillis()) {
            return snapshot;
        }

        synchronized (this) {
            LookupSnapshot current = cache.get();
            long refreshedNow = System.currentTimeMillis();
            if (current != null && refreshedNow < current.expiresAtMillis()) {
                return current;
            }

            LookupSnapshot reloaded = reloadSnapshot(refreshedNow);
            cache.set(reloaded);
            return reloaded;
        }
    }

    private LookupSnapshot reloadSnapshot(long now) {
        List<MedicineResponseDto> medicines = medicineService.getAllMedicines();

        List<String> standardNames = medicines.stream()
                .map(MedicineResponseDto::getName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .toList();

        Map<String, String> synonyms = new LinkedHashMap<>();

        for (MedicineResponseDto medicine : medicines) {
            String name = normalize(medicine.getName());
            String genericName = normalize(medicine.getGenericName());

            if (name == null || genericName == null) {
                continue;
            }

            // Generic name acts as synonym that maps to the canonical medicine name.
            if (!name.equalsIgnoreCase(genericName)) {
                synonyms.putIfAbsent(genericName, name);
            }
        }

        return new LookupSnapshot(standardNames, Map.copyOf(synonyms), now + cacheTtlMillis);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private record LookupSnapshot(List<String> standardDrugNames,
                                  Map<String, String> synonymMappings,
                                  long expiresAtMillis) {
    }
}
