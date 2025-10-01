package kaiquebt.dev.termolivre.service;

import com.github.philippheuer.events4j.core.EventManager;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import kaiquebt.dev.termolivre.model.ChatMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ChatService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final List<ChatMessage> messages = new CopyOnWriteArrayList<>();
    private TwitchClient twitchClient;
    
    @Value("${twitch.channel.url:https://www.twitch.tv/}")
    private String twitchChannelUrl;
    
    public ChatService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    @PostConstruct
    public void init() {
        // Extrair o nome do canal da URL
        String channelName = extractChannelNameFromUrl(twitchChannelUrl);
        if (channelName == null) {
            channelName = "monstercat"; // Canal padrão
        }
        
        // Construir o cliente do Twitch
        twitchClient = TwitchClientBuilder.builder()
                .withEnableChat(true)
                .build();
        
        // Conectar ao chat do canal
        twitchClient.getChat().joinChannel(channelName);
        
        // Registrar listener para mensagens do chat
        twitchClient.getEventManager().onEvent(ChannelMessageEvent.class, event -> {
            ChatMessage chatMessage = new ChatMessage(
                event.getMessage(),
                event.getUser().getName(),
                LocalDateTime.now()
            );
            
            // Adicionar à lista de mensagens
            messages.add(chatMessage);
            
            // Enviar via WebSocket para os clientes conectados
            messagingTemplate.convertAndSend("/topic/messages", chatMessage);
        });
    }
    
    @PreDestroy
    public void cleanup() {
        if (twitchClient != null) {
            twitchClient.close();
        }
    }
    
    private String extractChannelNameFromUrl(String url) {
        // Extrair o nome do canal da URL
        // Exemplo: https://www.twitch.tv/nome_do_canal -> nome_do_canal
        if (url != null && !url.trim().isEmpty()) {
            String[] parts = url.split("/");
            if (parts.length > 0) {
                return parts[parts.length - 1];
            }
        }
        return null;
    }
    
    public List<ChatMessage> getMessages() {
        return new ArrayList<>(messages);
    }
    
    public String getChatChannel() {
        return extractChannelNameFromUrl(twitchChannelUrl);
    }
}
