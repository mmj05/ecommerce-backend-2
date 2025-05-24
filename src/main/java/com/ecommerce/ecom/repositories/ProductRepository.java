package com.ecommerce.ecom.repositories;

import com.ecommerce.ecom.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByCategoryOrderByPriceAsc(Category category, Pageable pageable);
    Page<Product> findByProductNameLikeIgnoreCase(String keyword, Pageable pageable);

    // Find products by user/seller
    Page<Product> findByUser(User user, Pageable pageable);

    boolean existsByProductName(String productName);

    List<Product> findByUser(User user);
}