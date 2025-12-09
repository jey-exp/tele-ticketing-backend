package com.capstone.tele_ticketing_backend_1.security.config;


import com.capstone.tele_ticketing_backend_1.security.jwt.AuthEntryPointJwt;
import com.capstone.tele_ticketing_backend_1.security.jwt.AuthTokenFilter;
import com.capstone.tele_ticketing_backend_1.security.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity 
@RequiredArgsConstructor
public class WebSecurityConfig { 
	private final UserDetailsServiceImpl userDetailsService;
	private final AuthEntryPointJwt unauthorizedHandler;
	private final AuthTokenFilter authenticationJwtTokenFilter;



	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();

		authProvider.setUserDetailsService(userDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder());

		return authProvider;
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	protected final static String[] PUBLIC_REQUEST_MATCHERS = { "/api/test/all","/api/v1/auth/**", "/api-docs/**", "/swagger-ui/**","/v3/api-docs/**" };

	public final static String managerRole = "MANAGER";

	// In your SecurityConfig class
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.csrf(AbstractHttpConfigurer::disable)
				// Dr. X's Note: We are defining the authorization rules here.
				.authorizeHttpRequests(auth -> auth.
						requestMatchers(PUBLIC_REQUEST_MATCHERS).permitAll()
						.requestMatchers("/api/v1/customer/**").hasRole("CUSTOMER")
						.requestMatchers("/api/v1/agent/**").hasRole("AGENT")
						.requestMatchers("/api/v1/triage/**").hasRole("TRIAGE_OFFICER")
						.requestMatchers("/api/v1/engineer/**").hasAnyRole("L1_ENGINEER", "NOC_ENGINEER", "FIELD_ENGINEER")
						.requestMatchers("/api/v1/users/engineers").hasRole("TRIAGE_OFFICER")
						.requestMatchers("/api/v1/tickets/**").authenticated()
						.requestMatchers("/api/v1/users/customers").hasRole("AGENT")

						.requestMatchers("/api/v1/team-lead/**").hasRole("TEAM_LEAD")
						.requestMatchers("/api/v1/manager/**").hasRole(managerRole)
						.requestMatchers("/api/v1/teams/**").hasRole(managerRole)
						.requestMatchers("/api/v1/users/cities").hasRole(managerRole)
						.requestMatchers("/api/v1/reports/**").hasRole("CXO")
						.requestMatchers("/api/v1/ai/**").hasRole("CUSTOMER")
						.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

						.anyRequest().authenticated()
				)
						.exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
						.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
						.authenticationProvider(authenticationProvider())
						.addFilterBefore(authenticationJwtTokenFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		// Rule 1: Allow requests from your React app's origin.
		configuration.setAllowedOriginPatterns(List.of(
				"http://localhost:8080",
				"https://*.vercel.app",
				"https://tele-ticketing-frontend-h4ijhl4iz.vercel.app"
		));


		// Rule 2: Allow standard HTTP methods.
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

		// Rule 3: THIS IS THE MOST IMPORTANT PART.
		// You must explicitly allow the 'Authorization' and 'Content-Type' headers.
		configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));

		// Rule 4: Allow credentials (cookies, etc.) if needed in the future.
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration); // Apply this configuration to all paths.
		return source;
	}
}