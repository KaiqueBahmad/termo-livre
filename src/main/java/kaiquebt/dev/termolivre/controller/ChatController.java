package kaiquebt.dev.termolivre.controller;

import kaiquebt.dev.termolivre.service.ChatService;
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
}
