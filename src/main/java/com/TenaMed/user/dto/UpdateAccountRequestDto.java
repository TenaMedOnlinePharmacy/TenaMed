package com.TenaMed.user.dto;

import lombok.Data;
import java.util.Map;

@Data
public class UpdateAccountRequestDto {
    private String phone;
    private Map<String, Object> address;
}
