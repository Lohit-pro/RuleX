package org.rulex.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenAIService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAIService.class);
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.enabled:false}")
    private boolean enabled;

    @Value("${openai.model:gpt-4}")
    private String model;

    public Map<String, ColumnInference> inferColumnTypes(Map<String, List<String>> columnSamples) {
        if (!enabled || apiKey == null || apiKey.isEmpty()) {
            LOGGER.warn("OpenAI API is disabled or API key not configured");
            return new HashMap<>();
        }

        try {
            // Build the prompt
            String prompt = buildInferencePrompt(columnSamples);
            
            // Make API call
            String response = callOpenAI(prompt);
            
            // Parse response
            return parseInferenceResponse(response, columnSamples);
            
        } catch (Exception e) {
            LOGGER.error("Error calling OpenAI API: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    private String buildInferencePrompt(Map<String, List<String>> columnSamples) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the following column data samples and infer the column type and cleaning rules.\n");
        prompt.append("For each column, determine the type (EMAIL, NUMERIC, DATE, NAME, PHONE, CATEGORICAL, or UNKNOWN) ");
        prompt.append("and provide cleaning rules.\n\n");
        prompt.append("Data samples:\n");
        
        for (Map.Entry<String, List<String>> entry : columnSamples.entrySet()) {
            prompt.append(entry.getKey()).append(": ");
            prompt.append(String.join(", ", entry.getValue().stream()
                .map(v -> "\"" + v + "\"")
                .toList()));
            prompt.append("\n");
        }
        
        prompt.append("\nRespond with a JSON object in this exact format:\n");
        prompt.append("{\n");
        prompt.append("  \"Column1\": {\"type\": \"EMAIL\", \"cleaningRules\": [\"lowercase\", \"validate_format\"]},\n");
        prompt.append("  \"Column2\": {\"type\": \"NUMERIC\", \"cleaningRules\": [\"remove_symbols\", \"parse_number\"]}\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }

    private String callOpenAI(String prompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
            Map.of("role", "system", "content", "You are a data cleaning expert. Analyze data samples and provide JSON responses only."),
            Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 2000);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                OPENAI_API_URL,
                HttpMethod.POST,
                entity,
                Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error calling OpenAI API", e);
            throw new RuntimeException("Failed to call OpenAI API: " + e.getMessage(), e);
        }

        return null;
    }

    private Map<String, ColumnInference> parseInferenceResponse(String response, Map<String, List<String>> columnSamples) {
        Map<String, ColumnInference> inferences = new HashMap<>();
        
        if (response == null || response.isEmpty()) {
            return inferences;
        }

        try {
            // Extract JSON from response (might be wrapped in markdown code blocks)
            String jsonStr = response.trim();
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.substring(7);
            }
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.substring(3);
            }
            if (jsonStr.endsWith("```")) {
                jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
            }
            jsonStr = jsonStr.trim();

            JsonNode rootNode = objectMapper.readTree(jsonStr);
            
            for (String columnName : columnSamples.keySet()) {
                JsonNode columnNode = rootNode.get(columnName);
                if (columnNode != null) {
                    String type = columnNode.has("type") ? columnNode.get("type").asText() : "UNKNOWN";
                    List<String> cleaningRules = new java.util.ArrayList<>();
                    if (columnNode.has("cleaningRules") && columnNode.get("cleaningRules").isArray()) {
                        for (JsonNode rule : columnNode.get("cleaningRules")) {
                            cleaningRules.add(rule.asText());
                        }
                    }
                    
                    inferences.put(columnName, new ColumnInference(type, cleaningRules));
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error parsing OpenAI response: {}", e.getMessage());
        }

        return inferences;
    }

    public static class ColumnInference {
        private final String type;
        private final List<String> cleaningRules;

        public ColumnInference(String type, List<String> cleaningRules) {
            this.type = type;
            this.cleaningRules = cleaningRules;
        }

        public String getType() { return type; }
        public List<String> getCleaningRules() { return cleaningRules; }
    }
}

