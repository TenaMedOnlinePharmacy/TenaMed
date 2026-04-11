package com.TenaMed.pharmacy.dto.request;

import com.TenaMed.pharmacy.enums.StaffRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AcceptOrderRequest {

    @NotNull
    private UUID actorUserId;

    @NotNull
    private StaffRole actorRole;
}