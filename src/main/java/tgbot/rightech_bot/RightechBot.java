package tgbot.rightech_bot;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import tgbot.rightech_bot.service.RightechService;

@Slf4j
@Getter
@Setter
@Component
public class RightechBot extends TelegramLongPollingBot {

    private static final int MAX_MESSAGE_LENGTH = 1000; // Максимальная длина сообщения

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${telegram.bot.token}")
    private String botToken;

    private final RightechService rightechService;
    private static final String LIGHT_ID = "urfu-project"; // ID фонаря в системе Rightech

    public RightechBot(@Value("${telegram.bot.token}") String botToken, RightechService rightechService) {
        super(botToken);
        this.rightechService = rightechService;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    private void sendMessage(long chatId, String text) throws TelegramApiException {
        if (text.length() > MAX_MESSAGE_LENGTH) {
            log.warn("Message too long ({} chars), truncating to {}", text.length(), MAX_MESSAGE_LENGTH);
            text = text.substring(0, MAX_MESSAGE_LENGTH - 3) + "...";
        }
        
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        log.debug("Sending message with length: {}", text.length());
        execute(message);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();
            String response;

            switch (messageText) {
                case "/start" -> response = "Привет! Я бот для управления умным фонарем. Используйте следующие команды:\n" +
                        "/status - показать текущее состояние фонаря\n" +
                        "/turn_on - включить фонарь\n" +
                        "/turn_off - выключить фонарь";
                case "/turn_on" -> response = rightechService.turnLightOn(LIGHT_ID);
                case "/turn_off" -> response = rightechService.turnLightOff(LIGHT_ID);
                case "/status" -> response = rightechService.getDeviceStatus(LIGHT_ID);
                default -> response = "Неизвестная команда. Используйте /start для просмотра списка команд.";
            }

            SendMessage message = new SendMessage();
            message.setChatId(chatId);
            message.setText(response);
            log.debug("Sending message with length: {}", response.length());
            try {
                execute(message);
            } catch (TelegramApiException e) {
                log.error("Error sending message: {}", e.getMessage());
            }
        }
    }
}