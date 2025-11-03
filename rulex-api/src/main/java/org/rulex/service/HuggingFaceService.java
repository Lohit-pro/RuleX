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

import java.util.*;

/**
 * HuggingFaceService: mirrors OpenAIService structure but calls Hugging Face router endpoint:
 * POST https://router.huggingface.co/v1/chat/completions
 *
 * Body: {
 *   "model": "<model>",
 *   "messages": [ { "role":"system","content":"..." }, { "role":"user","content":"..." } ],
 *   "temperature": 0.3,
 *   "max_tokens": 500
 * }
 */
@Service
public class HuggingFaceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HuggingFaceService.class);
    private static final String HF_API_URL = "https://router.huggingface.co/v1/chat/completions";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${huggingface.api.key:}")
    private String apiKey;

    @Value("${huggingface.api.enabled:false}")
    private boolean enabled;

    @Value("${huggingface.model:meta-llama/Meta-Llama-3-8B-Instruct}")
    private String model;

    // Simple cache to reduce repeated calls
    private static final Map<String, Map<String, ColumnInference>> cache = new HashMap<>();
    private static final int MAX_CACHE_SIZE = 50;

    public Map<String, ColumnInference> inferColumnTypes(Map<String, List<String>> columnSamples) {
        if (!enabled || apiKey == null || apiKey.isEmpty()) {
            LOGGER.warn("Hugging Face disabled or API key missing.");
            return new HashMap<>();
        }

        String cacheKey = createCacheKey(columnSamples);
        if (cache.containsKey(cacheKey)) {
            return new HashMap<>(cache.get(cacheKey));
        }

        try {
            String prompt = buildInferencePrompt(columnSamples);

            String response = callHuggingFace(prompt);
            if (response == null) {
                return new HashMap<>();
            }

            Map<String, ColumnInference> result = parseInferenceResponse(response, columnSamples);
            result = autoCorrectInference(result, columnSamples);

            if (cache.size() >= MAX_CACHE_SIZE) {
                cache.clear(); // simple eviction
            }
            cache.put(cacheKey, result);

            return result;

        } catch (RuntimeException e) {
            if (e.getMessage() != null && (e.getMessage().contains("429") || e.getMessage().toLowerCase().contains("quota"))) {
                LOGGER.warn("Hugging Face rate limit or quota hit: {}", e.getMessage());
                return new HashMap<>();
            }
            LOGGER.error("Error during inferColumnTypes: {}", e.getMessage(), e);
            return new HashMap<>();
        } catch (Exception e) {
            LOGGER.error("Unexpected error during inferColumnTypes: {}", e.getMessage(), e);
            return new HashMap<>();
        }
    }

    public Map<String, ColumnInference> refineInference(
            Map<String, List<String>> columnSamples,
            Map<String, ColumnInference> previousInference,
            String feedback,
            boolean autoRegenerate) {

        if (!enabled || apiKey == null || apiKey.isEmpty()) {
            LOGGER.warn("Hugging Face disabled or API key missing; returning previous inference.");
            return previousInference;
        }

        try {
            String prompt = buildRefinementPrompt(columnSamples, previousInference, feedback, autoRegenerate);

            String response = callHuggingFace(prompt);
            if (response == null) {
                return previousInference;
            }

            Map<String, ColumnInference> refined = parseInferenceResponse(response, columnSamples);
            refined = autoCorrectInference(refined, columnSamples);

            // Merge: keep previous inference for any columns missing in refined response
            for (String col : columnSamples.keySet()) {
                if (!refined.containsKey(col) && previousInference.containsKey(col)) {
                    refined.put(col, previousInference.get(col));
                }
            }

            return refined;

        } catch (RuntimeException e) {
            LOGGER.warn("Runtime exception during refineInference: {}", e.getMessage());
            return previousInference;
        } catch (Exception e) {
            LOGGER.error("Unexpected error during refineInference: {}", e.getMessage(), e);
            return previousInference;
        }
    }

    private String buildInferencePrompt(Map<String, List<String>> columnSamples) {
        StringBuilder p = new StringBuilder();
        p.append("You are a data cleaning expert.\\n");
        p.append("Task: Infer the column type and cleaning rules for each column from the given samples.\\n");
        p.append("Allowed types (UPPERCASE only): EMAIL, NUMERIC, DATE, NAME, PHONE, CATEGORICAL, UNKNOWN.\\n");
        p.append("- EMAIL: must contain '@' and a domain.\\n");
        p.append("- NUMERIC: digits with optional decimal point and minus sign.\\n");
        p.append("- DATE: common formats like yyyy-MM-dd, dd/MM/yyyy, MM/dd/yyyy.\\n");
        p.append("- NAME: alphabetic words, spaces, hyphens, apostrophes.\\n");
        p.append("- PHONE: digits (ignore punctuation) with length >= 7.\\n");
        p.append("- CATEGORICAL: small set of repeated labels.\\n\\n");
        p.append("Return STRICT JSON ONLY (no markdown) using EXACTLY the column keys shown below.\\n");
        p.append("JSON schema per column: { \\\"type\\\": <TYPE>, \\\"cleaningRules\\\": [<RULES>] }\\n");
        p.append("Examples of cleaningRules: ['lowercase','validate_format'], ['remove_symbols','parse_number'], ['normalize_date_yyyy_mm_dd']\\n\\n");
        p.append("Samples:\\n");
        for (Map.Entry<String, List<String>> e : columnSamples.entrySet()) {
            p.append(e.getKey()).append(": ");
            List<String> limited = e.getValue().stream().limit(3).map(s -> "\\\"" + s + "\\\"").toList();
            p.append(String.join(", ", limited)).append("\\n");
        }
        p.append("\\nRespond with JSON ONLY. Example shape:\\n");
        p.append("{\\n");
        p.append("  \\\"Column1\\\": {\\\"type\\\": \\\"EMAIL\\\", \\\"cleaningRules\\\": [\\\"lowercase\\\", \\\"validate_format\\\"]},\\n");
        p.append("  \\\"Column2\\\": {\\\"type\\\": \\\"NUMERIC\\\", \\\"cleaningRules\\\": [\\\"remove_symbols\\\", \\\"parse_number\\\"]}\\n");
        p.append("}\\n");
        return p.toString();
    }

    private String buildRefinementPrompt(
            Map<String, List<String>> columnSamples,
            Map<String, ColumnInference> previousInference,
            String feedback,
            boolean autoRegenerate) {

        StringBuilder prompt = new StringBuilder();
        prompt.append("You previously analyzed these column samples and produced this inference:\n\n");

        prompt.append("Previous Inference:\n");
        for (Map.Entry<String, ColumnInference> e : previousInference.entrySet()) {
            prompt.append(e.getKey()).append(": { \"type\": \"").append(e.getValue().getType()).append("\", \"cleaningRules\": [");
            prompt.append(String.join(", ", e.getValue().getCleaningRules().stream().map(r -> "\"" + r + "\"").toList()));
            prompt.append("] }\n");
        }

        prompt.append("\nOriginal Samples:\n");
        for (Map.Entry<String, List<String>> e : columnSamples.entrySet()) {
            prompt.append(e.getKey()).append(": ");
            prompt.append(String.join(", ", e.getValue().stream().map(s -> "\"" + s + "\"").toList()));
            prompt.append("\n");
        }

        if (autoRegenerate) {
            prompt.append("\nPlease refine and improve the previous inference. Provide an improved JSON with the same structure.\n");
        } else if (feedback != null && !feedback.trim().isEmpty()) {
            prompt.append("\nUser Feedback: ").append(feedback).append("\n");
            prompt.append("Based on this feedback, refine the previous inference and return JSON only.\n");
        }

        prompt.append("\nReturn JSON only, example:\n");
        prompt.append("{\"Column1\": {\"type\": \"EMAIL\", \"cleaningRules\": [\"lowercase\",\"validate_format\"]}, ...}\n");
        return prompt.toString();
    }

    private String callHuggingFace(String prompt) {
        if (!enabled || apiKey == null || apiKey.isEmpty()) {
            LOGGER.warn("Hugging Face disabled or API key missing; call aborted.");
            return null;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> systemMessage = Map.of("role", "system", "content", "You are a data cleaning expert. Analyze data samples and provide JSON responses only.");
            Map<String, Object> userMessage = Map.of("role", "user", "content", prompt);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", List.of(systemMessage, userMessage));
            body.put("temperature", 0.3);
            body.put("max_tokens", 500);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            LOGGER.info("Calling Hugging Face router endpoint for model: {}", model);
            ResponseEntity<String> response = restTemplate.exchange(HF_API_URL, HttpMethod.POST, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                LOGGER.warn("Hugging Face returned status {} with body: {}", response.getStatusCode(), response.getBody());
                return null;
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            int status = e.getStatusCode().value();
            String body = e.getResponseBodyAsString();
            LOGGER.error("Hugging Face HTTP error {}: {}", status, body);

            // Handle common status codes gracefully
            if (status == 401) {
                throw new RuntimeException("Invalid Hugging Face API key (401).");
            } else if (status == 404) {
                throw new RuntimeException("Hugging Face endpoint not found (404). Raw: " + body);
            } else if (status == 429) {
                throw new RuntimeException("Rate limited by Hugging Face (429).");
            } else {
                throw new RuntimeException("Hugging Face API error: " + status + " - " + body);
            }
        } catch (Exception e) {
            LOGGER.error("Error calling Hugging Face: {}", e.getMessage(), e);
            return null;
        }
    }

    private Map<String, ColumnInference> parseInferenceResponse(String response, Map<String, List<String>> columnSamples) {
        Map<String, ColumnInference> inferences = new HashMap<>();
        if (response == null || response.isEmpty()) return inferences;

        try {
            // If HF returns an OpenAI-like response (choices[].message.content), extract it.
            JsonNode root = objectMapper.readTree(response);

            // Attempt: response.choices[0].message.content
            if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
                JsonNode firstChoice = root.get("choices").get(0);
                JsonNode message = firstChoice.has("message") ? firstChoice.get("message") : null;
                String content = null;
                if (message != null && message.has("content")) {
                    content = message.get("content").asText();
                } else if (firstChoice.has("text")) {
                    // fallback for older style
                    content = firstChoice.get("text").asText();
                }

                if (content != null) {
                    return parseJsonContentToInferences(content, columnSamples);
                }
            }

            // Fallback: maybe HF returned raw text JSON (not wrapped)
            if (root.isObject()) {
                // If the root directly contains Column keys, parse it.
                boolean containsColumnKeys = false;
                Iterator<String> fieldNames = root.fieldNames();
                while (fieldNames.hasNext()) {
                    String f = fieldNames.next();
                    if (columnSamples.containsKey(f)) {
                        containsColumnKeys = true;
                        break;
                    }
                }
                if (containsColumnKeys) {
                    // Convert root to string and parse similarly
                    return parseJsonContentToInferences(root.toString(), columnSamples);
                }
            }

        } catch (Exception e) {
            LOGGER.error("Error parsing Hugging Face response: {}. Raw response: {}", e.getMessage(), response);
        }

        return inferences;
    }

    // Sanity filter and fallback: validate types and fill missing using heuristics over samples
    private Map<String, ColumnInference> autoCorrectInference(Map<String, ColumnInference> ai, Map<String, List<String>> samples) {
        Map<String, ColumnInference> corrected = new HashMap<>();
        for (Map.Entry<String, List<String>> e : samples.entrySet()) {
            String col = e.getKey();
            List<String> vals = e.getValue();
            ColumnInference current = ai.get(col);
            String type = (current != null && current.getType() != null) ? current.getType().toUpperCase() : "";
            if (!isValidType(type)) {
                type = inferTypeFromSamples(vals);
            }
            List<String> rules = (current != null && current.getCleaningRules() != null) ? current.getCleaningRules() : defaultRulesFor(type);
            corrected.put(col, new ColumnInference(type, rules));
        }
        return corrected;
    }

    private boolean isValidType(String t) {
        return "EMAIL".equals(t) || "NUMERIC".equals(t) || "DATE".equals(t) || "NAME".equals(t) || "PHONE".equals(t) || "CATEGORICAL".equals(t) || "UNKNOWN".equals(t);
    }

    private String inferTypeFromSamples(List<String> vals) {
        int email=0, date=0, numeric=0, phone=0, name=0; Set<String> uniques = new HashSet<>();
        for (String v : vals) {
            if (v == null) continue;
            String s = v.trim();
            if (s.isEmpty()) continue;
            uniques.add(s);
            if (looksLikeEmail(s)) email++;
            else if (looksLikeDate(s)) date++;
            else if (looksLikeNumeric(s)) numeric++;
            else if (looksLikePhone(s)) phone++;
            else if (looksLikeName(s)) name++;
        }
        int total = Math.max(1, vals.size());
        if (email > 0 && email >= date && email >= numeric && email >= phone && email >= name) return "EMAIL";
        if (date > 0 && date >= numeric && date >= phone && date >= name) return "DATE";
        if (numeric > 0 && numeric >= phone && numeric >= name) return "NUMERIC";
        if (phone > 0 && phone >= name) return "PHONE";
        if (!uniques.isEmpty() && uniques.size() <= Math.max(10, (int)Math.ceil(total * 0.5))) return "CATEGORICAL";
        if (name > 0) return "NAME";
        return "UNKNOWN";
    }

    private boolean looksLikeEmail(String s) {
        return s.contains("@") && s.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    private boolean looksLikeNumeric(String s) {
        return s.matches("^-?\\d+(?:[.,]\\d+)?$");
    }
    private boolean looksLikePhone(String s) {
        String digits = s.replaceAll("[^0-9]", "");
        return digits.length() >= 7;
    }
    private boolean looksLikeName(String s) {
        return s.matches("^[A-Za-z\\\\s'\\\\-]+$");
    }
    private boolean looksLikeDate(String s) {
        String[] fmts = new String[]{"yyyy-MM-dd","dd/MM/yyyy","MM/dd/yyyy","dd-MM-yyyy","MM-dd-yyyy","yyyy/MM/dd"};
        for (String f : fmts) {
            try {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(f);
                sdf.setLenient(false);
                sdf.parse(s);
                return true;
            } catch (Exception ignore) {}
        }
        return s.matches(".*\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}.*") || s.matches(".*\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}.*");
    }
    private List<String> defaultRulesFor(String type) {
        switch (type) {
            case "EMAIL": return Arrays.asList("lowercase","validate_format");
            case "NUMERIC": return Arrays.asList("remove_symbols","parse_number");
            case "DATE": return Arrays.asList("normalize_date_yyyy_mm_dd");
            case "NAME": return Arrays.asList("strip_symbols","title_case");
            case "PHONE": return Arrays.asList("digits_only","keep_last_12");
            case "CATEGORICAL": return Arrays.asList("title_case","map_common_roles");
            default: return Collections.emptyList();
        }
    }

    private Map<String, ColumnInference> parseJsonContentToInferences(String jsonContent, Map<String, List<String>> columnSamples) {
        Map<String, ColumnInference> inferences = new HashMap<>();
        if (jsonContent == null || jsonContent.isEmpty()) return inferences;

        try {
            // strip markdown code fences if present
            String s = jsonContent.trim();
            if (s.startsWith("```json")) s = s.substring(7);
            if (s.startsWith("```")) s = s.substring(3);
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
            s = s.trim();

            JsonNode parsed = objectMapper.readTree(s);

            for (String columnName : columnSamples.keySet()) {
                JsonNode colNode = parsed.get(columnName);
                if (colNode != null && colNode.isObject()) {
                    String type = colNode.has("type") ? colNode.get("type").asText() : "UNKNOWN";
                    List<String> rules = new ArrayList<>();
                    if (colNode.has("cleaningRules") && colNode.get("cleaningRules").isArray()) {
                        for (JsonNode r : colNode.get("cleaningRules")) {
                            rules.add(r.asText());
                        }
                    }
                    inferences.put(columnName, new ColumnInference(type, rules));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse JSON content returned by model: {}", e.getMessage());
        }
        return inferences;
    }

    private String createCacheKey(Map<String, List<String>> columnSamples) {
        StringBuilder key = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : columnSamples.entrySet()) {
            key.append(entry.getKey()).append(":");
            List<String> samples = entry.getValue();
            if (!samples.isEmpty()) {
                key.append(samples.get(0).length()).append("-").append(samples.get(samples.size() - 1).length());
            }
            key.append("|");
        }
        return key.toString();
    }


}
