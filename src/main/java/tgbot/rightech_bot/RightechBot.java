package tgbot.rightech_bot;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import tgbot.rightech_bot.service.RightechService;

@Getter
@Setter
@Component
public class RightechBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    private final RightechService rightechService;
    private static final String LIGHT_ID = "light1"; // ID фонаря в системе Rightech

    public RightechBot(@Value("${telegram.bot.token}") String botToken, RightechService rightechService) {
        super(botToken);
        this.rightechService = rightechService;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));

            switch (messageText) {
                case "/start":
                    message.setText("Привет! Я бот для управления уличным освещением через платформу Rightech. Используй команды:\n" +
                            "/devices - показать список доступных устройств\n" +
                            "/status - проверить состояние фонарей\n" +
                            "/turn_on - включить фонарь\n" +
                            "/turn_off - выключить фонарь");
                    break;
                case "/devices":
                    message.setText(rightechService.getProjectObjects());
                    break;
                case "/status":
                    message.setText("Состояние фонаря: " + rightechService.getLightStatus(LIGHT_ID));
                    break;
                case "/turn_on":
                    message.setText(rightechService.turnLightOn(LIGHT_ID));
                    break;
                case "/turn_off":
                    message.setText(rightechService.turnLightOff(LIGHT_ID));
                    break;
                default:
                    message.setText("Неизвестная команда. Используй /start, чтобы увидеть доступные команды.");
                    break;
            }

            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
}