package com.nustnest.backend.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data

public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@seecs\\.edu\\.pk$",
    message = "Only SEECS email addresses are allowed (@seecs.edu.pk")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
