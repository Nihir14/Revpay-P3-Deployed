package com.revpay.service;

import com.revpay.exception.ResourceNotFoundException;
import com.revpay.exception.UnauthorizedException;
import com.revpay.exception.UserAlreadyExistsException;
import com.revpay.model.dto.ResetPasswordRequest;
import com.revpay.model.dto.SignupRequest;
import com.revpay.model.dto.UpdatePasswordRequest;
import com.revpay.model.dto.UpdatePinRequest;
import com.revpay.model.entity.Role;
import com.revpay.model.entity.User;
import com.revpay.repository.BusinessProfileRepository;
import com.revpay.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BusinessProfileRepository businessRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerUser_Success_Personal() {
        SignupRequest request = new SignupRequest();
        request.setEmail("test@example.com");
        request.setPhoneNumber("1234567890");
        request.setPassword("password");
        request.setTransactionPin("1234");
        request.setSecurityAnswer("answer");
        request.setRole("PERSONAL");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        User savedUser = new User();
        savedUser.setUserId(1L);
        savedUser.setRole(Role.PERSONAL);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        assertDoesNotThrow(() -> authService.registerUser(request));

        verify(userRepository, times(1)).save(any(User.class));
        verify(businessRepository, never()).save(any());
    }

    @Test
    void registerUser_EmailExists_ThrowsException() {
        SignupRequest request = new SignupRequest();
        request.setEmail("test@example.com");

        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> authService.registerUser(request));
    }

    @Test
    void registerUser_BusinessRole_Success() {
        SignupRequest request = new SignupRequest();
        request.setEmail("business@example.com");
        request.setPhoneNumber("0987654321");
        request.setPassword("password");
        request.setTransactionPin("1234");
        request.setSecurityAnswer("answer");
        request.setRole("BUSINESS");
        request.setBusinessName("My Business");
        request.setTaxId("TAX123");
        request.setAddress("123 Business St");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhoneNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        User savedUser = new User();
        savedUser.setUserId(2L);
        savedUser.setRole(Role.BUSINESS);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        assertDoesNotThrow(() -> authService.registerUser(request));

        verify(userRepository, times(1)).save(any(User.class));
        verify(businessRepository, times(1)).save(any());
    }

    @Test
    void getSecurityQuestion_Success() {
        User user = new User();
        user.setSecurityQuestion("What is your pet's name?");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));

        String question = authService.getSecurityQuestion("test@example.com");

        assertEquals("What is your pet's name?", question);
    }

    @Test
    void getSecurityQuestion_NotFound_ThrowsException() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.getSecurityQuestion("test@example.com"));
    }

    @Test
    void resetPassword_Success() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@example.com");
        request.setSecurityAnswer("answer");
        request.setNewPassword("newpass");

        User user = new User();
        user.setSecurityAnswerHash("hashedAnswer");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(passwordEncoder.encode(anyString())).thenReturn("newHashedPass");

        assertDoesNotThrow(() -> authService.resetPassword(request));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void resetPassword_InvalidAnswer_ThrowsException() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@example.com");
        request.setSecurityAnswer("wrongAnswer");

        User user = new User();
        user.setSecurityAnswerHash("hashedAnswer");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.resetPassword(request));
    }
}
