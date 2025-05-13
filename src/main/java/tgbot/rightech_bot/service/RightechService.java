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

            // Получаем информацию о проекте, которая уже содержит состояние фонаря
            String url = rightechConfig.getApiUrl() + "/v1/objects?project=" + rightechConfig.getProjectId() + "&limit=1";
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
            
            JSONArray objects = new JSONArray(response.getBody());
            if (objects.length() == 0) {
                return List.of("Проект не содержит объектов");
            }

            JSONObject object = objects.getJSONObject(0);
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
            log.error("URL: {}", rightechConfig.getApiUrl() + "/v1/objects?project=" + rightechConfig.getProjectId() + "&limit=1");
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

    private boolean isDeviceOnline(String lightId) {
        try {
            String url = rightechConfig.getApiUrl() + "/v1/objects/" + lightId;
            log.info("Checking device status. URL: {}", url);
            
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject object = new JSONObject(response.getBody());
                boolean online = object.optBoolean("online", false);
                log.info("Device {} is {}", lightId, online ? "online" : "offline");
                return online;
            }
            log.error("Failed to get device status. Response: {}", response.getBody());
            return false;
        } catch (Exception e) {
            log.error("Error checking device status: {}", e.getMessage());
            return false;
        }
    }

    public String turnLightOn(String lightId) {
        try {
            // Проверяем состояние устройства перед отправкой команды
            if (!isDeviceOnline(lightId)) {
                return "Ошибка: устройство офлайн или недоступно. Пожалуйста, проверьте подключение устройства и попробуйте снова.";
            }

            // Используем правильный эндпоинт для отправки команд с именем команды ON
            String url = rightechConfig.getApiUrl() + "/v1/objects/" + lightId + "/commands/ON";
            log.info("Making POST request to URL: {}", url);
            log.debug("Full request details:");
            log.debug("URL: {}", url);
            log.debug("Method: POST");
            log.debug("Headers: {}", createHeaders());
            
            JSONObject command = new JSONObject();
            command.put("brightness", 100);
            log.debug("Request body: {}", command.toString());

            HttpEntity<String> entity = new HttpEntity<>(command.toString(), createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            log.debug("Response status: {}", response.getStatusCode());
            log.debug("Response headers: {}", response.getHeaders());
            log.debug("Response body: {}", response.getBody());
            
            if (response.getStatusCode().is2xxSuccessful()) {
                // После успешной команды ждем немного и проверяем состояние
                Thread.sleep(1000);
                List<String> status = getProjectObjects();
                return "Фонарь успешно включен на 100% яркости\n\n" + status.get(0);
            } else {
                JSONObject errorResponse = new JSONObject(response.getBody());
                String errorMessage = "Ошибка включения фонаря: ";
                if (errorResponse.has("tags") && errorResponse.getJSONArray("tags").toList().contains("error_offline")) {
                    errorMessage += "устройство офлайн или недоступно";
                } else {
                    errorMessage += errorResponse.optString("message", response.getBody());
                }
                log.error("Error response from API: {}", response.getBody());
                return errorMessage;
            }
        } catch (Exception e) {
            log.error("Error turning light on. Full request details:", e);
            log.error("URL: {}", rightechConfig.getApiUrl() + "/v1/objects/" + lightId + "/commands/ON");
            log.error("Headers: {}", createHeaders());
            return "Ошибка включения фонаря: " + e.getMessage();
        }
    }

    public String turnLightOff(String lightId) {
        try {
            // Проверяем состояние устройства перед отправкой команды
            if (!isDeviceOnline(lightId)) {
                return "Ошибка: устройство офлайн или недоступно. Пожалуйста, проверьте подключение устройства и попробуйте снова.";
            }

            // Используем правильный эндпоинт для отправки команд с именем команды OFF
            String url = rightechConfig.getApiUrl() + "/v1/objects/" + lightId + "/commands/OFF";
            log.info("Making POST request to URL: {}", url);
            log.debug("Full request details:");
            log.debug("URL: {}", url);
            log.debug("Method: POST");
            log.debug("Headers: {}", createHeaders());
            
            JSONObject command = new JSONObject();
            log.debug("Request body: {}", command.toString());

            HttpEntity<String> entity = new HttpEntity<>(command.toString(), createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            
            log.debug("Response status: {}", response.getStatusCode());
            log.debug("Response headers: {}", response.getHeaders());
            log.debug("Response body: {}", response.getBody());
            
            if (response.getStatusCode().is2xxSuccessful()) {
                // После успешной команды ждем немного и проверяем состояние
                Thread.sleep(1000);
                List<String> status = getProjectObjects();
                return "Фонарь успешно выключен\n\n" + status.get(0);
            } else {
                JSONObject errorResponse = new JSONObject(response.getBody());
                String errorMessage = "Ошибка выключения фонаря: ";
                if (errorResponse.has("tags") && errorResponse.getJSONArray("tags").toList().contains("error_offline")) {
                    errorMessage += "устройство офлайн или недоступно";
                } else {
                    errorMessage += errorResponse.optString("message", response.getBody());
                }
                log.error("Error response from API: {}", response.getBody());
                return errorMessage;
            }
        } catch (Exception e) {
            log.error("Error turning light off. Full request details:", e);
            log.error("URL: {}", rightechConfig.getApiUrl() + "/v1/objects/" + lightId + "/commands/OFF");
            log.error("Headers: {}", createHeaders());
            return "Ошибка выключения фонаря: " + e.getMessage();
        }
    }
} 