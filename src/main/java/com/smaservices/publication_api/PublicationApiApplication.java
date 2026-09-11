package com.smaservices.publication_api;

import com.smaservices.publication_api.entity.User;
import com.smaservices.publication_api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;

@SpringBootApplication
public class PublicationApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(
				PublicationApiApplication.class,
				args
		);
	}

	@Bean
	@Profile("dev")
	public CommandLineRunner initDevUser(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			@Value("${DEV_ADMIN_EMAIL:}")
			String adminEmail,
			@Value("${DEV_ADMIN_PASSWORD:}")
			String adminPassword) {

		return args -> {

			if (adminEmail.isBlank()
					|| adminPassword.isBlank()) {

				return;
			}

			String normalizedEmail =
					adminEmail
							.trim()
							.toLowerCase(Locale.ROOT);

			if (userRepository
					.findByEmail(normalizedEmail)
					.isPresent()) {

				return;
			}

			User user = new User();

			user.setEmail(
					normalizedEmail
			);

			user.setPasswordHash(
					passwordEncoder.encode(
							adminPassword
					)
			);

			userRepository.save(user);

			System.out.println(
					"====== Utilisateur de développement créé avec succès ! ======"
			);
		};
	}
}