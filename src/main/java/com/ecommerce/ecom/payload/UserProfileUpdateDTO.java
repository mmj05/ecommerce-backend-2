// src/main/java/com/ecommerce/ecom/payload/UserProfileUpdateDTO.java
package com.ecommerce.ecom.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateDTO {
    private String email;
    // Can add more fields as needed for profile updates
}