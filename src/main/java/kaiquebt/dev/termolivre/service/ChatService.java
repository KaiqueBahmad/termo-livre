package kaiquebt.dev.termolivre.service;

import kaiquebt.dev.termolivre.model.ChatMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final List<ChatMessage> messages = new ArrayList<>();
    
    @Value("${chat.channel:general}")
    private String chatChannel;
    
    public ChatService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    public void sendMessage(ChatMessage message) {
        message.setTimestamp(LocalDateTime.now());
        messages.add(message);
        // Envia a mensagem para todos os subscribers do tópico
        messagingTemplate.convertAndSend("/topic/messages", message);
    }
    
    public List<ChatMessage> getMessages() {
        return new ArrayList<>(messages);
    }
    
    public String getChatChannel() {
        return chatChannel;
    }
}
