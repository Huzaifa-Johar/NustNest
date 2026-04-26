package com.nustnest.backend.notes;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor

public class NoteController {

    private final NoteService noteService;

    @PostMapping("/upload")
    public ResponseEntity<NoteResponse> uploadNote(
            @Valid @ModelAttribute NoteUploadRequest request,
            @RequestParam("file") MultipartFile file,
            HttpSession session) throws IOException{

        Long userId = (Long) session.getAttribute("userId");
        if(userId == null){
            return ResponseEntity.status(401).build();
        }

        NoteResponse response = noteService.uploadNote(userId, request, file);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/subject/{subject}")
    public ResponseEntity<List<NoteResponse>> getNotesBySubject(
            @PathVariable Subject subject,
            @RequestParam(required = false) SortOption sortBy, HttpSession session){

        Long userId = (Long) session.getAttribute("userId");
        if(userId == null){
            return ResponseEntity.status(401).build();
        }

        List<NoteResponse> notes = noteService.getNotesBySubject(subject, userId, sortBy);

        return ResponseEntity.ok(notes);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadNote(

            @PathVariable Long id, HttpSession session) throws IOException{

        Long userId = (Long) session.getAttribute("userId");
        if(userId == null){
            return ResponseEntity.status(401).build();
        }

        Resource resource = noteService.downloadNote(id);

        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
    }

    @PostMapping("/rate/{id}")
    public  ResponseEntity<NoteResponse> rateNote(
            @PathVariable Long id, @RequestParam int rating, HttpSession session){

        Long userId = (Long) session.getAttribute("userId");
        if(userId == null){
            return ResponseEntity.status(401).build();
        }

        NoteResponse response = noteService.rateNote(id, userId, rating);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteNote(
            @PathVariable Long id, HttpSession session) throws IOException{

        Long userId = (Long) session.getAttribute("userId");
        if(userId == null){
            return ResponseEntity.status(401).build();
        }

        String message = noteService.deleteNote(id, userId);

        return ResponseEntity.ok(message);
    }

}
