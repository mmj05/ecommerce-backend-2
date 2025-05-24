package com.ecommerce.ecom.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long orderId;
    private String email;
    private List<OrderItemDTO> orderItemDTOs = new ArrayList<>();
    private LocalDate orderDate;
    private PaymentDTO paymentDTO;
    private AddressDTO addressDTO;
    private Double totalAmount;
    private String orderStatus;
    private Long addressId;
    private Double sellerTotal; // Total amount for seller's products only
}