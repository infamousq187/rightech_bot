package tgbot.rightech_bot.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rightech")
@Getter
@Setter
public class RightechConfig {
    private String apiUrl = "https://dev.rightech.io/api/v1";
    private String token;
    private String projectId;
} 