package org.rulex.dto;

import java.util.Map;

public class RefinementRequestDTO {
    private String fileName;
    private Map<String, Object> previousInference; // Previous AI inference result
    private String feedback; // User's custom feedback (null if auto-regenerate)
    private boolean autoRegenerate; // true for auto-regenerate, false for custom feedback
    private Map<String, java.util.List<String>> columnSamples; // Original column samples

    public RefinementRequestDTO() {}

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Map<String, Object> getPreviousInference() {
        return previousInference;
    }

    public void setPreviousInference(Map<String, Object> previousInference) {
        this.previousInference = previousInference;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public boolean isAutoRegenerate() {
        return autoRegenerate;
    }

    public void setAutoRegenerate(boolean autoRegenerate) {
        this.autoRegenerate = autoRegenerate;
    }

    public Map<String, java.util.List<String>> getColumnSamples() {
        return columnSamples;
    }

    public void setColumnSamples(Map<String, java.util.List<String>> columnSamples) {
        this.columnSamples = columnSamples;
    }
}

