package kaiquebt.dev.termolivre.controller;

import kaiquebt.dev.termolivre.model.ChatMessage;
import kaiquebt.dev.termolivre.service.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/")
    public String chatPage(Model model) {
        model.addAttribute("channel", chatService.getChatChannel());
        model.addAttribute("messages", chatService.getMessages());
        return "chat";
    }

    @MessageMapping("/sendMessage")
    @SendTo("/topic/messages")
    public ChatMessage sendMessage(ChatMessage message) {
        chatService.sendMessage(message);
        return message;
    }
}
