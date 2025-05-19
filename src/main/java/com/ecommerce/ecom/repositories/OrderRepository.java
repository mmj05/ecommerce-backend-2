// Updated src/main/java/com/ecommerce/ecom/repositories/OrderRepository.java
package com.ecommerce.ecom.repositories;

import com.ecommerce.ecom.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Find orders by email, ordered by date (most recent first)
    List<Order> findByEmailOrderByOrderDateDesc(String email);
}