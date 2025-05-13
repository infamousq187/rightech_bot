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

            try {
                switch (messageText) {
                    case "/start":
                        SendMessage startMessage = new SendMessage();
                        startMessage.setChatId(String.valueOf(chatId));
                        startMessage.setText("Привет! Я бот для управления уличным освещением через платформу Rightech. Используй команды:\n" +
                                "/devices - показать список доступных устройств\n" +
                                "/status - проверить состояние фонарей\n" +
                                "/turn_on - включить фонарь\n" +
                                "/turn_off - выключить фонарь");
                        execute(startMessage);
                        break;
                    case "/devices":
                        for (String message : rightechService.getProjectObjects()) {
                            SendMessage deviceMessage = new SendMessage();
                            deviceMessage.setChatId(String.valueOf(chatId));
                            deviceMessage.setText(message);
                            execute(deviceMessage);
                        }
                        break;
                    case "/status":
                        SendMessage statusMessage = new SendMessage();
                        statusMessage.setChatId(String.valueOf(chatId));
                        statusMessage.setText("Состояние фонаря: " + rightechService.getLightStatus(LIGHT_ID));
                        execute(statusMessage);
                        break;
                    case "/turn_on":
                        SendMessage turnOnMessage = new SendMessage();
                        turnOnMessage.setChatId(String.valueOf(chatId));
                        turnOnMessage.setText(rightechService.turnLightOn(LIGHT_ID));
                        execute(turnOnMessage);
                        break;
                    case "/turn_off":
                        SendMessage turnOffMessage = new SendMessage();
                        turnOffMessage.setChatId(String.valueOf(chatId));
                        turnOffMessage.setText(rightechService.turnLightOff(LIGHT_ID));
                        execute(turnOffMessage);
                        break;
                    default:
                        SendMessage unknownMessage = new SendMessage();
                        unknownMessage.setChatId(String.valueOf(chatId));
                        unknownMessage.setText("Неизвестная команда. Используй /start, чтобы увидеть доступные команды.");
                        execute(unknownMessage);
                        break;
                }
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
}