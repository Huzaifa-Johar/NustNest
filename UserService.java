package com.nustnest.backend.user;

import com.nustnest.backend.user.UserProfileResponse;
import com.nustnest.backend.user.UpdateBioRequest;
import com.nustnest.backend.user.UpdatePasswordRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor

public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String upload_Dir = "uploads/profiles/";
    private static final List<String>allowed_Types = List.of("image/jpeg",
            "image/png",
            "image/webp");

    private static final long maxSize = 2 * 1024 * 1024;

    public UserProfileResponse getProfile(Long userId){

        User user =     userRepository.findById(userId)
                .orElseThrow(()-> new IllegalArgumentException("User not found"));

        return UserProfileResponse.builder().id(user.getId()).fullName(user.getFullName())
                .email(user.getEmail()).bio(user.getBio()).profileImagePath(user.getProfileImagePath())
                .emailVerified(user.isEmailVerified()).createdAt(user.getCreatedAt()).build();

    }

    public String updateBio(Long userId, UpdateBioRequest request) {

        User user = userRepository.findById((userId))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setBio(request.getBio());
        userRepository.save(user);

        return "Bio updated successfully";
    }
    public String updatePassword(Long userId, UpdatePasswordRequest request){

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new IllegalArgumentException("User not found"));

        if(!passwordEncoder.matches(request.getCurrentPassword(),user.getPassword())){
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if(!request.getNewPassword().equals(request.getConfirmPassword())){
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        if(passwordEncoder.matches(request.getNewPassword(), user.getPassword())){
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return "Password updated successfully";
    }

    public String uploadProfileImage(Long userId, MultipartFile file) throws IOException{

        if(file.isEmpty()){
            throw new IllegalArgumentException("Please select an image to upload");
        }

        String contentType = file.getContentType();
        if(contentType == null || !allowed_Types.contains(contentType)){
            throw new IllegalArgumentException("Only JPG, PNG, and WEBP images are allowed");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new IllegalArgumentException("User not found"));

        if(user.getProfileImagePath() != null ){
            Path oldImage = Paths.get(user.getProfileImagePath());
            Files.deleteIfExists(oldImage);
        }

        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName != null && originalFileName.contains(".")
                ? originalFileName.substring(originalFileName.lastIndexOf("."))
                : ".jpg";

        String newFileName = UUID.randomUUID() + extension;

        Path uploadPath = Paths.get(upload_Dir);
        Files.createDirectories(uploadPath);

        Path filePath = uploadPath.resolve(newFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String savedPath = upload_Dir + newFileName;
        user.setProfileImagePath(savedPath);
        userRepository.save(user);

        return savedPath;
    }

    public byte[] getProfileImage(Long userId) throws IOException{

        User user = userRepository.findById(userId)
                .orElseThrow(()-> new IllegalArgumentException("User not found"));

        if(user.getProfileImagePath() == null){
            throw new IllegalArgumentException("User has no profile image");
        }

        Path imagePath = Paths.get(user.getProfileImagePath());
        if(!Files.exists(imagePath)){
            throw new IllegalArgumentException("Profile image file not found");
        }

        return Files.readAllBytes(imagePath);
    }

    public String deleteProfileImage(Long userId) throws IOException{
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new IllegalArgumentException("User not found"));

        if(user.getProfileImagePath() == null){
            throw new IllegalArgumentException("You don't have a profile image to remove");
        }

        Path imagePath = Paths.get(user.getProfileImagePath());
        Files.deleteIfExists(imagePath);

        user.setProfileImagePath(null);
        userRepository.save(user);

        return "Profile image removed successfully";
    }

    public List<UserProfileResponse> searchUsers(String name, Long currentUserId) {
        return userRepository.findByFullNameContainingIgnoreCase(name)
                .stream()
                .filter(user -> !user.getId().equals(currentUserId)) // exclude self
                .map(user -> UserProfileResponse.builder()
                        .id(user.getId())
                        .fullName(user.getFullName())
                        .email(user.getEmail())
                        .bio(user.getBio())
                        .profileImagePath(user.getProfileImagePath())
                        .emailVerified(user.isEmailVerified())
                        .createdAt(user.getCreatedAt())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

}
