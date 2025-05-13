package tgbot.rightech_bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tgbot.rightech_bot.config.RightechConfig;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RightechService {
    private final RightechConfig rightechConfig;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final int MAX_MESSAGE_LENGTH = 3000; // Уменьшаем лимит для надежности
    private static final int MAX_DEVICES_PER_MESSAGE = 5; // Уменьшаем количество устройств в сообщении

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + rightechConfig.getToken());
        headers.set("Content-Type", "application/json");
        return headers;
    }

    public List<String> getProjectObjects() {
        try {
            String url = rightechConfig.getApiUrl() + "/things?project=" + rightechConfig.getProjectId();
            log.info("Requesting URL: {}", url);
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            JSONArray objects = new JSONArray(response.getBody());
            log.info("Received {} objects from API", objects.length());
            
            List<String> messages = new ArrayList<>();
            StringBuilder currentMessage = new StringBuilder("Доступные устройства:\n");
            int deviceCount = 0;
            int messageCount = 1;
            
            for (int i = 0; i < objects.length(); i++) {
                JSONObject object = objects.getJSONObject(i);
                String deviceInfo = String.format("%d. %s (ID: %s)\n", 
                    i + 1,
                    object.getString("name"),
                    object.getString("id"));
                
                log.debug("Device info length: {}", deviceInfo.length());
                
                // Если добавление нового устройства превысит лимит или достигнем MAX_DEVICES_PER_MESSAGE
                if (currentMessage.length() + deviceInfo.length() > MAX_MESSAGE_LENGTH || deviceCount >= MAX_DEVICES_PER_MESSAGE) {
                    String message = currentMessage.toString();
                    log.info("Adding message {} with length {}", messageCount, message.length());
                    messages.add(message);
                    messageCount++;
                    currentMessage = new StringBuilder(String.format("Доступные устройства (часть %d):\n", messageCount));
                    deviceCount = 0;
                }
                
                currentMessage.append(deviceInfo);
                deviceCount++;
            }
            
            // Добавляем последнее сообщение, если оно не пустое
            if (currentMessage.length() > 0) {
                String message = currentMessage.toString();
                log.info("Adding final message {} with length {}", messageCount, message.length());
                messages.add(message);
            }
            
            if (messages.isEmpty()) {
                messages.add("Устройства не найдены");
            }
            
            log.info("Total messages to send: {}", messages.size());
            for (int i = 0; i < messages.size(); i++) {
                log.info("Message {} length: {}", i + 1, messages.get(i).length());
            }
            
            return messages;
        } catch (Exception e) {
            log.error("Error getting project objects. URL: {}", rightechConfig.getApiUrl() + "/things?project=" + rightechConfig.getProjectId(), e);
            return List.of("Ошибка получения списка устройств: " + e.getMessage());
        }
    }

    public String getLightStatus(String lightId) {
        try {
            String url = rightechConfig.getApiUrl() + "/things/" + lightId + "?project=" + rightechConfig.getProjectId();
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            JSONObject object = new JSONObject(response.getBody());
            return object.getJSONObject("state").getString("power") + " (яркость: " + 
                   object.getJSONObject("state").getInt("brightness") + "%)";
        } catch (Exception e) {
            log.error("Error getting light status", e);
            return "Ошибка получения статуса фонаря";
        }
    }

    public String turnLightOn(String lightId) {
        try {
            String url = rightechConfig.getApiUrl() + "/things/" + lightId + "/command?project=" + rightechConfig.getProjectId();
            JSONObject command = new JSONObject();
            command.put("command", "turn_on");
            command.put("brightness", 100);

            HttpEntity<String> entity = new HttpEntity<>(command.toString(), createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            return "Фонарь успешно включен";
        } catch (Exception e) {
            log.error("Error turning light on", e);
            return "Ошибка включения фонаря";
        }
    }

    public String turnLightOff(String lightId) {
        try {
            String url = rightechConfig.getApiUrl() + "/things/" + lightId + "/command?project=" + rightechConfig.getProjectId();
            JSONObject command = new JSONObject();
            command.put("command", "turn_off");

            HttpEntity<String> entity = new HttpEntity<>(command.toString(), createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            return "Фонарь успешно выключен";
        } catch (Exception e) {
            log.error("Error turning light off", e);
            return "Ошибка выключения фонаря";
        }
    }
} 