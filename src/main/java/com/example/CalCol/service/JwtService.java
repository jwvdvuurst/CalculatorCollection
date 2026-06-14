package com.example.CalCol.service;

import com.example.CalCol.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class JwtService {

	private final JwtProperties jwtProperties;

	public JwtService(JwtProperties jwtProperties) {
		this.jwtProperties = jwtProperties;
	}

	public String generateToken(UserDetails userDetails) {
		Map<String, Object> claims = new HashMap<>();
		String authorities = userDetails.getAuthorities().stream()
			.map(GrantedAuthority::getAuthority)
			.collect(Collectors.joining(","));
		claims.put("roles", authorities);
		return buildToken(claims, userDetails.getUsername());
	}

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		String username = extractUsername(token);
		return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
	}

	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	private Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}

	private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parser()
			.verifyWith(getSigningKey())
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	private String buildToken(Map<String, Object> extraClaims, String subject) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + jwtProperties.getExpirationMs());
		return Jwts.builder()
			.claims(extraClaims)
			.subject(subject)
			.issuedAt(now)
			.expiration(expiry)
			.signWith(getSigningKey())
			.compact();
	}

	private SecretKey getSigningKey() {
		byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}

	public long getExpirationMs() {
		return jwtProperties.getExpirationMs();
	}
}
