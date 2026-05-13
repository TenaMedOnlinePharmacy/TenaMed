package com.TenaMed.complaint.dto.request;

import com.TenaMed.complaint.enums.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateComplaintStatusRequest {

    @NotNull
    private ComplaintStatus status;
}
