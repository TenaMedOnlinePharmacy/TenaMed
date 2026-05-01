package com.TenaMed.admin.dto;

import lombok.Data;

@Data
public class DashboardResponse {
    private long totalUsers;
    private long totalPharmacies;
    private long totalHospitals;
    private long totalProducts;
    private long totalOrders;
}
