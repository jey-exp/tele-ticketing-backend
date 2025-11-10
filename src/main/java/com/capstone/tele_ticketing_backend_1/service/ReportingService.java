package com.capstone.tele_ticketing_backend_1.service;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.capstone.tele_ticketing_backend_1.dto.AverageResolutionTimeDto;
import com.capstone.tele_ticketing_backend_1.dto.SatisfactionScoreDto;
import com.capstone.tele_ticketing_backend_1.dto.TimeSeriesDataPointDto;
import com.capstone.tele_ticketing_backend_1.projections.SatisfactionScoreProjection;
import com.capstone.tele_ticketing_backend_1.projections.TicketVolumeProjection;
import com.capstone.tele_ticketing_backend_1.repo.FeedbackRepo;
import com.capstone.tele_ticketing_backend_1.repo.TicketRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportingService implements ReportingServiceInterface {

    private final TicketRepo ticketRepo;
    private final FeedbackRepo feedbackRepo;

    // Defines the time range for our reports (e.g., last 30 days)
    private static final LocalDateTime REPORTING_START_DATE = LocalDateTime.now().minusDays(30);

    public List<TimeSeriesDataPointDto> getTicketVolumeReport() {
        List<TicketVolumeProjection> projections = ticketRepo.getTicketVolumeByDay(REPORTING_START_DATE);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd");
        return projections.stream()
                .map(p -> new TimeSeriesDataPointDto(
                        p.getDate().format(formatter),
                        p.getCount()
                ))
                .collect(Collectors.toList());
    }

    public AverageResolutionTimeDto getAverageResolutionTimeReport() {
        Double avgHours = ticketRepo.getAverageResolutionTimeInHours(REPORTING_START_DATE);
        return new AverageResolutionTimeDto(avgHours != null ? avgHours : 0.0);
    }

    public List<SatisfactionScoreDto> getSatisfactionScoreReport() {
        List<SatisfactionScoreProjection> projections = feedbackRepo.getSatisfactionScoreDistribution();
        return projections.stream()
                .map(p -> new SatisfactionScoreDto(
                        p.getRating(),
                        p.getCount()
                ))
                .collect(Collectors.toList());
    }
}
