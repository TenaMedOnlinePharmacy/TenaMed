package com.TenaMed.complaint.dto.request;

import com.TenaMed.complaint.enums.ComplaintCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateComplaintRequest {

    @NotNull
    private UUID orderId;

    @NotNull
    private ComplaintCategory category;

    @NotBlank
    private String subject;

    @NotBlank
    private String description;
}
