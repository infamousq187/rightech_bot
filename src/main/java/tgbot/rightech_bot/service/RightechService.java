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
    private static final int MAX_MESSAGE_LENGTH = 1000; // Значительно уменьшаем лимит
    private static final int MAX_DEVICES_PER_MESSAGE = 3; // Уменьшаем количество устройств в сообщении

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + rightechConfig.getToken());
        headers.set("Content-Type", "application/json");
        log.debug("Created headers: {}", headers);
        return headers;
    }

    private String truncateMessage(String message) {
        if (message.length() > MAX_MESSAGE_LENGTH) {
            log.warn("Message too long ({} chars), truncating to {}", message.length(), MAX_MESSAGE_LENGTH);
            return message.substring(0, MAX_MESSAGE_LENGTH - 3) + "...";
        }
        return message;
    }

    private boolean checkProjectAccess() {
        try {
            // Проверяем доступ через информацию о проекте
            String url = rightechConfig.getApiUrl() + "/v1/objects?project=" + rightechConfig.getProjectId() + "&limit=1";
            log.info("Checking project access. URL: {}", url);
            
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            log.debug("Project check response status: {}", response.getStatusCode());
            log.debug("Project check response body: {}", response.getBody());
            
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Error checking project access: {}", e.getMessage());
            return false;
        }
    }

    public List<String> getProjectObjects() {
        try {
            if (!checkProjectAccess()) {
                return List.of("Ошибка доступа к проекту. Проверьте project ID и права доступа токена.");
            }

            // Получаем состояние объекта через /objects/{projectId}/store
            String url = rightechConfig.getApiUrl() + "/v1/objects/" + rightechConfig.getProjectId() + "/store";
            log.info("Making GET request to URL: {}", url);
            log.debug("Full request details:");
            log.debug("URL: {}", url);
            log.debug("Method: GET");
            log.debug("Headers: {}", createHeaders());
            
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            log.debug("Response status: {}", response.getStatusCode());
            log.debug("Response headers: {}", response.getHeaders());
            log.debug("Response body: {}", response.getBody());
            
            JSONObject object = new JSONObject(response.getBody());
            log.info("Received object state from API");
            
            // Формируем сообщение о состоянии фонаря
            JSONObject state = object.getJSONObject("state");
            JSONObject payload = new JSONObject(state.getString("payload"));
            
            StringBuilder message = new StringBuilder();
            message.append("Состояние фонаря:\n");
            message.append(String.format("Яркость: %d%%\n", payload.getInt("brightness")));
            message.append(String.format("Освещенность: %d lux\n", payload.getInt("lux")));
            message.append(String.format("Движение: %s\n", payload.getBoolean("motion") ? "есть" : "нет"));
            message.append(String.format("Ресурс лампы: %.2f часов\n", payload.getDouble("lamp_life")));
            
            String finalMessage = truncateMessage(message.toString());
            log.info("Message length: {}", finalMessage.length());
            return List.of(finalMessage);
            
        } catch (Exception e) {
            log.error("Error getting project objects. Full request details:", e);
            log.error("URL: {}", rightechConfig.getApiUrl() + "/v1/objects/" + rightechConfig.getProjectId() + "/store");
            log.error("Headers: {}", createHeaders());
            return List.of("Ошибка получения состояния фонаря: " + e.getMessage());
        }
    }

    public String getLightStatus(String lightId) {
        try {
            String url = rightechConfig.getApiUrl() + "/things/" + lightId + "?project=" + rightechConfig.getProjectId();
            log.debug("Making GET request to URL: {}", url);
            log.debug("Full request details:");
            log.debug("URL: {}", url);
            log.debug("Method: GET");
            log.debug("Headers: {}", createHeaders());
            
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            log.debug("Response status: {}", response.getStatusCode());
            log.debug("Response headers: {}", response.getHeaders());
            log.debug("Response body: {}", response.getBody());
            
            JSONObject object = new JSONObject(response.getBody());
            return object.getJSONObject("state").getString("power") + " (яркость: " + 
                   object.getJSONObject("state").getInt("brightness") + "%)";
        } catch (Exception e) {
            log.error("Error getting light status for device {}. Full request details:", lightId, e);
            log.error("URL: {}", rightechConfig.getApiUrl() + "/things/" + lightId + "?project=" + rightechConfig.getProjectId());
            log.error("Headers: {}", createHeaders());
            return "Ошибка получения статуса фонаря";
        }
    }

    public String turnLightOn(String lightId) {
        try {
            String url = rightechConfig.getApiUrl() + "/things/" + lightId + "/command?project=" + rightechConfig.getProjectId();
            log.debug("Making POST request to URL: {}", url);
            log.debug("Full request details:");
            log.debug("URL: {}", url);
            log.debug("Method: POST");
            log.debug("Headers: {}", createHeaders());
            
            JSONObject command = new JSONObject();
            command.put("command", "turn_on");
            command.put("brightness", 100);
            log.debug("Request body: {}", command.toString());

            HttpEntity<String> entity = new HttpEntity<>(command.toString(), createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            log.debug("Response status: {}", response.getStatusCode());
            log.debug("Response headers: {}", response.getHeaders());
            log.debug("Response body: {}", response.getBody());
            
            return "Фонарь успешно включен";
        } catch (Exception e) {
            log.error("Error turning light on for device {}. Full request details:", lightId, e);
            log.error("URL: {}", rightechConfig.getApiUrl() + "/things/" + lightId + "/command?project=" + rightechConfig.getProjectId());
            log.error("Headers: {}", createHeaders());
            return "Ошибка включения фонаря";
        }
    }

    public String turnLightOff(String lightId) {
        try {
            String url = rightechConfig.getApiUrl() + "/things/" + lightId + "/command?project=" + rightechConfig.getProjectId();
            log.debug("Making POST request to URL: {}", url);
            log.debug("Full request details:");
            log.debug("URL: {}", url);
            log.debug("Method: POST");
            log.debug("Headers: {}", createHeaders());
            
            JSONObject command = new JSONObject();
            command.put("command", "turn_off");
            log.debug("Request body: {}", command.toString());

            HttpEntity<String> entity = new HttpEntity<>(command.toString(), createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            log.debug("Response status: {}", response.getStatusCode());
            log.debug("Response headers: {}", response.getHeaders());
            log.debug("Response body: {}", response.getBody());
            
            return "Фонарь успешно выключен";
        } catch (Exception e) {
            log.error("Error turning light off for device {}. Full request details:", lightId, e);
            log.error("URL: {}", rightechConfig.getApiUrl() + "/things/" + lightId + "/command?project=" + rightechConfig.getProjectId());
            log.error("Headers: {}", createHeaders());
            return "Ошибка выключения фонаря";
        }
    }
} 