package tgbot.rightech_bot;

import lombok.Getter;
import lombok.Setter;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Getter
@Setter
public class RightechBot extends TelegramLongPollingBot {

    private String botUsername;
    private String botToken;

    public RightechBot(String botUsername, String botToken) {
        this.botUsername = botUsername;
        this.botToken = botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
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
                    message.setText("Привет! Я бот для управления уличным освещением. Используй команды:\n" +
                            "/status - проверить состояние фонарей\n" +
                            "/turn_on - включить фонарь\n" +
                            "/turn_off - выключить фонарь");
                    break;
                case "/status":
                    message.setText("Состояние фонарей: Фонарь №1 включён, Фонарь №2 выключен. (Пока это заглушка)");
                    break;
                case "/turn_on":
                    message.setText("Фонарь включён! (Пока это заглушка)");
                    break;
                case "/turn_off":
                    message.setText("Фонарь выключен! (Пока это заглушка)");
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