package com.nustnest.backend.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @Query("SELECT m FROM Message m WHERE " +
            "(m.sender.id = :senderId AND m.receiver.id = :receiverId) OR " +
            "(m.sender.id = :receiverId AND m.receiver.id = :senderId) " +
            "ORDER BY m.sentAt ASC")
    List<Message> findConversation(@Param("senderId") Long senderId,
                                   @Param("receiverId") Long receiverId);

    @Query("SELECT m FROM Message m WHERE m.receiver.id = :userId " +
            "AND m.isRead = false")
    List<Message> findUnreadMessages(@Param("userId") Long userId);
}
