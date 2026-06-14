package com.example.CalCol.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private final DatabaseUserDetailsService userDetailsService;
	private final AuthenticationSuccessHandler loginSuccessHandler;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	public SecurityConfig(DatabaseUserDetailsService userDetailsService,
			@Lazy AuthenticationSuccessHandler loginSuccessHandler,
			JwtAuthenticationFilter jwtAuthenticationFilter) {
		this.userDetailsService = userDetailsService;
		this.loginSuccessHandler = loginSuccessHandler;
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/", "/welcome", "/login", "/error").permitAll()
				.requestMatchers("/register", "/forgot-password", "/reset-password").permitAll()
				.requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
				.requestMatchers("/calculators", "/calculators/**").permitAll()
				.requestMatchers("/uploads/**").permitAll()
				.requestMatchers("/h2-console/**").permitAll()
				.requestMatchers("/share/**").permitAll()
				.requestMatchers("/api/**").authenticated()
				.requestMatchers("/admin/**").hasRole("ADMIN")
				.requestMatchers("/profile/**").authenticated()
				.anyRequest().authenticated()
			)
			.csrf(csrf -> csrf
				.ignoringRequestMatchers("/h2-console/**", "/api/**")
			)
			.headers(headers -> headers
				.frameOptions(frame -> frame.sameOrigin())
			)
			.userDetailsService(userDetailsService)
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.httpBasic(httpBasic -> {})
			.formLogin(form -> form
				.successHandler(loginSuccessHandler)
				.permitAll()
			)
			.logout(logout -> logout
				.logoutSuccessUrl("/")
				.permitAll()
			);
		return http.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
