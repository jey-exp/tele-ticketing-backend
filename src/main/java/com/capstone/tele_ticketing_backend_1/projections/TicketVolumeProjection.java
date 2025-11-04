package com.capstone.tele_ticketing_backend_1.projections;


import java.time.LocalDate;

public interface TicketVolumeProjection {
    LocalDate getDate();
    Long getCount();
}