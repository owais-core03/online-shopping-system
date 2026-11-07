package com.team5.onlineshopping.common;

import com.team5.onlineshopping.common.dto.ApiErrorResponse;
import com.team5.onlineshopping.common.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiResponse and ApiErrorResponse unit tests")
class ApiResponseTest {

    @Test
    @DisplayName("ApiResponse.success(message, data) wraps data with success=true and message")
    void successResponse_hasCorrectFields() {
        ApiResponse<String> response = ApiResponse.success("Operation completed", "Hello");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Operation completed");
        assertThat(response.getData()).isEqualTo("Hello");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("ApiResponse.success(data) with non-String type wraps correctly")
    void successResponse_withNonStringData_wrapsCorrectly() {
        ApiResponse<Integer> response = ApiResponse.success(42);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isNotBlank();
        assertThat(response.getData()).isEqualTo(42);
    }

    @Test
    @DisplayName("ApiResponse.success(message) produces null data")
    void successResponse_withMessageOnly_hasNullData() {
        ApiResponse<Void> response = ApiResponse.success("Done");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("Done");
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("ApiErrorResponse stores errorCode and message correctly")
    void errorResponse_hasCorrectFields() {
        ApiErrorResponse error = new ApiErrorResponse("BAD_REQUEST", "Validation failed");

        assertThat(error.isSuccess()).isFalse();
        assertThat(error.getErrorCode()).isEqualTo("BAD_REQUEST");
        assertThat(error.getMessage()).isEqualTo("Validation failed");
        assertThat(error.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("ApiErrorResponse.of() factory method with details list works")
    void errorResponse_withDetails_hasDetailsList() {
        ApiErrorResponse error = ApiErrorResponse.of(
                "VALIDATION_ERROR",
                "One or more fields are invalid",
                List.of("email: must not be blank", "password: too short")
        );

        assertThat(error.isSuccess()).isFalse();
        assertThat(error.getDetails()).hasSize(2);
        assertThat(error.getDetails()).contains("email: must not be blank");
    }

    @Test
    @DisplayName("Two ApiResponse objects have independent timestamps")
    void twoResponses_haveTimestamps() {
        ApiResponse<Integer> r1 = ApiResponse.success(1);
        ApiResponse<Integer> r2 = ApiResponse.success(2);

        assertThat(r1.getTimestamp()).isNotNull();
        assertThat(r2.getTimestamp()).isNotNull();
        assertThat(r1.getData()).isEqualTo(1);
        assertThat(r2.getData()).isEqualTo(2);
    }
}
