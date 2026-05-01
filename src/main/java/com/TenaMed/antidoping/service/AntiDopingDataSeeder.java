package com.TenaMed.antidoping.service;

import com.TenaMed.antidoping.entity.BannedSubstance;
import com.TenaMed.antidoping.entity.BannedSubstanceStatus;
import com.TenaMed.antidoping.entity.IngredientAlias;
import com.TenaMed.antidoping.repository.BannedSubstanceRepository;
import com.TenaMed.antidoping.repository.IngredientAliasRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AntiDopingDataSeeder {

    private final BannedSubstanceRepository bannedSubstanceRepository;
    private final IngredientAliasRepository ingredientAliasRepository;

    @PostConstruct
    public void seed() {
        if (bannedSubstanceRepository.count() == 0) {
            log.info("Seeding initial WADA dataset...");

            seedBannedSubstance("pseudoephedrine", "S6 Stimulants", BannedSubstanceStatus.RESTRICTED);
            seedBannedSubstance("ephedrine", "S6 Stimulants", BannedSubstanceStatus.RESTRICTED);
            seedBannedSubstance("dexamethasone", "S9 Glucocorticoids", BannedSubstanceStatus.BANNED);
            seedBannedSubstance("prednisone", "S9 Glucocorticoids", BannedSubstanceStatus.BANNED);
            seedBannedSubstance("furosemide", "S5 Diuretics", BannedSubstanceStatus.BANNED);
            seedBannedSubstance("hydrochlorothiazide", "S5 Diuretics", BannedSubstanceStatus.BANNED);
            seedBannedSubstance("testosterone", "S1 Anabolic Agents", BannedSubstanceStatus.BANNED);
        }

        if (ingredientAliasRepository.count() == 0) {
            log.info("Seeding ingredient aliases...");

            seedAlias("pseudoephedrine hydrochloride", "pseudoephedrine");
            seedAlias("dexamethasone sodium phosphate", "dexamethasone");
            seedAlias("prednisolone", "prednisone");
        }
    }

    private void seedBannedSubstance(String ingredient, String category, BannedSubstanceStatus status) {
        BannedSubstance substance = new BannedSubstance();
        substance.setIngredientName(ingredient);
        substance.setCategory(category);
        substance.setStatus(status);
        substance.setRuleset("WADA");
        substance.setRulesetYear(2026); // current or specific year
        bannedSubstanceRepository.save(substance);
    }

    private void seedAlias(String aliasName, String canonicalName) {
        IngredientAlias alias = new IngredientAlias();
        alias.setAliasName(aliasName);
        alias.setCanonicalName(canonicalName);
        ingredientAliasRepository.save(alias);
    }
}
