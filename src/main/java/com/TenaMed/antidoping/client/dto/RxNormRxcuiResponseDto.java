package com.TenaMed.antidoping.client.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class RxNormRxcuiResponseDto {

    private IdGroup idGroup;

    @Data
    @NoArgsConstructor
    public static class IdGroup {
        private String name;
        private List<String> rxnormId;
    }
}
