package com.TenaMed.hospital.mapper;

import com.TenaMed.hospital.dto.HospitalRequestDto;
import com.TenaMed.hospital.dto.HospitalResponseDto;
import com.TenaMed.hospital.entity.Hospital;
import org.springframework.stereotype.Component;

@Component
public class HospitalMapper {

    public Hospital toEntity(HospitalRequestDto dto) {
        Hospital entity = new Hospital();
        updateEntity(dto, entity);
        return entity;
    }

    public HospitalResponseDto toResponse(Hospital entity) {
        HospitalResponseDto dto = new HospitalResponseDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setLicenseNumber(entity.getLicenseNumber());
        dto.setLicenseImageUrl(entity.getLicenseImageUrl());
        dto.setOwnerId(entity.getOwnerId());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public void updateEntity(HospitalRequestDto dto, Hospital entity) {
        entity.setName(dto.getName());
        entity.setLicenseNumber(dto.getLicenseNumber());
        entity.setLicenseImageUrl(dto.getLicenseImageUrl());
    }
}
