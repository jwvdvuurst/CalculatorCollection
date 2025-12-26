package com.example.CalCol.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final DatabaseUserDetailsService userDetailsService;
	private final AuthenticationSuccessHandler loginSuccessHandler;

	public SecurityConfig(DatabaseUserDetailsService userDetailsService, 
			@Lazy AuthenticationSuccessHandler loginSuccessHandler) {
		this.userDetailsService = userDetailsService;
		this.loginSuccessHandler = loginSuccessHandler;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/", "/welcome", "/login", "/error").permitAll()
				.requestMatchers("/calculators", "/calculators/**").permitAll()
				.requestMatchers("/uploads/**").permitAll()
				.requestMatchers("/h2-console/**").permitAll()
				.requestMatchers("/share/**").permitAll()
				.requestMatchers("/admin/**").hasRole("ADMIN")
				.requestMatchers("/profile/**").authenticated()
				.anyRequest().authenticated()
			)
			.csrf(csrf -> csrf
				.ignoringRequestMatchers("/h2-console/**")
			)
			.headers(headers -> headers
				.frameOptions(frame -> frame.sameOrigin())
			)
			.userDetailsService(userDetailsService)
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
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}

