package com.nustnest.backend.user;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor

public class UserController {

    private final UserService userService;
    private final OnlineStatusService onlineStatusService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(HttpSession session){

        Long userId = (Long) session.getAttribute("userId");
        if(userId == null){
            return ResponseEntity.status(401).build();
        }

        UserProfileResponse profile = userService.getProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/setup")
    public ResponseEntity<String> setupProfile(
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) MultipartFile photo,
            HttpSession session) throws IOException {


        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body("You must be logged in");
        }

        if (bio != null) {
            UpdateBioRequest bioRequest = new UpdateBioRequest();
            bioRequest.setBio(bio);
            userService.updateBio(userId, bioRequest);
        }

        if (photo != null && !photo.isEmpty()) {
            userService.uploadProfileImage(userId, photo);
        }

        return ResponseEntity.ok("Profile setup successful");
    }

    @PutMapping("/bio")
    public ResponseEntity<String> updateBio(
            @Valid @RequestBody UpdateBioRequest request, HttpSession session){

        System.out.println("Session ID: " + session.getId());
        System.out.println("userId: " + session.getAttribute("userId"));

        Long userId = (Long) session.getAttribute("userId");
        if(userId == null){
            return ResponseEntity.status(401).body("You must be logged in to update your bio");
        }

        String message = userService.updateBio(userId, request);
        return ResponseEntity.ok(message);
    }

    @PutMapping("/password")
    public ResponseEntity<String> updatePassword(
            @Valid @RequestBody UpdatePasswordRequest request, HttpSession session){

        Long userId = (Long) session.getAttribute("userId");
        if(userId == null){
            return ResponseEntity.status(401).body("You must be logged in to update your password");
        }

        String message = userService.updatePassword(userId, request);
        return ResponseEntity.ok(message);
    }

    @PostMapping("/image")
    public ResponseEntity<String> uploadImage(
            @RequestParam("file") MultipartFile file, HttpSession session) throws Exception{

        Long userId = (Long) session.getAttribute("userId");
        if(userId == null){
            return ResponseEntity.status(401).body("You must be logged in to upload an image");
        }

        String savedPath = userService.uploadProfileImage(userId, file);
        return ResponseEntity.ok("Profile image uploaded successfully: " + savedPath);
    }

    @GetMapping("/image/{userId}")
    public ResponseEntity<byte[]> getImage(@PathVariable Long userId) throws IOException{

        byte[] imageBytes = userService.getProfileImage(userId);

        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(imageBytes);
    }

    @DeleteMapping("/image")
    public ResponseEntity<String> deleteImage(HttpSession session) throws IOException{

        Long userId = (Long) session.getAttribute("userId");
        if(userId == null){
            return ResponseEntity.status(401).body("You must be logged in to remove your profile image");
        }
        String message = userService.deleteProfileImage(userId);
        return ResponseEntity.ok(message);
    }

    @GetMapping("/search")
    public ResponseEntity<List<UserProfileResponse>> searchUsers(
            @RequestParam String name, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if(userId == null)
            return ResponseEntity.status(401).build();
        List<UserProfileResponse> users = userService.searchUsers(name, userId);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/online/{userId}")
    public ResponseEntity<Boolean> isOnline(@PathVariable Long userId, HttpSession session) {

        Long currentUserId = (Long) session.getAttribute("userId");
        if (currentUserId == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(onlineStatusService.isOnline(userId));
    }

    @GetMapping("/online")
    public ResponseEntity<Set<Long>> getOnlineUsers(HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();

        return ResponseEntity.ok(onlineStatusService.getOnlineUsers());
    }
}
