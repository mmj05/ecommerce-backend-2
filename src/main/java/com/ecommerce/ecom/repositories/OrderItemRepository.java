package com.ecommerce.ecom.repositories;

import com.ecommerce.ecom.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    // Add this method for seller functionality
    List<OrderItem> findByProductProductIdIn(List<Long> productIds);
}