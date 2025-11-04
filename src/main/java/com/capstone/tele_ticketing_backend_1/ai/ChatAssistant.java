package com.capstone.tele_ticketing_backend_1.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ChatAssistant {

    @SystemMessage({
            "You are a friendly and helpful Network Ticketing Support Assistant.",
            "Your goal is to assist customers with inquiries about their support tickets.",
            "Before answering questions about specific data (like 'what is my ticket status?'), use the available tools to fetch the necessary information.",
            "Always base your answer on the information provided by the tools.",
            "If the user asks a question you cannot answer with your tools (e.g., 'How is the weather?'),",
            "politely state that you can only help with network ticketing matters.",
            "Keep your answers concise and easy to understand."
    })
    String chat(@MemoryId String chatId, @UserMessage String userMessage);
}