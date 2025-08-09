package org.rulex.dto;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class ValidationRequest {

    private String fileName;
    private List<ColumnRule> columnRules;
    private MultipartFile file;

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<ColumnRule> getColumnRules() {
        return columnRules;
    }

    public void setColumnRules(List<ColumnRule> columnRules) {
        this.columnRules = columnRules;
    }
}
