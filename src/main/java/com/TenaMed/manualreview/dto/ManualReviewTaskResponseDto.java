package com.TenaMed.manualreview.dto;

import com.TenaMed.manualreview.entity.TaskPriority;
import com.TenaMed.manualreview.entity.TaskReason;
import com.TenaMed.manualreview.entity.TaskStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class ManualReviewTaskResponseDto {
    UUID id;
    UUID prescriptionId;
    TaskStatus status;
    TaskReason reason;
    TaskPriority priority;
    UUID assignedTo;
    String notes;
    String imageUrl;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime completedAt;
}
