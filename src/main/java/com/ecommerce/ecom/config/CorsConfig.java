package com.ecommerce.ecom.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Allow specific origins (add all possible development URLs)
        config.addAllowedOrigin("http://localhost:5173"); // Vite's default port
        config.addAllowedOrigin("http://localhost:3000");  // React's default port
        config.addAllowedOrigin("http://127.0.0.1:5173");  // Alternative localhost
        config.addAllowedOrigin("http://127.0.0.1:3000");  // Alternative localhost

        // Allow all HTTP methods
        config.addAllowedMethod("*");

        // Allow all headers (including custom headers)
        config.addAllowedHeader("*");

        // CRITICAL: Allow credentials (cookies, authorization headers, etc.)
        config.setAllowCredentials(true);

        // Set max age for preflight cache
        config.setMaxAge(3600L);

        // Allow specific headers in responses
        config.addExposedHeader("Set-Cookie");
        config.addExposedHeader("Authorization");

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}