package com.nustnest.backend.user;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data

public class UpdatePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "Password must be 8 characters long")
    private String newPassword;

    @NotBlank(message = "Please confirm your new password")
    @Size(min = 8, message = "Confirm password must be 8 characters long")
    private String confirmPassword;
}
