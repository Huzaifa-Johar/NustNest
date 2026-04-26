package com.nustnest.backend.user;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data

public class UpdateBioRequest {

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;


}
