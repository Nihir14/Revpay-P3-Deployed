package com.revpay.controller;

import com.revpay.exception.ResourceNotFoundException;
import com.revpay.model.dto.ApiResponse;
import com.revpay.model.entity.User;
import com.revpay.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Operations", description = "Endpoints for user profile and lookup")
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/resolve")
    @Operation(summary = "Resolve user ID from email or phone", description = "Internal/Public endpoint to find a user's ID using their unique identifier.")
    public ResponseEntity<ApiResponse<Long>> resolveUser(@RequestParam String identifier) {
        log.debug("Resolving identifier: {}", identifier);
        
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByPhoneNumber(identifier))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with identifier: " + identifier));
                
        return ResponseEntity.ok(ApiResponse.success(user.getUserId(), "User resolved"));
    }

    @GetMapping("/{userId}/name")
    public String getUserName(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(User::getFullName)
                .orElse("Unknown User");
    }
}
