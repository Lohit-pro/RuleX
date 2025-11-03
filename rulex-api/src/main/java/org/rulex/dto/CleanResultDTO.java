package org.rulex.dto;

import java.util.List;
import java.util.Map;

public class CleanResultDTO {
    private int totalRows;
    private int cleanedRows;
    private int totalCorrections;
    private List<ColumnInfo> columns;
    private List<CorrectionInfo> sampleCorrections;
    private Map<String, Object> aiInference; // Store AI inference for refinement
    private Map<String, List<String>> columnSamples; // Store original column samples

    public static class ColumnInfo {
        private int index;
        private String type;
        private int corrections;

        public ColumnInfo() {}

        public ColumnInfo(int index, String type, int corrections) {
            this.index = index;
            this.type = type;
            this.corrections = corrections;
        }

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public int getCorrections() { return corrections; }
        public void setCorrections(int corrections) { this.corrections = corrections; }
    }

    public static class CorrectionInfo {
        private int row;
        private int col;
        private String original;
        private String cleaned;

        public CorrectionInfo() {}

        public CorrectionInfo(int row, int col, String original, String cleaned) {
            this.row = row;
            this.col = col;
            this.original = original;
            this.cleaned = cleaned;
        }

        public int getRow() { return row; }
        public void setRow(int row) { this.row = row; }
        public int getCol() { return col; }
        public void setCol(int col) { this.col = col; }
        public String getOriginal() { return original; }
        public void setOriginal(String original) { this.original = original; }
        public String getCleaned() { return cleaned; }
        public void setCleaned(String cleaned) { this.cleaned = cleaned; }
    }

    public CleanResultDTO() {}

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    public int getCleanedRows() { return cleanedRows; }
    public void setCleanedRows(int cleanedRows) { this.cleanedRows = cleanedRows; }
    public int getTotalCorrections() { return totalCorrections; }
    public void setTotalCorrections(int totalCorrections) { this.totalCorrections = totalCorrections; }
    public List<ColumnInfo> getColumns() { return columns; }
    public void setColumns(List<ColumnInfo> columns) { this.columns = columns; }
    public List<CorrectionInfo> getSampleCorrections() { return sampleCorrections; }
    public void setSampleCorrections(List<CorrectionInfo> sampleCorrections) { this.sampleCorrections = sampleCorrections; }
    public Map<String, Object> getAiInference() { return aiInference; }
    public void setAiInference(Map<String, Object> aiInference) { this.aiInference = aiInference; }
    public Map<String, List<String>> getColumnSamples() { return columnSamples; }
    public void setColumnSamples(Map<String, List<String>> columnSamples) { this.columnSamples = columnSamples; }
}

