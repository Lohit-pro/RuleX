package org.rulex.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.rulex.dto.ColumnInference;
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

    // Simple cache to avoid duplicate API calls for similar patterns
    private static final Map<String, Map<String, ColumnInference>> cache = new HashMap<>();
    private static final int MAX_CACHE_SIZE = 50;

    public Map<String, ColumnInference> inferColumnTypes(Map<String, List<String>> columnSamples) {
        if (!enabled || apiKey == null || apiKey.isEmpty()) {
            return new HashMap<>();
        }

        // Create cache key from column samples
        String cacheKey = createCacheKey(columnSamples);
        
        // Check cache first
        if (cache.containsKey(cacheKey)) {
            return new HashMap<>(cache.get(cacheKey));
        }

        try {
            // Build the prompt (reduced sample size)
            String prompt = buildInferencePrompt(columnSamples);
            
            // Make API call
            String response = callOpenAI(prompt);
            
            if (response == null) {
                return new HashMap<>();
            }
            
            // Parse response
            Map<String, ColumnInference> result = parseInferenceResponse(response, columnSamples);
            
            // Cache the result
            if (cache.size() >= MAX_CACHE_SIZE) {
                cache.clear(); // Simple eviction - clear when full
            }
            cache.put(cacheKey, result);
            
            return result;
            
        } catch (RuntimeException e) {
            if (e.getMessage() != null && (e.getMessage().contains("429") || e.getMessage().contains("quota"))) {
                return new HashMap<>();
            }
            return new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private String createCacheKey(Map<String, List<String>> columnSamples) {
        // Create a simple hash-like key from column sample patterns
        StringBuilder key = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : columnSamples.entrySet()) {
            key.append(entry.getKey()).append(":");
            List<String> samples = entry.getValue();
            if (!samples.isEmpty()) {
                // Use first and last sample for pattern matching
                key.append(samples.get(0).length()).append("-")
                   .append(samples.get(samples.size() - 1).length());
            }
            key.append("|");
        }
        return key.toString();
    }

    public Map<String, ColumnInference> refineInference(
            Map<String, List<String>> columnSamples,
            Map<String, ColumnInference> previousInference,
            String feedback,
            boolean autoRegenerate) {
        
        if (!enabled || apiKey == null || apiKey.isEmpty()) {
            LOGGER.warn("OpenAI API is disabled or API key not configured");
            return previousInference; // Return previous if AI disabled
        }

        try {
            // Build refinement prompt
            String prompt = buildRefinementPrompt(columnSamples, previousInference, feedback, autoRegenerate);
            
            // Make API call
            String response = callOpenAI(prompt);
            
            if (response == null) {
                return previousInference;
            }
            
            Map<String, ColumnInference> refinedInference = parseInferenceResponse(response, columnSamples);
            
            // Merge with previous inference if needed
            for (String columnName : columnSamples.keySet()) {
                if (!refinedInference.containsKey(columnName) && previousInference.containsKey(columnName)) {
                    refinedInference.put(columnName, previousInference.get(columnName));
                }
            }
            
            return refinedInference;
            
        } catch (RuntimeException e) {
            if (e.getMessage() != null && (e.getMessage().contains("429") || e.getMessage().contains("quota"))) {
                return previousInference;
            }
            return previousInference;
        } catch (Exception e) {
            return previousInference;
        }
    }

    private String buildInferencePrompt(Map<String, List<String>> columnSamples) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Infer column types from these samples. Types: EMAIL, NUMERIC, DATE, NAME, PHONE, CATEGORICAL, UNKNOWN.\n\n");
        
        for (Map.Entry<String, List<String>> entry : columnSamples.entrySet()) {
            prompt.append(entry.getKey()).append(": ");
            // Use fewer samples to reduce token usage (max 3 instead of all)
            List<String> limitedSamples = entry.getValue().stream()
                .limit(3)
                .map(v -> "\"" + v + "\"")
                .toList();
            prompt.append(String.join(", ", limitedSamples));
            prompt.append("\n");
        }
        
        prompt.append("\nJSON format: {\"Column1\": {\"type\": \"EMAIL\", \"cleaningRules\": []}, ...}\n");
        
        return prompt.toString();
    }

    private String buildRefinementPrompt(
            Map<String, List<String>> columnSamples,
            Map<String, ColumnInference> previousInference,
            String feedback,
            boolean autoRegenerate) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("You previously analyzed these column data samples and provided this inference:\n\n");
        
        // Show previous inference
        prompt.append("Previous Inference:\n");
        for (Map.Entry<String, ColumnInference> entry : previousInference.entrySet()) {
            prompt.append(entry.getKey()).append(": {\n");
            prompt.append("  \"type\": \"").append(entry.getValue().getType()).append("\",\n");
            prompt.append("  \"cleaningRules\": [");
            prompt.append(String.join(", ", entry.getValue().getCleaningRules().stream()
                .map(r -> "\"" + r + "\"")
                .toList()));
            prompt.append("]\n");
            prompt.append("},\n");
        }
        
        prompt.append("\nOriginal Data Samples:\n");
        for (Map.Entry<String, List<String>> entry : columnSamples.entrySet()) {
            prompt.append(entry.getKey()).append(": ");
            prompt.append(String.join(", ", entry.getValue().stream()
                .map(v -> "\"" + v + "\"")
                .toList()));
            prompt.append("\n");
        }
        
        if (autoRegenerate) {
            prompt.append("\nPlease refine and improve the previous inference. ");
            prompt.append("Look for areas where the column type detection or cleaning rules could be more accurate. ");
            prompt.append("Provide an improved version while maintaining the same JSON structure.\n");
        } else if (feedback != null && !feedback.trim().isEmpty()) {
            prompt.append("\nUser Feedback: ").append(feedback).append("\n\n");
            prompt.append("Based on this feedback, please refine the previous inference. ");
            prompt.append("Address the user's concerns while maintaining the same JSON structure.\n");
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
        requestBody.put("max_tokens", 500); // Reduced from 2000 to minimize cost

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
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) {
                throw new RuntimeException("Model '" + model + "' not found.", e);
            } else if (e.getStatusCode().value() == 401) {
                throw new RuntimeException("Invalid API key.", e);
            } else if (e.getStatusCode().value() == 429) {
                return null; // Quota exceeded - silent fallback
            }
            throw new RuntimeException("API error: " + e.getStatusCode(), e);
        } catch (Exception e) {
            throw new RuntimeException("API call failed: " + e.getMessage(), e);
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

//    public static class ColumnInference {
//        private final String type;
//        private final List<String> cleaningRules;
//
//        public ColumnInference(String type, List<String> cleaningRules) {
//            this.type = type;
//            this.cleaningRules = cleaningRules;
//        }
//
//        public String getType() { return type; }
//        public List<String> getCleaningRules() { return cleaningRules; }
//    }
}

