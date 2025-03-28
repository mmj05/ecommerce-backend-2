package com.ecommerce.ecom.service;

import com.ecommerce.ecom.payload.OrderDTO;

public interface OrderService {

    OrderDTO placeOrder(String email, Long addressId, String paymentMethod, String pgName, String pgPaymentId, String pgStatus, String pgResponseMessage);
}
