package com.TenaMed.antidoping.client.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class RxNormRelatedResponseDto {

    private RelatedGroup relatedGroup;

    @Data
    @NoArgsConstructor
    public static class RelatedGroup {
        private String rxcui;
        private String tty;
        private List<ConceptGroup> conceptGroup;
    }

    @Data
    @NoArgsConstructor
    public static class ConceptGroup {
        private String tty;
        private List<ConceptProperty> conceptProperties;
    }

    @Data
    @NoArgsConstructor
    public static class ConceptProperty {
        private String rxcui;
        private String name;
    }
}
