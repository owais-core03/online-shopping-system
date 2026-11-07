package com.team5.onlineshopping.health;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Slice test for HealthController.
 *
 * @WebMvcTest includes Spring Security auto-configuration, which scans
 * JwtAuthenticationFilter (@Component). That filter depends on JwtService
 * which is a @Service (excluded from the web slice by default). We mock
 * JwtService here so the ApplicationContext loads cleanly.
 *
 * UserDetailsServiceAutoConfiguration is excluded to prevent the auto-
 * registration of an in-memory UserDetailsService that would conflict with
 * our CustomUserDetailsService mock.
 *
 * @WithMockUser supplies a pre-authenticated principal so the request
 * passes Spring Security and reaches the controller.
 */
@WebMvcTest(
    value = HealthController.class,
    excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class
)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    @DisplayName("GET /api/health should return 200 OK and status UP without requiring authentication")
    @WithMockUser
    void healthCheckShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("online-shopping-system"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }
}
