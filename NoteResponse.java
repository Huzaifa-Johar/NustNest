package com.nustnest.backend.notes;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class NoteResponse {

    private Long id;
    private String title;
    private String description;
    private String fileType;
    private long fileSize;
    private Subject subject;
    private String uploadedByName;
    private double averageRating;
    private int totalRatings;
    private Integer userRating;
    private LocalDateTime createdAt;
}
