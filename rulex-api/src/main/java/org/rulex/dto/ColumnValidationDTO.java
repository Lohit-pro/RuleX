package org.rulex.dto;

import java.util.List;

public class ColumnValidationDTO {
    private String fileName;
    private List<ColumnRule> rules;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<ColumnRule> getRules() {
        return rules;
    }

    public void setRules(List<ColumnRule> rules) {
        this.rules = rules;
    }

    public static class ColumnRule {
        private String column;
        private Rule rule;

        public String getColumn() {
            return column;
        }

        public void setColumn(String column) {
            this.column = column;
        }

        public Rule getRule() {
            return rule;
        }

        public void setRule(Rule rule) {
            this.rule = rule;
        }
    }
}
