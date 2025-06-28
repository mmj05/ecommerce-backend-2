package com.ecommerce.ecom.repositories;

import com.ecommerce.ecom.model.OrderItem;
import com.ecommerce.ecom.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    // Add this method for seller functionality
    List<OrderItem> findByProductProductIdIn(List<Long> productIds);

    // Check if at least one order item exists for the given product
    boolean existsByProduct(Product product);
}