package com.ecommerce.ecom.repositories;

import com.ecommerce.ecom.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Find orders by user email
    List<Order> findByEmail(String email);

    // Find orders by user email and order status
    List<Order> findByEmailAndOrderStatus(String email, String orderStatus);

    // Custom query to find orders with payment method
    @Query("SELECT o FROM Order o JOIN o.payment p WHERE o.email = ?1 AND p.paymentMethod = ?2")
    List<Order> findByEmailAndPaymentMethod(String email, String paymentMethod);
}