package com.nustnest.backend.notes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository

public interface NoteRepository extends JpaRepository<Note, Long>{

    List<Note> findAllByOrderByAverageRatingDesc();
    List<Note> findAllByOrderByAverageRatingAsc();
    List<Note> findAllByOrderByCreatedAtDesc();
    List<Note> findAllByOrderByCreatedAtAsc();

    List<Note> findBySubjectOrderByAverageRatingDesc(Subject subject);
    List<Note> findBySubjectOrderByAverageRatingAsc(Subject subject);
    List<Note> findBySubjectOrderByCreatedAtDesc(Subject subject);
    List<Note> findBySubjectOrderByCreatedAtAsc(Subject subject);
    List<Note> findByUploadedById(Long userId);

}
