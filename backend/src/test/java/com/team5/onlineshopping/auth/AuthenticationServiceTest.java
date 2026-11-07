package com.team5.onlineshopping.auth;

import com.team5.onlineshopping.auth.dto.AuthenticationResponse;
import com.team5.onlineshopping.auth.dto.LoginRequest;
import com.team5.onlineshopping.auth.dto.RegistrationRequest;
import com.team5.onlineshopping.auth.service.AuthenticationService;
import com.team5.onlineshopping.common.enums.Role;
import com.team5.onlineshopping.common.exception.BadRequestException;
import com.team5.onlineshopping.security.JwtService;
import com.team5.onlineshopping.user.entity.User;
import com.team5.onlineshopping.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService unit tests")
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthenticationService authenticationService;

    private RegistrationRequest validRegistration;
    private LoginRequest validLogin;

    @BeforeEach
    void setUp() {
        validRegistration = new RegistrationRequest();
        validRegistration.setEmail("customer@example.com");
        validRegistration.setPassword("SecurePass123!");
        validRegistration.setFullName("John Customer");
        validRegistration.setPhoneNumber("9876543210");

        validLogin = new LoginRequest();
        validLogin.setEmail("customer@example.com");
        validLogin.setPassword("SecurePass123!");
    }

    // ─── Registration Tests ────────────────────────────────────────────────────

    @Test
    @DisplayName("register() with new email creates customer and returns JWT")
    void register_withNewEmail_returnsJwt() {
        when(userRepository.existsByEmail("customer@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("$2a$10$bcryptHash");

        User savedUser = buildUser(1L, "customer@example.com", "$2a$10$bcryptHash",
                "John Customer", Role.CUSTOMER, true);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any())).thenReturn("mock.jwt.token");

        AuthenticationResponse response = authenticationService.register(validRegistration);

        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getEmail()).isEqualTo("customer@example.com");
        assertThat(response.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(response.getUserId()).isEqualTo(1L);

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("SecurePass123!");
        verify(jwtService).generateToken(any());
    }

    @Test
    @DisplayName("register() always assigns CUSTOMER role regardless of request")
    void register_alwaysAssignsCustomerRole() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        User savedUser = buildUser(1L, "customer@example.com", "hashed",
                "John Customer", Role.CUSTOMER, true);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any())).thenReturn("token");

        AuthenticationResponse response = authenticationService.register(validRegistration);

        assertThat(response.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("register() normalizes email to lowercase")
    void register_normalizesEmailToLowercase() {
        RegistrationRequest request = new RegistrationRequest();
        request.setEmail("  CUSTOMER@EXAMPLE.COM  ");
        request.setPassword("pass");
        request.setFullName("Test");

        when(userRepository.existsByEmail("customer@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        User savedUser = buildUser(1L, "customer@example.com", "hashed", "Test", Role.CUSTOMER, true);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any())).thenReturn("token");

        authenticationService.register(request);

        verify(userRepository).existsByEmail("customer@example.com");
    }

    @Test
    @DisplayName("register() with duplicate email throws BadRequestException")
    void register_withDuplicateEmail_throwsBadRequestException() {
        when(userRepository.existsByEmail("customer@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authenticationService.register(validRegistration))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("register() never stores plain-text password")
    void register_neverStoresPlainTextPassword() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("$2a$10$hash");

        User savedUser = buildUser(1L, "customer@example.com", "$2a$10$hash",
                "John Customer", Role.CUSTOMER, true);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any())).thenReturn("token");

        authenticationService.register(validRegistration);

        // Capture saved user and verify password was encoded
        verify(userRepository).save(argThat(user ->
                user.getPassword() != null &&
                user.getPassword().equals("$2a$10$hash") &&
                !user.getPassword().equals("SecurePass123!")
        ));
    }

    // ─── Login Tests ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("login() with correct credentials returns JWT")
    void login_withCorrectCredentials_returnsJwt() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null); // authenticate() returns Authentication; null means no exception = success

        User user = buildUser(1L, "customer@example.com", "hash", "John", Role.CUSTOMER, true);
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("mock.jwt.token");

        AuthenticationResponse response = authenticationService.login(validLogin);

        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getEmail()).isEqualTo("customer@example.com");
        assertThat(response.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("login() with wrong password throws BadRequestException")
    void login_withWrongPassword_throwsBadRequestException() {
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThatThrownBy(() -> authenticationService.login(validLogin))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid email or password");

        verify(userRepository, never()).findByEmail(anyString());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("login() with inactive account throws BadRequestException")
    void login_withInactiveAccount_throwsBadRequestException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);

        User inactiveUser = buildUser(2L, "customer@example.com", "hash", "Jane", Role.CUSTOMER, false);
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> authenticationService.login(validLogin))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("deactivated");

        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("login() normalizes email to lowercase")
    void login_normalizesEmailToLowercase() {
        LoginRequest request = new LoginRequest();
        request.setEmail("  CUSTOMER@EXAMPLE.COM  ");
        request.setPassword("pass");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);

        User user = buildUser(1L, "customer@example.com", "hash", "John", Role.CUSTOMER, true);
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("token");

        authenticationService.login(request);

        verify(userRepository).findByEmail("customer@example.com");
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private User buildUser(Long id, String email, String password,
                           String fullName, Role role, boolean active) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setPassword(password);
        user.setFullName(fullName);
        user.setRole(role);
        user.setActive(active);
        return user;
    }
}
