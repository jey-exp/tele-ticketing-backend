package com.capstone.tele_ticketing_backend_1.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatResponseDto {
    private String response;
    private String chatId;
}