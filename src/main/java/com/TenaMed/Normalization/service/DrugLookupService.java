package com.TenaMed.Normalization.service;

import java.util.List;
import java.util.Map;

public interface DrugLookupService {
    List<String> getStandardDrugNames();

    Map<String, String> getSynonymMappings();
}
