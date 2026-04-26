package com.nustnest.backend.notes;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final NoteRepository noteRepository;

    @GetMapping("/file-counts")
    public ResponseEntity<Map<String, Integer>> getFileCounts(HttpSession session) {

        Map<String, Integer> fileCounts = new LinkedHashMap<>();
        fileCounts.put("CS_212_OOP",
                noteRepository.findBySubjectOrderByAverageRatingDesc(Subject.CS_212_OOP).size());
        fileCounts.put("EE_122_COMPUTER_ARCHITECTURE_AND_LOGIC_DESIGN",
                noteRepository.findBySubjectOrderByAverageRatingDesc(Subject.EE_122_COMPUTER_ARCHITECTURE_AND_LOGIC_DESIGN).size());
        fileCounts.put("MATH_161_DISCRETE_MATHEMATICS",
                noteRepository.findBySubjectOrderByAverageRatingDesc(Subject.MATH_161_DISCRETE_MATHEMATICS).size());
        fileCounts.put("MATH_121_LINEAR_ALGEBRA_AND_ODE",
                noteRepository.findBySubjectOrderByAverageRatingDesc(Subject.MATH_121_LINEAR_ALGEBRA_AND_ORDINARY_DIFFERENTIAL_EQUATIONS).size());
        fileCounts.put("HU_114_FUNCTIONAL_ENGLISH",
                noteRepository.findBySubjectOrderByAverageRatingDesc(Subject.HU_114_FUNCTIONAL_ENGLISH).size());
        fileCounts.put("HU_132_UNDERSTANDING_OF_QURAN",
                noteRepository.findBySubjectOrderByAverageRatingDesc(Subject.HU_132_UNDERSTANDING_OF_QURAN).size());

        return ResponseEntity.ok(fileCounts);
    }
}
