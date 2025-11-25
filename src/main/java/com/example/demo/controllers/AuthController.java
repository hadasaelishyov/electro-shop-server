package com.example.demo.controllers;

import com.example.demo.entities.User;
import com.example.demo.entities.UserRole;
import com.example.demo.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller for authentication-related endpoints including login, registration,
 * and password management.
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:5173")

public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * User login endpoint
     * @param loginData Map containing email and password for authentication
     * @return User data on successful login or error message
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> loginData) {
        String email = loginData.get("email");
        String password = loginData.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body("Email and password are required");
        }

        boolean isAuthenticated = userService.authenticate(email, password);

        if (isAuthenticated) {
            Optional<User> userOpt = userService.getByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();

                if (!user.isActive()) {
                    return ResponseEntity.status(401).body("Account is deactivated. Please contact support.");
                }

                Map<String, Object> response = new HashMap<>();
                response.put("id", user.getId());
                response.put("name", user.getUsername());
                response.put("email", user.getEmail());
                response.put("role", user.getRole());
                response.put("active", user.isActive());

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(404).body("User not found");
            }
        } else {
            return ResponseEntity.status(401).body("Invalid email or password");
        }
    }

    /**
     * User registration endpoint
     * @param userData User registration data as a map
     * @return Success message or error details
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> userData) {
        try {
            // Validate input data
            String email = (String) userData.get("email");
            String password = (String) userData.get("password");
            String username = (String) userData.get("username");
            String phone = (String) userData.get("phone");
            String address = (String) userData.get("address");

            if (email == null || email.isEmpty()) {
                return ResponseEntity.badRequest().body("Email is required");
            }

            if (password == null || password.isEmpty()) {
                return ResponseEntity.badRequest().body("Password is required");
            }

            if (username == null || username.isEmpty()) {
                return ResponseEntity.badRequest().body("Username is required");
            }

            // Check if email already exists
            if (userService.getByEmail(email).isPresent()) {
                return ResponseEntity.badRequest().body("Email already in use");
            }

            // Check if username already exists
            if (userService.getByUsername(username).isPresent()) {
                return ResponseEntity.badRequest().body("Username already in use");
            }

            // Create user entity directly
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setPassword(password);
            newUser.setUsername(username);
            newUser.setPhone(phone);
            newUser.setAddress(address);
            newUser.setRole(UserRole.USER); // Default role for registration

            User user = userService.register(newUser);

            if (user != null) {
                // Return basic user info without sensitive data
                Map<String, Object> response = new HashMap<>();
                response.put("message", "User registered successfully");
                response.put("userId", user.getId());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Registration failed");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during registration");
        }
    }

    /**
     * Request password reset endpoint
     * @param email User's email address
     * @return Success message regardless of whether email exists (for security)
     */
    @PostMapping("/password/reset-request")
    public ResponseEntity<?> requestPasswordReset(@RequestParam String email) {
        try {
            String resetToken = userService.generatePasswordResetToken(email);
            // In a real app, you would send an email with the reset token/link
            // For security, we always return success even if email doesn't exist
            return ResponseEntity.ok("If your email exists in our system, you will receive reset instructions");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred processing your request");
        }
    }

    /**
     * Complete password reset with token
     * @param resetData Contains reset token and new password
     * @return Success or error message
     */
    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> resetData) {
        String token = resetData.get("token");
        String newPassword = resetData.get("password");

        if (token == null || token.isEmpty() || newPassword == null || newPassword.isEmpty()) {
            return ResponseEntity.badRequest().body("Token and password are required");
        }

        // This would validate the token and update the password
        // Not implemented in the current UserService
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body("Password reset functionality not implemented");
    }

    /**
     * Check if user is authenticated (for frontend session validation)
     * @return User data if authenticated or error
     */
    @GetMapping("/check")
    public ResponseEntity<?> checkAuthStatus(@RequestParam Long userId) {
        try {
            Optional<User> userOpt = userService.getById(userId);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                if (!user.isActive()) {
                    return ResponseEntity.status(401).body("Account is deactivated");
                }

                Map<String, Object> response = new HashMap<>();
                response.put("id", user.getId());
                response.put("name", user.getUsername());
                response.put("email", user.getEmail());
                response.put("role", user.getRole());

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(404).body("User not found");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error checking authentication status");
        }
    }
}