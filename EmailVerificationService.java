package com.nustnest.backend.auth;

import com.nustnest.backend.user.User;
import com.nustnest.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor

public class EmailVerificationService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;

    @Value("${app.base-url}")
    private String baseUrl;

    private static final int token_Expiry_Hours = 24;

    public void sendVerificationEmail(User user) {

        String token = UUID.randomUUID().toString();
        user.setVerificationToken(token);
        user.setTokenExpiresat(LocalDateTime.now().plusHours(token_Expiry_Hours));
        userRepository.save(user);

        String verificationLink = baseUrl + "/api/auth/verify?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("${spring.mail.username}");
        message.setTo(user.getEmail());
        message.setSubject("Verify your NustNest email");
        message.setText("Hello " + user.getFullName() + ",\n\n" + "Thankyou for registering at " +
                "NustNest. Please click the link below to verify your email:\n\n" +
                verificationLink + "\n\n" + "This link will expire in 24 hours. If you " +
                "did not register please ignore this email.\n\n  NustNest Team");
        mailSender.send(message);
    }


    public String verifyEmail(String token){

            User user = userRepository.findByVerificationToken(token)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid verification link"));

            if(user.getTokenExpiresat().isBefore(LocalDateTime.now())){
                throw new IllegalArgumentException("Verification link has expired. Please register again.");
            }

            if(user.isEmailVerified())
                return "Email is already verified. You can log in.";

            user.setEmailVerified(true);
            user.setVerificationToken(null);
            user.setTokenExpiresat(null);
            userRepository.save(user);

        return "Email verified successfully. You can now log in.";
        }

    public String resendVerificationEmail(String email){

        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new IllegalArgumentException("No account found with this " +
                        "email"));

        if(user.isEmailVerified())
            return "Your email is already verified. You can log in.";

        sendVerificationEmail(user);
        return "Verification email resent. Please check your inbox.";
    }

}
