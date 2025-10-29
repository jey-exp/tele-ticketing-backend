package com.capstone.tele_ticketing_backend_1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TicketCreationReqDTO {

    @NotBlank(message = "Title is required")
    private String title;


    @NotBlank(message = "Status is required")
    @Size(max = 50, message = "Status must not exceed 50 characters")
    private String status;

    @NotBlank(message = "Priority is required")
    @Size(max = 50, message = "Priority must not exceed 50 characters")
    private String priority;

    @NotBlank(message = "Severity is required")
    @Size(max = 50, message = "Severity must not exceed 50 characters")
    private String severity;

    private String category; // optional
    private String subCategory; // optional
    private Integer slaDurationHours; // optional

    @NotNull(message = "Customer ID is required")
    private Long customerId;

    @NotNull(message = "Created By User ID is required")
    private Long createdById;

    // optional fields
    private String issueDate; // can be sent as IS
}
