package com.nustnest.backend.notes;

import com.nustnest.backend.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(nullable = false)
    private double averageRating = 0.0;

    @Column(nullable = false)
    private int totalRatings = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static Note create(String title, String description, String filePath,String fileType,
                              Subject subject, User uploadedBy,long fileSize){

        Note note = new Note();
        note.title = title;
        note.description = description;
        note.filePath = filePath;
        note.fileType = fileType;
        note.subject = subject;
        note.uploadedBy = uploadedBy;
        note.fileSize = fileSize;
        note.averageRating = 0.0;
        note.totalRatings = 0;

        return note;
    }

    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
    }
}
