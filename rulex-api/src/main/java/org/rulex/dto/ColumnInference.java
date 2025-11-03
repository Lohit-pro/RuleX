package org.rulex.dto;

import java.util.List;
 public class ColumnInference {
    private final String type;
    private final List<String> cleaningRules;

    public ColumnInference(String type, List<String> cleaningRules) {
        this.type = type;
        this.cleaningRules = cleaningRules;
    }

    public String getType() { return type; }
    public List<String> getCleaningRules() { return cleaningRules; }
}

