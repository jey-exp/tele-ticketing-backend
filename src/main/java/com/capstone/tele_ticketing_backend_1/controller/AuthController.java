package com.capstone.tele_ticketing_backend_1.controller;


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.capstone.tele_ticketing_backend_1.entities.AppUser;
import com.capstone.tele_ticketing_backend_1.entities.ERole;
import com.capstone.tele_ticketing_backend_1.entities.Role;
import com.capstone.tele_ticketing_backend_1.repo.RoleRepo;
import com.capstone.tele_ticketing_backend_1.repo.UserRepo;
import com.capstone.tele_ticketing_backend_1.security.jwt.JwtUtils;
import com.capstone.tele_ticketing_backend_1.security.payload.request.LoginRequest;
import com.capstone.tele_ticketing_backend_1.security.payload.request.SignupRequest;
import com.capstone.tele_ticketing_backend_1.security.payload.response.JwtResponse;
import com.capstone.tele_ticketing_backend_1.security.payload.response.MessageResponse;
import com.capstone.tele_ticketing_backend_1.security.service.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;


@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	UserRepo userRepository;

	@Autowired
	RoleRepo roleRepository;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	JwtUtils jwtUtils;

	@PostMapping("/signin")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),
						loginRequest.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);
		String jwt = jwtUtils.generateJwtToken(authentication);

		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
		List<String> roles = userDetails.getAuthorities().stream()
				.map(item -> item.getAuthority())
				.collect(Collectors.toList());
		String firstAuthority = userDetails.getAuthorities().stream()
				.findFirst()
				.map(GrantedAuthority::getAuthority)
				.map(this::mapRoleToFrontend)
				.orElse(null);


		return ResponseEntity.ok(new JwtResponse(jwt,
				userDetails.getId(),
				userDetails.getUsername(),
				firstAuthority
				));
	}

	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
		System.out.println("Signup request : " + signUpRequest);
		if (userRepository.existsByUsername(signUpRequest.getUsername())) {
			return ResponseEntity
					.badRequest()
					.body(new MessageResponse("Error: Username is already taken!"));
		}

		// Create new user's account
		AppUser user = new AppUser(signUpRequest.getUsername(),
				encoder.encode(signUpRequest.getPassword()), signUpRequest.getFullname());

		Set<String> strRoles = signUpRequest.getRole();
		Set<Role> roles = new HashSet<>();

		strRoles.forEach(role -> {
			switch (role) {
				case "customer": {
					Role foundRole = roleRepository.findByName(ERole.ROLE_CUSTOMER)
							.orElseThrow(() -> new RuntimeException("Error: Role 'CUSTOMER' is not found."));
					roles.add(foundRole);
					break;
				}
				case "agent": {
					Role foundRole = roleRepository.findByName(ERole.ROLE_AGENT)
							.orElseThrow(() -> new RuntimeException("Error: Role 'AGENT' is not found."));
					roles.add(foundRole);
					break;
				}
				case "triage_officer": {
					Role foundRole = roleRepository.findByName(ERole.ROLE_TRIAGE_OFFICER)
							.orElseThrow(() -> new RuntimeException("Error: Role 'TRIAGE_OFFICER' is not found."));
					roles.add(foundRole);
					break;
				}
				case "field_engineer": {
					Role foundRole = roleRepository.findByName(ERole.ROLE_FIELD_ENGINEER)
							.orElseThrow(() -> new RuntimeException("Error: Role 'FIELD_ENGINEER' is not found."));
					roles.add(foundRole);
					break;
				}
				case "noc_engineer": {
					Role foundRole = roleRepository.findByName(ERole.ROLE_NOC_ENGINEER)
							.orElseThrow(() -> new RuntimeException("Error: Role 'NOC_ENGINEER' is not found."));
					roles.add(foundRole);
					break;
				}
				case "l1_engineer": {
					Role foundRole = roleRepository.findByName(ERole.ROLE_L1_ENGINEER)
							.orElseThrow(() -> new RuntimeException("Error: Role 'L1_ENGINEER' is not found."));
					roles.add(foundRole);
					break;
				}
				case "manager": {
					Role foundRole = roleRepository.findByName(ERole.ROLE_MANAGER)
							.orElseThrow(() -> new RuntimeException("Error: Role 'MANAGER' is not found."));
					roles.add(foundRole);
					break;
				}
				case "team_lead": {
					Role foundRole = roleRepository.findByName(ERole.ROLE_TEAM_LEAD)
							.orElseThrow(() -> new RuntimeException("Error: Role 'TEAM_LEAD' is not found."));
					roles.add(foundRole);
					break;
				}
				case "cxo": {
					Role foundRole = roleRepository.findByName(ERole.ROLE_CXO)
							.orElseThrow(() -> new RuntimeException("Error: Role 'CXO' is not found."));
					roles.add(foundRole);
					break;
				}
				case "noc_admin": {
					Role foundRole = roleRepository.findByName(ERole.ROLE_NOC_ADMIN)
							.orElseThrow(() -> new RuntimeException("Error: Role 'NOC_ADMIN' is not found."));
					roles.add(foundRole);
					break;
				}
				default:
					// Handles any role string that doesn't match a case
					throw new RuntimeException("Error: Invalid role specified: " + role);
			}
		});

		user.setRoles(roles);
		//saving UserEntity to the database
		userRepository.save(user);

		return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
	}

	private String mapRoleToFrontend(String backendRole) {
		switch (backendRole) {
			case "ROLE_CUSTOMER": return "Customer";
			case "ROLE_AGENT": return "Agent";
			case "ROLE_TRIAGE_OFFICER": return "Triage Officer";
			case "ROLE_FIELD_ENGINEER": return "Field Engineer";
			case "ROLE_NOC_ENGINEER": return "NOC Engineer";
			case "ROLE_L1_ENGINEER": return "L1 Engineer";
			case "ROLE_TEAM_LEAD": return "Team Lead";
			case "ROLE_MANAGER": return "Manager";
			case "ROLE_CXO": return "CXO";
			case "ROLE_NOC_ADMIN": return "NOC Admin";
			default: return backendRole; // fallback if unknown
		}
	}

}