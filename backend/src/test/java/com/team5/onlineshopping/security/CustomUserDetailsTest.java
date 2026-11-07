package com.team5.onlineshopping.security;

import com.team5.onlineshopping.common.enums.Role;
import com.team5.onlineshopping.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CustomUserDetails unit tests")
class CustomUserDetailsTest {

    private User activeCustomer;
    private User inactiveSeller;

    @BeforeEach
    void setUp() {
        activeCustomer = new User();
        activeCustomer.setId(1L);
        activeCustomer.setEmail("customer@example.com");
        activeCustomer.setPassword("$2a$10$hashedpassword");
        activeCustomer.setFullName("John Customer");
        activeCustomer.setRole(Role.CUSTOMER);
        activeCustomer.setActive(true);

        inactiveSeller = new User();
        inactiveSeller.setId(2L);
        inactiveSeller.setEmail("seller@example.com");
        inactiveSeller.setPassword("$2a$10$hashedpassword2");
        inactiveSeller.setFullName("Jane Seller");
        inactiveSeller.setRole(Role.SELLER);
        inactiveSeller.setActive(false);
    }

    @Test
    @DisplayName("Active customer has correct identity fields")
    void activeCustomer_hasCorrectFields() {
        CustomUserDetails details = new CustomUserDetails(activeCustomer);

        assertThat(details.getId()).isEqualTo(1L);
        assertThat(details.getEmail()).isEqualTo("customer@example.com");
        assertThat(details.getUsername()).isEqualTo("customer@example.com");
        assertThat(details.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("Authority is prefixed with ROLE_")
    void authority_isPrefixedWithROLE() {
        CustomUserDetails details = new CustomUserDetails(activeCustomer);

        assertThat(details.getAuthorities()).hasSize(1);
        assertThat(details.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("SELLER role has ROLE_SELLER authority")
    void sellerRole_hasCorrectAuthority() {
        CustomUserDetails details = new CustomUserDetails(inactiveSeller);

        assertThat(details.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_SELLER");
    }

    @Test
    @DisplayName("Active user: isEnabled and isAccountNonLocked return true")
    void activeUser_isEnabledAndNonLocked() {
        CustomUserDetails details = new CustomUserDetails(activeCustomer);

        assertThat(details.isEnabled()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("Inactive user: isEnabled and isAccountNonLocked return false")
    void inactiveUser_isDisabledAndLocked() {
        CustomUserDetails details = new CustomUserDetails(inactiveSeller);

        assertThat(details.isEnabled()).isFalse();
        assertThat(details.isAccountNonLocked()).isFalse();
    }

    @Test
    @DisplayName("Password is returned as-is (not re-encoded)")
    void password_isReturnedAsStored() {
        CustomUserDetails details = new CustomUserDetails(activeCustomer);

        assertThat(details.getPassword()).isEqualTo("$2a$10$hashedpassword");
    }

    @Test
    @DisplayName("All Role enum values produce ROLE_-prefixed authority")
    void allRoles_producePrefixedAuthorities() {
        for (Role role : Role.values()) {
            User user = new User();
            user.setId(99L);
            user.setEmail("test@example.com");
            user.setPassword("hash");
            user.setFullName("Test");
            user.setRole(role);
            user.setActive(true);

            CustomUserDetails details = new CustomUserDetails(user);
            String authority = details.getAuthorities().iterator().next().getAuthority();
            assertThat(authority).isEqualTo("ROLE_" + role.name());
        }
    }
}
