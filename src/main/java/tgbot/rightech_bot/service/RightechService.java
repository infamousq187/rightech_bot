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
            List<String> messages = new ArrayList<>();
            StringBuilder currentMessage = new StringBuilder("Доступные устройства:\n");
            int deviceCount = 0;
            
            for (int i = 0; i < objects.length(); i++) {
                JSONObject object = objects.getJSONObject(i);
                String deviceInfo = "- " + object.getString("name") + 
                                  " (ID: " + object.getString("id") + ")\n";
                
                // Если добавление нового устройства превысит лимит или достигнем 10 устройств
                if (currentMessage.length() + deviceInfo.length() > 4000 || deviceCount >= 10) {
                    messages.add(currentMessage.toString());
                    currentMessage = new StringBuilder("Доступные устройства (продолжение):\n");
                    deviceCount = 0;
                }
                
                currentMessage.append(deviceInfo);
                deviceCount++;
            }
            
            // Добавляем последнее сообщение, если оно не пустое
            if (currentMessage.length() > 0) {
                messages.add(currentMessage.toString());
            }
            
            if (messages.isEmpty()) {
                messages.add("Устройства не найдены");
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