package com.TenaMed.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignRoleRequestDto {

    @NotBlank
    private String roleName;
}
