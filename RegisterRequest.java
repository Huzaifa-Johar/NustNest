package com.nustnest.backend.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data

public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@seecs\\.edu\\.pk$",
    message = "Only SEECS email addresses are allowed (@seecs.edu.pk")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be atleast 8 characters")
    private String password;
}
