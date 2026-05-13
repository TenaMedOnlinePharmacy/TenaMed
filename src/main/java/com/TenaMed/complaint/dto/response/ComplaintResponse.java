package com.TenaMed.complaint.dto.response;

import com.TenaMed.complaint.enums.ComplaintCategory;
import com.TenaMed.complaint.enums.ComplaintStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class ComplaintResponse {

    private UUID id;
    private UUID customerId;
    private UUID orderId;
    private ComplaintCategory category;
    private String subject;
    private String description;
    private ComplaintStatus status;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
