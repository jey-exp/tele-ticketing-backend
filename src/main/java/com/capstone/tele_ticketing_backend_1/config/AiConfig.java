package com.capstone.tele_ticketing_backend_1.config;

import com.capstone.tele_ticketing_backend_1.ai.ChatAssistant;
import com.capstone.tele_ticketing_backend_1.ai.TicketingTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    @Value("${google.api.key}")
    private String googleApiKey;

    @Bean
    public ChatMemoryStore chatMemoryStore() {
        return new InMemoryChatMemoryStore();
    }

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        if (googleApiKey == null || googleApiKey.trim().isEmpty()) {
            throw new RuntimeException(
                    "Error: google.api.key is not set in application.properties. " +
                            "Please add it to your configuration."
            );
        }

        return GoogleAiGeminiChatModel.builder()
                .apiKey(googleApiKey)
                // Dr. X's Fix: Use the model name you verified works.
                .modelName("gemini-flash-latest")
                .temperature(0.5)
                .maxOutputTokens(2048)
                .build();
    }

    @Bean
    public ChatAssistant chatAssistant(ChatLanguageModel chatLanguageModel,
                                       ChatMemoryStore chatMemoryStore,
                                       TicketingTools ticketingTools) {
        return AiServices.builder(ChatAssistant.class)
                .chatLanguageModel(chatLanguageModel)
                .chatMemoryProvider(chatId -> MessageWindowChatMemory.builder()
                        .chatMemoryStore(chatMemoryStore)
                        .maxMessages(20)
                        .id(chatId)
                        .build())
                .tools(ticketingTools)
                .build();
    }
}