package kaiquebt.dev.termolivre.controller;

import kaiquebt.dev.termolivre.service.ChatService;
import kaiquebt.dev.termolivre.service.TermoFilter;
import kaiquebt.dev.termolivre.model.ChatMessage;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class ChatController {

    private final ChatService chatService;
    
    @Autowired
    private TermoFilter termoFilter;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/")
    public String chatPage(Model model) {
        model.addAttribute("channel", chatService.getChatChannel());
        return "chat";
    }

    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public ChatMessage handleChatMessage(ChatMessage message) {
        // Verificar se o conteúdo da mensagem é seguro
        if (!termoFilter.isMessageSafe(message.getContent())) {
            // Substituir por mensagem de deletada
            return new ChatMessage("this message was deleted", message.getSender(), message.getTimestamp());
        }
        return message;
    }
}
