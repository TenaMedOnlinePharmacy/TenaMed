package com.TenaMed.inventory.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class BatchEditDetailsResponse {

    private UUID batchId;
    private AddBatchRequest batch;
    private String imageUrl;
}
