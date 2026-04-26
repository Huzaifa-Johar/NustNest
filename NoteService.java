package com.nustnest.backend.notes;

import com.nustnest.backend.user.User;
import com.nustnest.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor

public class NoteService {

    private final NoteRepository noteRepository;
    private final NoteRatingRepository noteRatingRepository;
    private final UserRepository userRepository;
    private static final String Upload_Dir = "uploads/notes/";

    private static final List<String> Allowed_Types = List.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final long Max_Size = 20 * 1024 * 1024;

    public NoteResponse uploadNote(Long userId, NoteUploadRequest request,
                                   MultipartFile file) throws IOException{

        if(file.isEmpty()){
            throw new IllegalArgumentException("Please select a file to upload");
        }

        String contentType = file.getContentType();
        if(contentType == null || !Allowed_Types.contains(contentType)){
            throw new IllegalArgumentException("Only PDF, PPTX and DOCX files are allowed");
        }

        if(file.getSize() > Max_Size){
            throw new IllegalArgumentException("File size must not exceed 20MB");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new IllegalArgumentException("User not found"));

        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName != null && originalFileName.contains(".")
                ? originalFileName.substring(originalFileName.lastIndexOf(".")): "";

        String newFileName = UUID.randomUUID() + extension;
        Path uploadPath = Paths.get(Upload_Dir);
        Files.createDirectories(uploadPath);
        Path filePath = uploadPath.resolve(newFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        String fileType = extension.replace(".","").toUpperCase();

        Note note = Note.create(
                request.getTitle(),
                request.getDescription(),
                Upload_Dir + newFileName,
                fileType,
                request.getSubject(),
                user,
                file.getSize()
        );

        noteRepository.save(note);

        return mapToResponse(note, null);
    }

    public List<NoteResponse> getAllNotes(Long userId, SortOption sortOption){

        if(sortOption == null)
            sortOption = SortOption.HIGHEST_RATING;

        List<Note> notes = switch (sortOption){

            case HIGHEST_RATING -> noteRepository.findAllByOrderByAverageRatingDesc();
            case LOWEST_RATING -> noteRepository.findAllByOrderByAverageRatingAsc();
            case NEWEST -> noteRepository.findAllByOrderByCreatedAtDesc();
            case OLDEST -> noteRepository.findAllByOrderByCreatedAtAsc();
        };
        return notes.stream().map(note-> mapToResponse(note,getUserRating(note.getId(), userId)))
                .collect(Collectors.toList());
    }

    public List<NoteResponse> getNotesBySubject(Subject subject, Long userId, SortOption sortOption){

        if(sortOption == null)
            sortOption = SortOption.HIGHEST_RATING;

        List<Note> notes = switch (sortOption){
            case HIGHEST_RATING -> noteRepository.findBySubjectOrderByAverageRatingDesc(subject);
            case LOWEST_RATING -> noteRepository.findBySubjectOrderByAverageRatingAsc(subject);
            case NEWEST -> noteRepository.findBySubjectOrderByCreatedAtDesc(subject);
            case OLDEST -> noteRepository.findBySubjectOrderByCreatedAtAsc(subject);
        };

        return notes.stream().map(note -> mapToResponse(note, getUserRating(note.getId(),userId)))
                .collect(Collectors.toList());
    }

    public Resource downloadNote(Long noteId) throws MalformedURLException{

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Notes not found"));

        Path filePath = Paths.get(note.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());

        if(!resource.exists()){
            throw new IllegalArgumentException("File not found on server");
        }

        return resource;
    }

    public NoteResponse rateNote(Long noteId, Long userId, int rating){

        if(rating < 1 || rating > 5){
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new IllegalArgumentException("Notes not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if(note.getUploadedBy().getId() == userId){
            throw new IllegalArgumentException("You cannot rate your own notes");
        }

        NoteRating noteRating = noteRatingRepository
                .findByNoteIdAndRatedById(noteId, userId)
                .orElseGet(() -> {
                    NoteRating newRating = new NoteRating();
                    newRating.setNote(note);
                    newRating.setRatedBy(user);
                    return newRating;
                });

        noteRating.setRating(rating);
        noteRatingRepository.save(noteRating);

        List<NoteRating> allRatings = noteRatingRepository.findByNoteId(noteId);
        double average = allRatings.stream().mapToInt(NoteRating::getRating).average().orElse(0.0);

        note.setAverageRating(Math.round(average * 10.0)/10.0);
        note.setTotalRatings(allRatings.size());
        noteRepository.save(note);

        return mapToResponse(note,rating);
    }

    public String deleteNote(Long noteId, Long userId) throws IOException{

        Note note = noteRepository.findById(noteId)
                .orElseThrow(()-> new IllegalArgumentException("Notes not found"));

        if(note.getUploadedBy().getId() != userId){
            throw new IllegalArgumentException("You can only delete your own notes");
        }

        Path filePath = Paths.get(note.getFilePath());
        Files.deleteIfExists(filePath);
        noteRepository.delete(note);

        return "Notes deleted successfully";
    }

    private Integer getUserRating(Long noteId, Long userId){

        if(userId == null)
            return null;
        return noteRatingRepository.findByNoteIdAndRatedById(noteId, userId)
                .map(NoteRating::getRating).orElse(null);
    }

    private NoteResponse mapToResponse(Note note, Integer userRating){

        NoteResponse response = new NoteResponse();
        response.setId(note.getId());
        response.setTitle(note.getTitle());
        response.setDescription(note.getDescription());
        response.setFileType(note.getFileType());
        response.setFileSize(note.getFileSize());
        response.setSubject(note.getSubject());
        response.setUploadedByName(note.getUploadedBy().getFullName());
        response.setAverageRating(note.getAverageRating());
        response.setTotalRatings(note.getTotalRatings());
        response.setUserRating(userRating);
        response.setCreatedAt(note.getCreatedAt());

        return response;
    }


}
