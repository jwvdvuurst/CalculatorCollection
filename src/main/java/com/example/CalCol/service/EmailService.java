package com.example.CalCol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "spring.mail.host")
@Slf4j
public class EmailService {

	private final JavaMailSender mailSender;
	private final TemplateEngine templateEngine;

	@Value("${spring.mail.from:Calculator Collector <noreply@calculatorcollector.com>}")
	private String fromEmail;

	@Value("${app.base-url:http://localhost:8080}")
	private String baseUrl;

	@Autowired
	public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
		this.mailSender = mailSender;
		this.templateEngine = templateEngine;
	}

	public void sendSimpleEmail(String to, String subject, String text) {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setFrom(fromEmail);
			message.setTo(to);
			message.setSubject(subject);
			message.setText(text);
			mailSender.send(message);
			log.info("Email sent successfully to: {}", to);
		} catch (Exception e) {
			log.error("Failed to send email to: {}", to, e);
			throw new RuntimeException("Failed to send email", e);
		}
	}

	public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			
			helper.setFrom(fromEmail);
			helper.setTo(to);
			helper.setSubject(subject);
			
			Context context = new Context();
			if (variables != null) {
				variables.forEach(context::setVariable);
			}
			context.setVariable("baseUrl", baseUrl);
			
			String htmlContent = templateEngine.process(templateName, context);
			helper.setText(htmlContent, true);
			
			mailSender.send(message);
			log.info("HTML email sent successfully to: {}", to);
		} catch (MessagingException e) {
			log.error("Failed to send HTML email to: {}", to, e);
			throw new RuntimeException("Failed to send HTML email", e);
		}
	}

	public void sendCollectionSharedEmail(String to, String sharedBy, String shareTitle, String shareUrl) {
		Map<String, Object> variables = Map.of(
			"sharedBy", sharedBy,
			"shareTitle", shareTitle != null ? shareTitle : "Calculator Collection",
			"shareUrl", shareUrl
		);
		sendHtmlEmail(to, "Calculator Collection Shared with You", "email/collection-shared", variables);
	}

	public void sendNewCalculatorNotification(String to, String calculatorModel, String manufacturer, Long calculatorId) {
		Map<String, Object> variables = Map.of(
			"calculatorModel", calculatorModel,
			"manufacturer", manufacturer,
			"calculatorUrl", baseUrl + "/calculators/" + calculatorId
		);
		sendHtmlEmail(to, "New Calculator Added: " + calculatorModel, "email/new-calculator", variables);
	}

	public void sendProposalApprovedEmail(String to, String calculatorModel, String manufacturer, Long calculatorId) {
		Map<String, Object> variables = Map.of(
			"calculatorModel", calculatorModel,
			"manufacturer", manufacturer,
			"calculatorUrl", baseUrl + "/calculators/" + calculatorId
		);
		sendHtmlEmail(to, "Your Calculator Proposal Was Approved", "email/proposal-approved", variables);
	}

	public void sendProposalRejectedEmail(String to, String calculatorModel, String manufacturer, String reason) {
		Map<String, Object> variables = Map.of(
			"calculatorModel", calculatorModel,
			"manufacturer", manufacturer,
			"reason", reason != null ? reason : "No reason provided"
		);
		sendHtmlEmail(to, "Your Calculator Proposal Was Rejected", "email/proposal-rejected", variables);
	}

	public void sendCollectionSummaryEmail(String to, String username, long collectionCount, 
			Map<String, Long> byManufacturer, List<String> recentAdditions) {
		Map<String, Object> variables = Map.of(
			"username", username,
			"collectionCount", collectionCount,
			"byManufacturer", byManufacturer,
			"recentAdditions", recentAdditions != null ? recentAdditions : List.of()
		);
		sendHtmlEmail(to, "Your Calculator Collection Summary", "email/collection-summary", variables);
	}
}

