package com.ecommerce.ecom.security.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String password;

    // Add remember me functionality
    private Boolean rememberMe = false;
}