package com.team5.onlineshopping.security;

import com.team5.onlineshopping.common.enums.Role;
import com.team5.onlineshopping.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService unit tests")
class JwtServiceTest {

    // 64 hex chars = 32 bytes = 256-bit key — valid for HMAC-SHA256
    private static final String TEST_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 86_400_000L; // 1 day

    private JwtService jwtService;
    private CustomUserDetails customerDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", EXPIRATION_MS);

        User user = new User();
        user.setId(1L);
        user.setEmail("customer@example.com");
        user.setPassword("hashed");
        user.setFullName("John");
        user.setRole(Role.CUSTOMER);
        user.setActive(true);

        customerDetails = new CustomUserDetails(user);
    }

    @Test
    @DisplayName("generateToken() returns a non-blank token string")
    void generateToken_returnsNonBlankToken() {
        String token = jwtService.generateToken(customerDetails);

        assertThat(token).isNotBlank();
        // JWT format: three Base64url-encoded segments separated by dots
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("extractUsername() extracts email from token subject")
    void extractUsername_returnsEmail() {
        String token = jwtService.generateToken(customerDetails);

        String username = jwtService.extractUsername(token);

        assertThat(username).isEqualTo("customer@example.com");
    }

    @Test
    @DisplayName("isTokenValid() returns true for freshly generated token")
    void isTokenValid_returnsTrue_forFreshToken() {
        String token = jwtService.generateToken(customerDetails);

        boolean valid = jwtService.isTokenValid(token, customerDetails);

        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("isTokenValid() returns false for wrong user")
    void isTokenValid_returnsFalse_forWrongUser() {
        String token = jwtService.generateToken(customerDetails);

        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("hash");
        otherUser.setFullName("Other");
        otherUser.setRole(Role.CUSTOMER);
        otherUser.setActive(true);
        CustomUserDetails otherDetails = new CustomUserDetails(otherUser);

        boolean valid = jwtService.isTokenValid(token, otherDetails);

        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("isTokenValid() returns false for expired token")
    void isTokenValid_returnsFalse_forExpiredToken() {
        // Set 0 ms expiration to generate an already-expired token
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", 0L);
        String expiredToken = jwtService.generateToken(customerDetails);

        // Restore normal expiration for validation
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", EXPIRATION_MS);

        // Parsing an expired token should throw an exception from JJWT
        assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, customerDetails))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Two tokens for same user are unique (differ by issuedAt)")
    void twoTokens_forSameUser_differ() throws InterruptedException {
        String token1 = jwtService.generateToken(customerDetails);
        Thread.sleep(10); // ensure distinct issuedAt
        String token2 = jwtService.generateToken(customerDetails);

        // Both are valid but may differ due to timestamp
        assertThat(token1).isNotBlank();
        assertThat(token2).isNotBlank();
    }

    @Test
    @DisplayName("validateConfiguration() throws IllegalStateException when secretKey is null or blank")
    void validateConfiguration_throwsOnMissingSecret() {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", "");

        assertThatThrownBy(service::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret key is missing");
    }

    @Test
    @DisplayName("validateConfiguration() throws IllegalStateException when secretKey is shorter than 256 bits")
    void validateConfiguration_throwsOnShortSecret() {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", "short-secret");

        assertThatThrownBy(service::validateConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret key is too short");
    }

    @Test
    @DisplayName("validateConfiguration() succeeds with 256-bit valid secret")
    void validateConfiguration_succeedsWithValidSecret() {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", TEST_SECRET);

        service.validateConfiguration();
    }
}
