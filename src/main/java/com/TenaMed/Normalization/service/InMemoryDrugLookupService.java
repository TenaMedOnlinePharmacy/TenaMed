package com.TenaMed.Normalization.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class InMemoryDrugLookupService implements DrugLookupService {

    private static final List<String> STANDARD_DRUGS = List.of(
            "Metformin",
            "Amoxicillin",
            "Paracetamol",
            "Ibuprofen",
            "Atorvastatin",
            "Omeprazole",
            "Amlodipine",
            "Losartan"
    );

    private static final Map<String, String> SYNONYMS = Map.ofEntries(
            Map.entry("acetaminophen", "Paracetamol"),
            Map.entry("tylenol", "Paracetamol"),
            Map.entry("glucophage", "Metformin"),
            Map.entry("amox", "Amoxicillin"),
            Map.entry("lipitor", "Atorvastatin"),
            Map.entry("norvasc", "Amlodipine"),
            Map.entry("cozaar", "Losartan")
    );

    @Override
    public List<String> getStandardDrugNames() {
        return STANDARD_DRUGS;
    }

    @Override
    public Map<String, String> getSynonymMappings() {
        return SYNONYMS;
    }
}
