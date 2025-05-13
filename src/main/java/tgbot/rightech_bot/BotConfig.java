package tgbot.rightech_bot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import tgbot.rightech_bot.service.RightechService;

@Configuration
public class BotConfig {

    @Bean
    public RightechBot rightechBot(RightechService rightechService) {
        RightechBot bot = new RightechBot("rightechProject_bot", "7432499710:AAGBV6llMQWi18WN8bWY2f7i84US2L1Mx7U", rightechService);
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
        return bot;
    }
}