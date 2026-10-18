package com.example.CalCol.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTest {
	private final ApplicationContextRunner runner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(MailSenderAutoConfiguration.class))
		.withUserConfiguration(EmailService.class)
		.withBean(TemplateEngine.class, TemplateEngine::new);

	@Test void missingMailConfigurationDoesNotPreventStartup() {
		runner.run(context -> {
			assertThat(context).hasNotFailed().doesNotHaveBean(JavaMailSender.class);
			EmailService service = context.getBean(EmailService.class);
			assertThat(service.isAvailable()).isFalse();
			assertThatThrownBy(() -> service.sendSimpleEmail("test@example.invalid", "Test", "Text"))
				.isInstanceOf(IllegalStateException.class).hasMessage("Email service is not configured");
			assertThatThrownBy(() -> service.sendHtmlEmail("test@example.invalid", "Test", "unused", null))
				.isInstanceOf(IllegalStateException.class).hasMessage("Email service is not configured");
		});
	}

	@Test void configuredSmtpHostEnablesMail() {
		runner.withPropertyValues("spring.mail.host=localhost", "spring.mail.port=2525").run(context -> {
			assertThat(context).hasNotFailed().hasSingleBean(JavaMailSender.class);
			assertThat(context.getBean(EmailService.class).isAvailable()).isTrue();
		});
	}

	@Test void customMailSenderWorksWithoutHostProperty() {
		JavaMailSender sender = mock(JavaMailSender.class);
		runner.withBean(JavaMailSender.class, () -> sender).run(context -> {
			EmailService service = context.getBean(EmailService.class);
			assertThat(service.isAvailable()).isTrue();
			service.sendSimpleEmail("test@example.invalid", "Test", "Text");
			verify(sender).send(any(SimpleMailMessage.class));
		});
	}
}
