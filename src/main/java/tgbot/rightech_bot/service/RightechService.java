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
    private static final String MODEL_ID = "682304b420b46dbb6c1f6af6"; // ID модели устройства

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + rightechConfig.getToken());
        headers.set("Content-Type", "application/json");
        log.debug("Created headers: {}", headers);
        return headers;
    }


    public String turnLightOn(String lightId) {
        try {
            // Используем эндпоинт команд
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
                return "Фонарь успешно включен на 100% яркости\n\n" + getDeviceStatus(lightId);
            } else {
                JSONObject errorResponse = new JSONObject(response.getBody());
                String errorMessage = "Ошибка включения фонаря: ";
                errorMessage += errorResponse.optString("message", response.getBody());
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
            // Используем эндпоинт команд
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
                return "Фонарь успешно выключен\n\n" + getDeviceStatus(lightId);
            } else {
                JSONObject errorResponse = new JSONObject(response.getBody());
                String errorMessage = "Ошибка выключения фонаря: ";
                errorMessage += errorResponse.optString("message", response.getBody());
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

    public String getDeviceStatus(String lightId) {
        try {
            String url = rightechConfig.getApiUrl() + "/v1/objects/" + lightId;
            log.info("Getting device status. URL: {}", url);
            
            HttpEntity<String> entity = new HttpEntity<>(createHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                JSONObject object = new JSONObject(response.getBody());
                StringBuilder status = new StringBuilder();
                
                // Основная информация об устройстве
                status.append("📱 Информация об устройстве:\n");
                status.append(String.format("ID: %s\n", object.optString("id", "неизвестно")));
                status.append(String.format("Название: %s\n", object.optString("name", "неизвестно")));
                status.append(String.format("Активно: %s\n", object.optBoolean("active", false) ? "✅" : "❌"));
                
                // Информация о состоянии
                if (object.has("state")) {
                    JSONObject state = object.getJSONObject("state");
                    boolean isOnline = state.optBoolean("online", false);
                    long lastUpdateTime = state.optLong("time", 0);
                    long currentTime = System.currentTimeMillis();
                    long timeDiff = currentTime - lastUpdateTime;
                    
                    log.info("Device state: online={}, lastUpdate={}, timeDiff={}ms", 
                            isOnline, new java.util.Date(lastUpdateTime), timeDiff);
                    
                    status.append(String.format("Статус: %s\n", isOnline ? "🟢 онлайн" : "🔴 офлайн"));
                    
                    if (state.has("payload")) {
                        try {
                            String payloadStr = state.getString("payload");
                            log.debug("Raw payload: {}", payloadStr);
                            
                            // Пробуем разные форматы payload
                            JSONObject payload;
                            try {
                                // Сначала пробуем как JSON строку
                                payload = new JSONObject(payloadStr);
                            } catch (Exception e) {
                                try {
                                    // Если не получилось, пробуем как обычную строку
                                    payload = new JSONObject();
                                    // Разбиваем строку на пары ключ-значение
                                    String[] pairs = payloadStr.replace("{", "").replace("}", "").split(",");
                                    for (String pair : pairs) {
                                        String[] keyValue = pair.split(":");
                                        if (keyValue.length == 2) {
                                            String key = keyValue[0].trim();
                                            String value = keyValue[1].trim();
                                            // Пробуем преобразовать значение в число
                                            try {
                                                if (value.contains(".")) {
                                                    payload.put(key, Double.parseDouble(value));
                                                } else {
                                                    payload.put(key, Integer.parseInt(value));
                                                }
                                            } catch (NumberFormatException nfe) {
                                                // Если не число, оставляем как строку
                                                payload.put(key, value);
                                            }
                                        }
                                    }
                                } catch (Exception e2) {
                                    log.error("Failed to parse payload in both formats: {}", e2.getMessage());
                                    throw e2;
                                }
                            }
                            
//                            status.append("\n💡 Состояние фонаря:\n");
//
//                            // Основные параметры лампы
//                            if (payload.has("brightness")) {
//                                status.append(String.format("Яркость: %d%%\n", payload.optInt("brightness", 0)));
//                            }
//                            if (payload.has("lux")) {
//                                status.append(String.format("Освещенность: %d lux\n", payload.optInt("lux", 0)));
//                            }
//                            if (payload.has("lamp_life")) {
//                                status.append(String.format("Ресурс лампы: %.2f часов\n", payload.optDouble("lamp_life", 0.0)));
//                            }
//                            if (payload.has("power")) {
//                                status.append(String.format("Питание: %s\n", payload.optBoolean("power", false) ? "включено" : "выключено"));
//                            }
//                            if (payload.has("motion")) {
//                                status.append(String.format("Движение: %s\n", payload.optBoolean("motion", false) ? "есть" : "нет"));
//                            }
                            
                        } catch (Exception e) {
                            log.error("Error parsing state payload: {} for payload: {}", e.getMessage(), state.optString("payload"));
                            status.append("\n⚠️ Ошибка чтения состояния: ").append(e.getMessage());
                        }
                    }
                } else {
                    status.append("Статус: 🔴 офлайн (нет данных о состоянии)\n");
                }
                
                return status.toString();
            } else {
                log.error("Failed to get device status. Response: {}", response.getBody());
                return "Ошибка получения статуса устройства: " + response.getBody();
            }
        } catch (Exception e) {
            log.error("Error getting device status: {}", e.getMessage());
            return "Ошибка получения статуса устройства: " + e.getMessage();
        }
    }
} 