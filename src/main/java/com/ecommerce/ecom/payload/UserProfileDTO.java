package com.ecommerce.ecom.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {
    private Long userId;
    private String username;
    private String email;

    // Additional fields that could be added to the User model in the future
    private String firstName;
    private String lastName;
    private String phone;

    // Do not include password in the DTO for security reasons
}