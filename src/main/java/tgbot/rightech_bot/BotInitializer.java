package tgbot.rightech_bot;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
public class BotInitializer {
    private final TelegramBotsApi telegramBotsApi;
    private final RightechBot rightechBot;

    @PostConstruct
    public void init() {
        try {
            telegramBotsApi.registerBot(rightechBot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
} 