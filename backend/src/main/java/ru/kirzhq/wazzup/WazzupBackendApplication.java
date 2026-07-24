package ru.kirzhq.wazzup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class WazzupBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(WazzupBackendApplication.class, args);
	}

}
