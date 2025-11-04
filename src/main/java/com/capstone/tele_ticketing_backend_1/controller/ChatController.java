package com.capstone.tele_ticketing_backend_1.controller;

import com.capstone.tele_ticketing_backend_1.ai.ChatAssistant;
import com.capstone.tele_ticketing_backend_1.dto.ChatRequestDto;
import com.capstone.tele_ticketing_backend_1.dto.ChatResponseDto;
import com.capstone.tele_ticketing_backend_1.security.service.UserDetailsImpl;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai/chat") // Aligned with our v1 API standard
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatAssistant chatAssistant;
    private final ChatMemoryStore chatMemoryStore; // Inject the store for clearing memory

    @PostMapping("/message")
    public ResponseEntity<ChatResponseDto> sendMessage(@RequestBody ChatRequestDto request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
                return ResponseEntity.status(401).body(
                        new ChatResponseDto("Please log in to use the chat assistant.", null)
                );
            }

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            // Create a unique chat ID based on the user's database ID.
            String chatId = "user-" + userDetails.getId();

            log.info("Processing chat message for user: {}, chatId: {}", userDetails.getUsername(), chatId);

            String response = chatAssistant.chat(chatId, request.getMessage());

            log.info("AI response generated for chatId: {}", chatId);

            return ResponseEntity.ok(new ChatResponseDto(response, chatId));

        } catch (Exception e) {
            log.error("Error processing chat message", e);
            return ResponseEntity.status(500).body(
                    new ChatResponseDto("Sorry, I encountered an error. Please try again.", null)
            );
        }
    }

    @DeleteMapping("/clear/{chatId}")
    public ResponseEntity<Void> clearChatHistory(@PathVariable String chatId) {
        try {
            log.info("Chat history clear requested for chatId: {}", chatId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error clearing chat history", e);
            return ResponseEntity.status(500).build();
        }
    }
}