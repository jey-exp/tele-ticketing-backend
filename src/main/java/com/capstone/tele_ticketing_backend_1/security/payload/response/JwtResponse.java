package com.capstone.tele_ticketing_backend_1.security.payload.response;

import lombok.*;
import lombok.ToString.Exclude;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class JwtResponse {
	private String accessToken;
	private String tokenType = "Bearer";
	private Long id;
	private String username;
	@Exclude
	private String role;

	public JwtResponse(String accessToken, Long id, String username, String role) {
		this.accessToken = accessToken;
		this.id = id;
		this.username = username;
		this.role = role;
	}
}