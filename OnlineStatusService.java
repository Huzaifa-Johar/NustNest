package com.nustnest.backend.user;

import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OnlineStatusService {

    private final Set<Long> onlineUsers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public void userConnected(Long userId) {
        onlineUsers.add(userId);
    }

    public void userDisconnected(Long userId) {
        onlineUsers.remove(userId);
    }

    public boolean isOnline(Long userId) {
        return onlineUsers.contains(userId);
    }

    public Set<Long> getOnlineUsers() {
        return Collections.unmodifiableSet(onlineUsers);
    }
}
