package com.example.CalCol.service;

/**
 * Exception thrown when an API rate limit (HTTP 429) is encountered
 */
public class RateLimitException extends Exception {
	
	public RateLimitException(String message) {
		super(message);
	}
	
	public RateLimitException(String message, Throwable cause) {
		super(message, cause);
	}
}
