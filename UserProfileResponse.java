package com.nustnest.backend.user;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class UserProfileResponse {

    private Long id;
    private String fullName;
    private String email;
    private String bio;
    private String profileImagePath;
    private boolean emailVerified;
    private LocalDateTime createdAt;
}
