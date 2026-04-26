package com.nustnest.backend.notes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository

public interface NoteRatingRepository extends JpaRepository<NoteRating, Long> {

    boolean existsByNoteIdAndRatedById(Long noteId, Long userId);
    Optional<NoteRating> findByNoteIdAndRatedById(Long noteId, Long userId);
    java.util.List<NoteRating> findByNoteId(Long noteId);
}
