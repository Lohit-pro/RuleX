package org.rulex.dto;

import java.util.List;

public class RequestDTO {
    private String fileName;
    private List<RulesDTO> rules;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<RulesDTO> getRules() {
        return rules;
    }

    public void setRules(List<RulesDTO> rules) {
        this.rules = rules;
    }
}
