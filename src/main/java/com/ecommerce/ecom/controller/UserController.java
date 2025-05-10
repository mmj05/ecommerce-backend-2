package com.ecommerce.ecom.controller;

import com.ecommerce.ecom.exceptions.APIException;
import com.ecommerce.ecom.exceptions.ResourceNotFoundException;
import com.ecommerce.ecom.model.User;
import com.ecommerce.ecom.payload.APIResponse;
import com.ecommerce.ecom.payload.UserProfileDTO;
import com.ecommerce.ecom.payload.PasswordChangeRequest;
import com.ecommerce.ecom.repositories.UserRepository;
import com.ecommerce.ecom.util.AuthUtil;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDTO> getUserProfile() {
        logger.info("Request received for user profile");

        try {
            User user = authUtil.loggedInUser();
            if (user == null) {
                logger.error("User not found");
                throw new ResourceNotFoundException("User", "id", "current");
            }

            logger.info("User found: {}", user.getUsername());
            UserProfileDTO userProfileDTO = modelMapper.map(user, UserProfileDTO.class);
            return new ResponseEntity<>(userProfileDTO, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("Error retrieving user profile", e);
            throw e;
        }
    }

    @PutMapping("/update")
    public ResponseEntity<UserProfileDTO> updateUserProfile(@RequestBody UserProfileDTO userProfileDTO) {
        logger.info("Request received to update user profile: {}", userProfileDTO.getUsername());

        User user = authUtil.loggedInUser();
        if (user == null) {
            logger.error("User not found for update");
            throw new ResourceNotFoundException("User", "id", "current");
        }

        // Check if username is being changed and if it's already taken
        if (!user.getUsername().equals(userProfileDTO.getUsername()) &&
                userRepository.existsByUsername(userProfileDTO.getUsername())) {
            logger.warn("Username already taken: {}", userProfileDTO.getUsername());
            throw new APIException("Username already taken");
        }

        // Check if email is being changed and if it's already taken
        if (!user.getEmail().equals(userProfileDTO.getEmail()) &&
                userRepository.existsByEmail(userProfileDTO.getEmail())) {
            logger.warn("Email already in use: {}", userProfileDTO.getEmail());
            throw new APIException("Email already in use");
        }

        // Update user fields
        logger.info("Updating user {} from {} to {}",
                user.getUserId(), user.getUsername(), userProfileDTO.getUsername());

        user.setUsername(userProfileDTO.getUsername());
        user.setEmail(userProfileDTO.getEmail());

        // Update additional fields if present in your User model
        // This is a placeholder for future functionality
        if (userProfileDTO.getFirstName() != null) {
            // user.setFirstName(userProfileDTO.getFirstName());
            logger.info("First name would be updated if implemented");
        }
        if (userProfileDTO.getLastName() != null) {
            // user.setLastName(userProfileDTO.getLastName());
            logger.info("Last name would be updated if implemented");
        }
        if (userProfileDTO.getPhone() != null) {
            // user.setPhone(userProfileDTO.getPhone());
            logger.info("Phone would be updated if implemented");
        }

        User updatedUser = userRepository.save(user);
        logger.info("User updated successfully: {}", updatedUser.getUsername());

        UserProfileDTO updatedProfile = modelMapper.map(updatedUser, UserProfileDTO.class);
        return new ResponseEntity<>(updatedProfile, HttpStatus.OK);
    }

    @PostMapping("/change-password")
    public ResponseEntity<APIResponse> changePassword(@RequestBody PasswordChangeRequest passwordChangeRequest) {
        logger.info("Request received to change password");

        User user = authUtil.loggedInUser();
        if (user == null) {
            logger.error("User not found for password change");
            throw new ResourceNotFoundException("User", "id", "current");
        }

        // Verify old password
        if (!passwordEncoder.matches(passwordChangeRequest.getOldPassword(), user.getPassword())) {
            logger.warn("Current password is incorrect for user: {}", user.getUsername());
            throw new APIException("Current password is incorrect");
        }

        // Update password
        logger.info("Updating password for user: {}", user.getUsername());
        user.setPassword(passwordEncoder.encode(passwordChangeRequest.getNewPassword()));
        userRepository.save(user);

        logger.info("Password changed successfully for user: {}", user.getUsername());
        return new ResponseEntity<>(new APIResponse("Password changed successfully", true), HttpStatus.OK);
    }
}