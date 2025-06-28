package com.ecommerce.ecom.security;

import com.ecommerce.ecom.model.AppRole;
import com.ecommerce.ecom.model.Role;
import com.ecommerce.ecom.model.User;
import com.ecommerce.ecom.repositories.RoleRepository;
import com.ecommerce.ecom.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.ecommerce.ecom.security.jwt.AuthEntryPointJwt;
import com.ecommerce.ecom.security.jwt.AuthTokenFilter;
import com.ecommerce.ecom.security.services.UserDetailsServiceImpl;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {
    @Autowired
    UserDetailsServiceImpl userDetailsService;

    @Autowired
    private AuthEntryPointJwt unauthorizedHandler;

    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {
        return new AuthTokenFilter();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration configuration = new CorsConfiguration();
                    // Allow multiple localhost origins for development
                    configuration.setAllowedOrigins(Arrays.asList(
                            "http://localhost:5173",
                            "http://localhost:3000",
                            "http://127.0.0.1:5173",
                            "http://127.0.0.1:3000"
                    ));
                    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
                    configuration.setAllowedHeaders(Arrays.asList("*"));
                    configuration.setAllowCredentials(true);
                    configuration.setMaxAge(3600L);
                    // Expose headers that might be needed by the frontend
                    configuration.setExposedHeaders(Arrays.asList("Set-Cookie", "Authorization"));
                    return configuration;
                }))
                .csrf(csrf -> csrf.disable())
                .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth ->
                        // Allow unauthenticated CORS pre-flight requests
                        auth.requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                                .requestMatchers("/api/auth/**").permitAll()
                                .requestMatchers("/api/public/**").permitAll()
                                .requestMatchers("/v3/api-docs/**").permitAll()
                                .requestMatchers("/h2-console/**").permitAll()
                                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SELLER")
                                .requestMatchers("/api/seller/**").hasRole("SELLER")
                                .requestMatchers("/api/order/**").hasAnyRole("USER", "ADMIN", "SELLER")
                                .requestMatchers("/api/orders/**").hasAnyRole("USER", "ADMIN", "SELLER")
                                .requestMatchers("/api/profile/**").hasAnyRole("USER", "ADMIN", "SELLER")
                                .anyRequest().authenticated());

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/h2-console/**")
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html");
    }

    @Bean
    public CommandLineRunner initData(RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            // Initialize default roles if they don't exist
            if (roleRepository.findByRoleName(AppRole.ROLE_USER).isEmpty()) {
                roleRepository.save(new Role(AppRole.ROLE_USER));
            }
            if (roleRepository.findByRoleName(AppRole.ROLE_ADMIN).isEmpty()) {
                roleRepository.save(new Role(AppRole.ROLE_ADMIN));
            }
            if (roleRepository.findByRoleName(AppRole.ROLE_SELLER).isEmpty()) {
                roleRepository.save(new Role(AppRole.ROLE_SELLER));
            }

            // Create default admin user if not exists
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@admin.com");
                admin.setPassword(passwordEncoder.encode("admin123"));

                Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                        .orElseThrow(() -> new RuntimeException("Error: Admin Role is not found."));
                admin.setRoles(Set.of(adminRole));

                userRepository.save(admin);
            }
        };
    }
}