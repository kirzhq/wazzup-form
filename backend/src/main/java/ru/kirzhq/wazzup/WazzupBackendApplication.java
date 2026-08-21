package ru.kirzhq.wazzup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import ru.kirzhq.wazzup.config.WazzupPartnerProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties(WazzupPartnerProperties.class)
@EnableScheduling
public class WazzupBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(WazzupBackendApplication.class, args);
	}

}
