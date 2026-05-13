package com.TenaMed.complaint.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddAdminNoteRequest {

    @NotBlank
    private String adminNote;
}
