package com.nustnest.backend.chat;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final MessageService messageService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload MessageRequest request,
                            SimpMessageHeaderAccessor headerAccessor) {

        Long senderId = (Long) headerAccessor.getSessionAttributes().get("userId");
        if (senderId == null) return;

        messageService.sendMessage(senderId, request);
    }

    @PostMapping("/send")
    public ResponseEntity<MessageResponse> send(
            @RequestBody MessageRequest request, HttpSession session) {

        Long senderId = (Long) session.getAttribute("userId");
        if (senderId == null) return ResponseEntity.status(401).build();

        MessageResponse response = messageService.sendMessage(senderId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history/{otherUserId}")
    public ResponseEntity<List<MessageResponse>> getHistory(
            @PathVariable Long otherUserId, HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();

        List<MessageResponse> messages = messageService.getConversation(userId, otherUserId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/unread")
    public ResponseEntity<List<MessageResponse>> getUnread(HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();

        List<MessageResponse> messages = messageService.getUnreadMessages(userId);
        return ResponseEntity.ok(messages);
    }

    @PutMapping("/read/{senderId}")
    public ResponseEntity<String> markAsRead(
            @PathVariable Long senderId, HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return ResponseEntity.status(401).build();

        messageService.markAsRead(senderId, userId);
        return ResponseEntity.ok("Messages marked as read");
    }
}
