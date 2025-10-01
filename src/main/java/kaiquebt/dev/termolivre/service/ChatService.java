package kaiquebt.dev.termolivre.service;

import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.chat.events.channel.ChannelMessageEvent;
import kaiquebt.dev.termolivre.model.ChatMessage;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final TermoFilter filter;
    private final SimpMessagingTemplate messagingTemplate;
    private TwitchClient twitchClient;
    
    @Value("${twitch.channel.url:https://www.twitch.tv/}")
    private String twitchChannelUrl;
        
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
            if (!filter.isMessageSafe(event.getMessage())) {
                chatMessage.setContent("Usuário tentou dizer a resposta!");
            }
            // Enviar via WebSocket para os clientes conectados
            // Não armazenamos as mensagens, apenas enviamos em tempo real
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
    
    public String getChatChannel() {
        return extractChannelNameFromUrl(twitchChannelUrl);
    }
}
