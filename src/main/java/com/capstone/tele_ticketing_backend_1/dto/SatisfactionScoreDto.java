package com.capstone.tele_ticketing_backend_1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SatisfactionScoreDto {
    private Integer rating;
    private Long count;
}