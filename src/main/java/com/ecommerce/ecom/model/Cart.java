package com.ecommerce.ecom.model;

import jakarta.persistence.*;
import lombok.*; // Import EqualsAndHashCode specifically if not using wildcard

import java.util.ArrayList;
import java.util.List;

@Entity
@Data // Includes @EqualsAndHashCode by default
@Table(name = "carts")
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartId;

    @OneToOne
    @JoinColumn(name = "user_id")
    @EqualsAndHashCode.Exclude // *** Add this exclusion ***
    private User user;

    @OneToMany(mappedBy = "cart", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE})
    @EqualsAndHashCode.Exclude // Exclude collection
    private List<CartItem> cartItems = new ArrayList<>();

    private Double totalPrice;
}