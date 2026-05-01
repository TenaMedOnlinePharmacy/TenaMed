package com.TenaMed.antidoping.service;

import com.TenaMed.antidoping.entity.IngredientAlias;
import com.TenaMed.antidoping.repository.IngredientAliasRepository;
import com.TenaMed.antidoping.util.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IngredientAliasService {

    private final IngredientAliasRepository aliasRepository;

    public String resolveCanonical(String ingredient) {
        String normalized = TextNormalizer.normalize(ingredient);
        if (normalized == null) {
            return ingredient;
        }

        Optional<IngredientAlias> alias = aliasRepository.findByNormalizedAlias(normalized);

        return alias.map(IngredientAlias::getCanonicalName)
                    .orElse(ingredient);
    }
}
