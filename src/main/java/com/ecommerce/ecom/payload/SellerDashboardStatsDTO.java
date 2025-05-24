package com.ecommerce.ecom.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerDashboardStatsDTO {
    private Long totalProducts;
    private Long totalOrders;
    private Double totalSales;
    private Long totalCustomers;
}