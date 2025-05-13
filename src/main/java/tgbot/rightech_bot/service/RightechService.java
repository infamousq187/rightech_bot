package tgbot.rightech_bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tgbot.rightech_bot.config.RightechConfig;

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

    public String getLightStatus(String lightId) {
        try {
            String url = rightechConfig.getApiUrl() + "/projects/" + rightechConfig.getProjectId() + "/objects/" + lightId;
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
            String url = rightechConfig.getApiUrl() + "/projects/" + rightechConfig.getProjectId() + "/objects/" + lightId + "/command";
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
            String url = rightechConfig.getApiUrl() + "/projects/" + rightechConfig.getProjectId() + "/objects/" + lightId + "/command";
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