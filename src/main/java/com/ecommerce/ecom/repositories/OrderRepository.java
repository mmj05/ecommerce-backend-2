package com.ecommerce.ecom.repositories;

import com.ecommerce.ecom.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Find orders by email, ordered by date (most recent first)
    List<Order> findByEmailOrderByOrderDateDesc(String email);

    // Find orders containing any of the specified products using a custom query with eager fetching
    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.product " +
           "LEFT JOIN FETCH o.payment " +
           "LEFT JOIN FETCH o.address " +
           "WHERE oi.product.productId IN :productIds")
    Page<Order> findOrdersContainingProducts(@Param("productIds") List<Long> productIds, Pageable pageable);
    
    // Find order by ID with all related entities fetched
    @Query("SELECT o FROM Order o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.product " +
           "LEFT JOIN FETCH o.payment " +
           "LEFT JOIN FETCH o.address " +
           "WHERE o.orderId = :orderId")
    Optional<Order> findByIdWithDetails(@Param("orderId") Long orderId);
}