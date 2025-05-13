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
    private static final String LIGHT_ID = "light1"; // ID фонаря в системе Rightech

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
            long chatId = update.getMessage().getChatId();

            try {
                switch (messageText) {
                    case "/start":
                        sendMessage(chatId, "Привет! Я бот для управления уличным освещением через платформу Rightech. Используй команды:\n" +
                                "/devices - показать список доступных устройств\n" +
                                "/status - проверить состояние фонарей\n" +
                                "/turn_on - включить фонарь\n" +
                                "/turn_off - выключить фонарь");
                        break;
                    case "/devices":
                        for (String message : rightechService.getProjectObjects()) {
                            sendMessage(chatId, message);
                        }
                        break;
                    case "/status":
                        sendMessage(chatId, "Состояние фонаря: " + rightechService.getLightStatus(LIGHT_ID));
                        break;
                    case "/turn_on":
                        sendMessage(chatId, rightechService.turnLightOn(LIGHT_ID));
                        break;
                    case "/turn_off":
                        sendMessage(chatId, rightechService.turnLightOff(LIGHT_ID));
                        break;
                    default:
                        sendMessage(chatId, "Неизвестная команда. Используй /start, чтобы увидеть доступные команды.");
                        break;
                }
            } catch (TelegramApiException e) {
                log.error("Error sending message", e);
            }
        }
    }
}