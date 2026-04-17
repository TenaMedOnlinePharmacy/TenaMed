package com.TenaMed.antidoping.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DopingCheckRequest {

    @NotBlank(message = "medicineName is required")
    private String medicineName;
}
