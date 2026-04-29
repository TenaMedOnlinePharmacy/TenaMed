package com.TenaMed.doctor.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class EditDoctorPrescriptionItemsRequestDto {

    @NotEmpty
    private List<@Valid DoctorPrescriptionItemRequestDto> items;
}

