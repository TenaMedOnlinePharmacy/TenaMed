package com.TenaMed.antidoping.service;

import com.TenaMed.antidoping.client.RxNormClient;
import com.TenaMed.antidoping.entity.RxNormMapping;
import com.TenaMed.antidoping.entity.RxcuiIngredient;
import com.TenaMed.antidoping.repository.RxNormMappingRepository;
import com.TenaMed.antidoping.repository.RxcuiIngredientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class IngredientResolverService {

    private final RxNormMappingRepository rxNormMappingRepository;
    private final RxcuiIngredientRepository rxcuiIngredientRepository;
    private final RxNormClient rxNormClient;

    public IngredientResolverService(RxNormMappingRepository rxNormMappingRepository,
                                     RxcuiIngredientRepository rxcuiIngredientRepository,
                                     RxNormClient rxNormClient) {
        this.rxNormMappingRepository = rxNormMappingRepository;
        this.rxcuiIngredientRepository = rxcuiIngredientRepository;
        this.rxNormClient = rxNormClient;
    }

    @Transactional
    public List<String> resolveIngredients(String medicineName) {
        if (medicineName == null || medicineName.isBlank()) {
            return List.of();
        }

        String normalizedMedicineName = medicineName.trim();
        String rxcui = resolveOrCreateRxcui(normalizedMedicineName);
        if (rxcui == null || rxcui.isBlank()) {
            return List.of();
        }

        List<RxcuiIngredient> cachedIngredients = rxcuiIngredientRepository.findByRxcui(rxcui);
        if (!cachedIngredients.isEmpty()) {
            return cachedIngredients.stream()
                    .map(RxcuiIngredient::getIngredientName)
                    .filter(name -> name != null && !name.isBlank())
                    .map(String::trim)
                    .distinct()
                    .toList();
        }

        List<String> apiIngredients = rxNormClient.getIngredients(rxcui);
        if (apiIngredients.isEmpty()) {
            return List.of();
        }

        Set<String> uniqueIngredients = new LinkedHashSet<>();
        List<RxcuiIngredient> ingredientsToSave = new ArrayList<>();

        for (String ingredientName : apiIngredients) {
            if (ingredientName == null || ingredientName.isBlank()) {
                continue;
            }

            String normalizedIngredient = ingredientName.trim();
            String dedupeKey = normalizedIngredient.toLowerCase(Locale.ROOT);
            if (!uniqueIngredients.add(dedupeKey)) {
                continue;
            }

            RxcuiIngredient ingredient = new RxcuiIngredient();
            ingredient.setRxcui(rxcui);
            ingredient.setIngredientName(normalizedIngredient);
            ingredientsToSave.add(ingredient);
        }

        if (ingredientsToSave.isEmpty()) {
            return List.of();
        }

        rxcuiIngredientRepository.saveAll(ingredientsToSave);
        return ingredientsToSave.stream().map(RxcuiIngredient::getIngredientName).toList();
    }

    private String resolveOrCreateRxcui(String medicineName) {
        return rxNormMappingRepository.findByMedicineNameIgnoreCase(medicineName)
                .map(RxNormMapping::getRxcui)
                .filter(value -> value != null && !value.isBlank())
                .orElseGet(() -> {
                    String fetchedRxcui = rxNormClient.getRxCui(medicineName);
                    if (fetchedRxcui == null || fetchedRxcui.isBlank()) {
                        return null;
                    }

                    RxNormMapping mapping = new RxNormMapping();
                    mapping.setMedicineName(medicineName);
                    mapping.setRxcui(fetchedRxcui);
                    rxNormMappingRepository.save(mapping);
                    return fetchedRxcui;
                });
    }
}
