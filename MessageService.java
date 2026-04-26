package com.nustnest.backend.chat;

import com.nustnest.backend.user.User;
import com.nustnest.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageResponse sendMessage(Long senderId, MessageRequest request) {
        System.out.println("SEND MESSAGE CALLED");
        System.out.println("Sender: " + senderId);
        System.out.println("Receiver: " + request.getReceiverId());
        System.out.println("Content: " + request.getContent());


        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new IllegalArgumentException("Sender not found"));

        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new IllegalArgumentException("Receiver not found"));

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(request.getContent())
                .isRead(false)
                .build();

        messageRepository.save(message);

        System.out.println("MESSAGE SAVED with ID: " + message.getId());

        MessageResponse response = mapToResponse(message);

        messagingTemplate.convertAndSendToUser(
                request.getReceiverId().toString(),
                "/queue/messages",
                response
        );

        return response;
    }

    public List<MessageResponse> getConversation(Long userId, Long otherUserId) {
        return messageRepository.findConversation(userId, otherUserId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<MessageResponse> getUnreadMessages(Long userId) {
        return messageRepository.findUnreadMessages(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public void markAsRead(Long senderId, Long receiverId) {
        messageRepository.findConversation(senderId, receiverId)
                .stream()
                .filter(m -> m.getReceiver().getId().equals(receiverId) && !m.isRead())
                .forEach(m -> {
                    m.setRead(true);
                    messageRepository.save(m);
                });
    }

    private MessageResponse mapToResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFullName())
                .receiverId(message.getReceiver().getId())
                .receiverName(message.getReceiver().getFullName())
                .content(message.getContent())
                .isRead(message.isRead())
                .sentAt(message.getSentAt())
                .build();
    }
}