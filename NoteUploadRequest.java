package com.nustnest.backend.notes;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data

public class NoteUploadRequest {

    @NotBlank(message = "Title is Required")
    @Size(max = 100, message = "Title must not exceed 100 characters")
    private String title;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotNull(message = "Subject is required")
    private Subject subject;
}
