package com.nustnest.backend.auth;

import com.nustnest.backend.user.User;
import com.nustnest.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private static final String University_Domain = "@seecs.edu.pk";

    public String register (RegisterRequest request){

        if(!request.getEmail().endsWith(University_Domain)){
            throw new IllegalArgumentException("Only SEECS email addresses are allowed (@seecs.edu.pk");
        }

        User existingUser = userRepository.findByEmail(request.getEmail()).orElse(null);

        if(existingUser != null){
            if(!existingUser.isEmailVerified()){
                emailVerificationService.sendVerificationEmail(existingUser);
                return "Account already exists but is unverified. A new verification email has been sent.";
            } else {
                throw new IllegalArgumentException("An account with this email already exists");
            }
        }

        User user = User.create(
                request.getFullName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword())
        );

        userRepository.save(user);
        emailVerificationService.sendVerificationEmail(user);

        return "Registration successful. Please check your email to verify your account";
    }

    public User login(LoginRequest request){

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(()-> new IllegalArgumentException("Invalid email or password"));

        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())){
            throw new IllegalArgumentException("Invalid email or password");
        }

        if(!user.isEmailVerified()){
            throw new IllegalArgumentException("Please verify your email before logging in. Check your inbox");
        }

        return user;
    }
}
