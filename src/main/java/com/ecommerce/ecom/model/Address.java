package com.ecommerce.ecom.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long addressId;

    @NotBlank
    @Size(min = 5, message = "Street should have atleast 5 characters")
    private String street;

    private String apartmentNumber;

    @NotBlank
    @Size(min = 4, message = "City name should have atleast 4 characters")
    private String city;

    @NotBlank
    @Size(min = 2, message = "State name should have atleast 2 characters")
    private String state;

    @NotBlank
    @Size(min = 2, message = "Country name should have atleast 2 characters")
    private String country;

    @NotBlank
    @Size(min = 5, message = "Zip code should have atleast 5 characters")
    private String zipCode;

    @ToString.Exclude
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public Address(String zipCode, String country, String state, String city, String apartmentNumber, String street) {
        this.zipCode = zipCode;
        this.country = country;
        this.state = state;
        this.city = city;
        this.apartmentNumber = apartmentNumber;
        this.street = street;
    }
}
