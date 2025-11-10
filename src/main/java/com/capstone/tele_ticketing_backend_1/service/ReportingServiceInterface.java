package com.capstone.tele_ticketing_backend_1.service;

import com.capstone.tele_ticketing_backend_1.dto.AverageResolutionTimeDto;
import com.capstone.tele_ticketing_backend_1.dto.SatisfactionScoreDto;
import com.capstone.tele_ticketing_backend_1.dto.TimeSeriesDataPointDto;
import java.util.List;

public interface ReportingServiceInterface {

    /**
     * Gets the ticket volume per day for the last 30 days.
     * @return A list of time series data points (label and value).
     */
    List<TimeSeriesDataPointDto> getTicketVolumeReport();

    /**
     * Gets the average ticket resolution time in hours for the last 30 days.
     * @return A DTO containing the average resolution time.
     */
    AverageResolutionTimeDto getAverageResolutionTimeReport();

    /**
     * Gets the distribution of all customer satisfaction ratings.
     * @return A list of DTOs, each containing a rating (1-5) and its total count.
     */
    List<SatisfactionScoreDto> getSatisfactionScoreReport();
}