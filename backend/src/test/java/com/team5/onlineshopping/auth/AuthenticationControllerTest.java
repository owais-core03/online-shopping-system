package com.team5.onlineshopping.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team5.onlineshopping.auth.controller.AuthenticationController;
import com.team5.onlineshopping.auth.dto.AuthenticationResponse;
import com.team5.onlineshopping.auth.dto.LoginRequest;
import com.team5.onlineshopping.auth.dto.RegistrationRequest;
import com.team5.onlineshopping.auth.service.AuthenticationService;
import com.team5.onlineshopping.common.enums.Role;
import com.team5.onlineshopping.common.exception.BadRequestException;
import com.team5.onlineshopping.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    value = AuthenticationController.class,
    excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
@DisplayName("AuthenticationController Slice Tests")
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/auth/register with valid request returns 201 Created and token")
    @WithMockUser
    void registerWithValidPayloadReturnsCreated() throws Exception {
        RegistrationRequest request = new RegistrationRequest(
                "john.doe@example.com",
                "SecurePassword123!",
                "John Doe",
                "+1234567890"
        );
        AuthenticationResponse response = new AuthenticationResponse(
                "mock-jwt-token",
                1L,
                "john.doe@example.com",
                "John Doe",
                Role.CUSTOMER
        );

        when(authenticationService.register(any(RegistrationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.data.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("POST /api/auth/register with duplicate email returns 400 Bad Request")
    @WithMockUser
    void registerWithExistingEmailReturnsBadRequest() throws Exception {
        RegistrationRequest request = new RegistrationRequest(
                "existing@example.com",
                "SecurePassword123!",
                "Existing User",
                null
        );

        when(authenticationService.register(any(RegistrationRequest.class)))
                .thenThrow(new BadRequestException("Email is already registered: existing@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already registered: existing@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/login with valid credentials returns 200 OK and token")
    @WithMockUser
    void loginWithValidCredentialsReturnsToken() throws Exception {
        LoginRequest request = new LoginRequest("john.doe@example.com", "SecurePassword123!");
        AuthenticationResponse response = new AuthenticationResponse(
                "login-jwt-token",
                1L,
                "john.doe@example.com",
                "John Doe",
                Role.CUSTOMER
        );

        when(authenticationService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").value("login-jwt-token"))
                .andExpect(jsonPath("$.data.userId").value(1L));
    }

    @Test
    @DisplayName("POST /api/auth/login with invalid credentials returns 400 Bad Request")
    @WithMockUser
    void loginWithInvalidCredentialsReturnsBadRequest() throws Exception {
        LoginRequest request = new LoginRequest("john.doe@example.com", "WrongPassword");

        when(authenticationService.login(any(LoginRequest.class)))
                .thenThrow(new BadRequestException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
