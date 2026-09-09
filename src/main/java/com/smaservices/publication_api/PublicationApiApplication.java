package com.smaservices.publication_api;

import com.smaservices.publication_api.entity.User;
import com.smaservices.publication_api.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class PublicationApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PublicationApiApplication.class, args);
	}

	// Ce code s'exécutera à chaque démarrage de l'application
	@Bean
	public CommandLineRunner initTestUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			if (userRepository.findByEmail("admin@emsi.ma").isEmpty()) {
				User user = new User();
				user.setEmail("admin@emsi.ma");
				// L'encodeur de Spring hache le mot de passe de manière garantie
				user.setPasswordHash(passwordEncoder.encode("password123"));
				userRepository.save(user);
				System.out.println("====== Utilisateur de test créé avec succès ! ======");
			}
		};
	}
}