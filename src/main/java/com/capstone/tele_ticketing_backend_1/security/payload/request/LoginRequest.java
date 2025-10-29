package com.capstone.tele_ticketing_backend_1.security.payload.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LoginRequest {
	@NotBlank
	private String username;

	@NotBlank
	private String password;
	
	
}
