package org.rulex.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ExcelAutoCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelAutoCleaner.class);

    @Autowired(required = false)
    private OpenAIService openAIService;

    @Value("${openai.api.enabled:false}")
    private boolean aiEnabled;

    // Patterns for type detection
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile(".*[0-9].*[0-9].*[0-9].*[0-9].*[0-9].*");
    private static final Pattern NUMERIC_PATTERN = Pattern.compile("^-?\\d+(\\.\\d+)?$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z\\s'-]+$");
    private static final Pattern DATE_PATTERN = Pattern.compile(".*\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}.*|.*\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}.*");

    private static final String[] DATE_FORMATS = {
        "yyyy-MM-dd", "yyyy/MM/dd", "dd/MM/yyyy", "dd-MM-yyyy",
        "MM/dd/yyyy", "MM-dd-yyyy", "dd/MM/yy", "dd-MM-yy",
        "MM/dd/yy", "MM-dd-yy", "yyyyMMdd"
    };

    public CleanResult cleanExcel(String inputFilePath, String outputFilePath) {
        CleanResult result = new CleanResult();
        
        try (FileInputStream fis = new FileInputStream(inputFilePath);
             Workbook workbook = new XSSFWorkbook(fis)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet.getLastRowNum() < 1) {
                LOGGER.warn("Excel file is empty or has no data rows");
                return result;
            }

            Row headerRow = sheet.getRow(0);
            int numColumns = headerRow.getLastCellNum();
            
            // Step 1: Infer column types
            List<ColumnMetadata> columnMetadata = inferColumnTypes(sheet, numColumns);
            result.setColumnMetadata(columnMetadata);

            // Step 2: Optionally use AI for inference
            if (aiEnabled && openAIService != null) {
                try {
                    enhanceWithAI(sheet, columnMetadata);
                } catch (Exception e) {
                    LOGGER.warn("AI inference failed, falling back to rule-based: {}", e.getMessage());
                }
            }

            // Step 3: Clean data
            DataFormatter formatter = new DataFormatter();
            int cleanedRows = 0;
            
            for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;
                
                boolean rowModified = false;
                for (int colIdx = 0; colIdx < numColumns && colIdx < columnMetadata.size(); colIdx++) {
                    Cell cell = row.getCell(colIdx);
                    if (cell == null) continue;
                    
                    String originalValue = formatter.formatCellValue(cell).trim();
                    if (originalValue.isEmpty()) continue;
                    
                    ColumnMetadata metadata = columnMetadata.get(colIdx);
                    String cleanedValue = cleanValue(originalValue, metadata.getType());
                    
                    if (!cleanedValue.equals(originalValue)) {
                        if (cleanedValue.isEmpty()) {
                            // Set blank for invalid values
                            cell.setBlank();
                        } else {
                            // Set the cleaned value
                            if (cell.getCellType() == CellType.STRING) {
                                cell.setCellValue(cleanedValue);
                            } else {
                                cell.setCellType(CellType.STRING);
                                cell.setCellValue(cleanedValue);
                            }
                        }
                        rowModified = true;
                        metadata.incrementCorrections();
                        result.addCorrection(rowIdx, colIdx, originalValue, cleanedValue);
                    }
                }
                
                if (rowModified) {
                    cleanedRows++;
                }
            }

            result.setCleanedRows(cleanedRows);
            result.setTotalRows(sheet.getLastRowNum());

            // Step 4: Write cleaned workbook
            try (FileOutputStream fos = new FileOutputStream(outputFilePath)) {
                workbook.write(fos);
            }

            LOGGER.info("Excel cleaned successfully. Output: {}", outputFilePath);
            
        } catch (IOException e) {
            LOGGER.error("Error processing Excel file", e);
            throw new RuntimeException("Failed to clean Excel file: " + e.getMessage(), e);
        }

        return result;
    }

    private List<ColumnMetadata> inferColumnTypes(Sheet sheet, int numColumns) {
        List<ColumnMetadata> metadata = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        
        // Sample up to 100 rows for type inference
        int sampleSize = Math.min(100, sheet.getLastRowNum());
        
        for (int colIdx = 0; colIdx < numColumns; colIdx++) {
            Map<String, Integer> typeCounts = new HashMap<>();
            List<String> samples = new ArrayList<>();
            
            for (int rowIdx = 1; rowIdx <= sampleSize; rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;
                
                Cell cell = row.getCell(colIdx);
                if (cell == null) continue;
                
                String value = formatter.formatCellValue(cell).trim();
                if (value.isEmpty()) continue;
                
                if (samples.size() < 20) {
                    samples.add(value);
                }
                
                ColumnType type = detectType(value);
                typeCounts.put(type.name(), typeCounts.getOrDefault(type.name(), 0) + 1);
            }
            
            // Determine most common type
            ColumnType inferredType = ColumnType.UNKNOWN;
            int maxCount = 0;
            for (Map.Entry<String, Integer> entry : typeCounts.entrySet()) {
                if (entry.getValue() > maxCount) {
                    maxCount = entry.getValue();
                    inferredType = ColumnType.valueOf(entry.getKey());
                }
            }
            
            // If UNKNOWN or low confidence, check if categorical
            if (inferredType == ColumnType.UNKNOWN || maxCount < sampleSize * 0.3) {
                if (isLikelyCategorical(samples)) {
                    inferredType = ColumnType.CATEGORICAL;
                }
            }
            
            metadata.add(new ColumnMetadata(colIdx, inferredType, samples));
        }
        
        return metadata;
    }

    private ColumnType detectType(String value) {
        if (EMAIL_PATTERN.matcher(value).matches()) {
            return ColumnType.EMAIL;
        }
        if (isDate(value)) {
            return ColumnType.DATE;
        }
        if (NUMERIC_PATTERN.matcher(value).matches()) {
            return ColumnType.NUMERIC;
        }
        if (PHONE_PATTERN.matcher(value).matches() && value.replaceAll("[^0-9]", "").length() >= 7) {
            return ColumnType.PHONE;
        }
        if (NAME_PATTERN.matcher(value).matches() && value.length() > 1) {
            return ColumnType.NAME;
        }
        return ColumnType.UNKNOWN;
    }

    private boolean isDate(String value) {
        if (value == null || value.isEmpty()) return false;
        
        // Try parsing with common formats
        for (String format : DATE_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setLenient(false);
                sdf.parse(value);
                return true;
            } catch (ParseException ignored) {}
        }
        
        // Check pattern
        return DATE_PATTERN.matcher(value).matches();
    }

    private boolean isLikelyCategorical(List<String> samples) {
        if (samples.size() < 3) return false;
        
        Set<String> uniqueValues = new HashSet<>(samples);
        // If we have few unique values relative to sample size, likely categorical
        return uniqueValues.size() <= Math.max(10, samples.size() * 0.5);
    }

    private String cleanValue(String value, ColumnType type) {
        if (value == null || value.isEmpty()) return value;
        
        switch (type) {
            case EMAIL:
                return cleanEmail(value);
            case NUMERIC:
                return cleanNumeric(value);
            case DATE:
                return cleanDate(value);
            case NAME:
                return cleanName(value);
            case PHONE:
                return cleanPhone(value);
            case CATEGORICAL:
                return cleanCategorical(value);
            default:
                return value; // UNKNOWN - leave unchanged
        }
    }

    private String cleanEmail(String value) {
        String cleaned = value.toLowerCase().trim();
        // Remove invalid characters before @
        cleaned = cleaned.replaceAll("[^a-zA-Z0-9+_.-]@", "@");
        
        if (EMAIL_PATTERN.matcher(cleaned).matches()) {
            return cleaned;
        }
        return ""; // Invalid email - return blank
    }

    private String cleanNumeric(String value) {
        // Remove currency symbols, commas, spaces
        String cleaned = value.replaceAll("[^0-9.-]", "");
        
        // Handle multiple dots or dashes
        if (cleaned.chars().filter(c -> c == '.').count() > 1) {
            // Keep only the last dot (for decimals)
            int lastDot = cleaned.lastIndexOf('.');
            cleaned = cleaned.substring(0, lastDot).replace(".", "") + cleaned.substring(lastDot);
        }
        
        try {
            Double.parseDouble(cleaned);
            return cleaned;
        } catch (NumberFormatException e) {
            return ""; // Invalid number - return blank
        }
    }

    private String cleanDate(String value) {
        String cleaned = value.trim();
        
        // Try to parse and normalize to yyyy-MM-dd
        for (String format : DATE_FORMATS) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                sdf.setLenient(false);
                Date date = sdf.parse(cleaned);
                
                SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
                return outputFormat.format(date);
            } catch (ParseException ignored) {}
        }
        
        return value; // Return original if parsing fails
    }

    private String cleanName(String value) {
        // Remove digits and invalid symbols, keep spaces, hyphens, apostrophes
        String cleaned = value.replaceAll("[^A-Za-z\\s'-]", "").trim();
        
        // Title case
        if (cleaned.isEmpty()) return "";
        
        String[] words = cleaned.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (result.length() > 0) result.append(" ");
            
            // Handle hyphenated names
            if (word.contains("-")) {
                String[] parts = word.split("-");
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) result.append("-");
                    if (!parts[i].isEmpty()) {
                        result.append(Character.toUpperCase(parts[i].charAt(0)));
                        if (parts[i].length() > 1) {
                            result.append(parts[i].substring(1));
                        }
                    }
                }
            } else {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
            }
        }
        
        return result.toString();
    }

    private String cleanPhone(String value) {
        // Extract digits only
        String digits = value.replaceAll("[^0-9]", "");
        
        // Keep last 10-12 digits
        if (digits.length() >= 10) {
            int start = Math.max(0, digits.length() - 12);
            return digits.substring(start);
        }
        
        return digits.isEmpty() ? "" : digits;
    }

    private String cleanCategorical(String value) {
        String cleaned = value.trim();
        
        // Title case
        if (cleaned.isEmpty()) return "";
        
        String[] words = cleaned.toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (result.length() > 0) result.append(" ");
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    result.append(word.substring(1));
                }
            }
        }
        
        cleaned = result.toString();
        
        // Map common role-like words
        Map<String, String> roleMapping = Map.of(
            "admin", "Admin",
            "user", "User",
            "manager", "Manager",
            "administrator", "Admin",
            "customer", "Customer",
            "employee", "Employee"
        );
        
        String lowerCleaned = cleaned.toLowerCase();
        if (roleMapping.containsKey(lowerCleaned)) {
            return roleMapping.get(lowerCleaned);
        }
        
        return cleaned;
    }

    private void enhanceWithAI(Sheet sheet, List<ColumnMetadata> metadata) {
        if (openAIService == null) return;
        
        // Extract samples for AI analysis
        Map<String, List<String>> columnSamples = new HashMap<>();
        
        for (ColumnMetadata colMeta : metadata) {
            if (colMeta.getSamples().size() >= 3) {
                columnSamples.put("Column" + (colMeta.getIndex() + 1), 
                    colMeta.getSamples().stream().limit(20).collect(Collectors.toList()));
            }
        }
        
        if (columnSamples.isEmpty()) return;
        
        LOGGER.info("Calling AI service for column type inference...");
        Map<String, OpenAIService.ColumnInference> aiInferences = openAIService.inferColumnTypes(columnSamples);
        
        // Apply AI inferences to metadata
        for (ColumnMetadata colMeta : metadata) {
            String columnKey = "Column" + (colMeta.getIndex() + 1);
            OpenAIService.ColumnInference inference = aiInferences.get(columnKey);
            
            if (inference != null) {
                try {
                    ColumnType aiType = ColumnType.valueOf(inference.getType().toUpperCase());
                    // Use AI type if it's more specific than UNKNOWN
                    if (aiType != ColumnType.UNKNOWN || colMeta.getType() == ColumnType.UNKNOWN) {
                        colMeta.setType(aiType);
                        LOGGER.info("AI inferred type for Column {}: {}", colMeta.getIndex() + 1, aiType);
                    }
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Unknown AI inferred type: {}", inference.getType());
                }
            }
        }
    }

    // Inner classes
    public static class CleanResult {
        private List<ColumnMetadata> columnMetadata = new ArrayList<>();
        private int cleanedRows = 0;
        private int totalRows = 0;
        private List<Correction> corrections = new ArrayList<>();

        public void addCorrection(int row, int col, String original, String cleaned) {
            corrections.add(new Correction(row, col, original, cleaned));
        }

        public void printReport() {
            System.out.println("\n=== Excel Cleaning Report ===");
            System.out.println("Total Rows: " + totalRows);
            System.out.println("Cleaned Rows: " + cleanedRows);
            System.out.println("Total Corrections: " + corrections.size());
            System.out.println("\nColumn Types:");
            
            for (ColumnMetadata meta : columnMetadata) {
                System.out.printf("  Column %d: %s (Corrections: %d)%n", 
                    meta.getIndex() + 1, meta.getType(), meta.getCorrections());
            }
            
            if (!corrections.isEmpty()) {
                System.out.println("\nSample Corrections (first 10):");
                corrections.stream()
                    .limit(10)
                    .forEach(c -> System.out.printf("  Row %d, Col %d: '%s' -> '%s'%n", 
                        c.row + 1, c.col + 1, c.original, c.cleaned));
            }
        }

        // Getters and setters
        public List<ColumnMetadata> getColumnMetadata() { return columnMetadata; }
        public void setColumnMetadata(List<ColumnMetadata> columnMetadata) { this.columnMetadata = columnMetadata; }
        public int getCleanedRows() { return cleanedRows; }
        public void setCleanedRows(int cleanedRows) { this.cleanedRows = cleanedRows; }
        public int getTotalRows() { return totalRows; }
        public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
        public List<Correction> getCorrections() { return corrections; }
    }

    public static class ColumnMetadata {
        private final int index;
        private ColumnType type;
        private final List<String> samples;
        private int corrections = 0;

        public ColumnMetadata(int index, ColumnType type, List<String> samples) {
            this.index = index;
            this.type = type;
            this.samples = samples;
        }

        public void incrementCorrections() { corrections++; }

        // Getters and setters
        public int getIndex() { return index; }
        public ColumnType getType() { return type; }
        public void setType(ColumnType type) { this.type = type; }
        public List<String> getSamples() { return samples; }
        public int getCorrections() { return corrections; }
    }

    public static class Correction {
        public final int row;
        public final int col;
        public final String original;
        public final String cleaned;

        public Correction(int row, int col, String original, String cleaned) {
            this.row = row;
            this.col = col;
            this.original = original;
            this.cleaned = cleaned;
        }
    }

    public enum ColumnType {
        EMAIL, NUMERIC, DATE, NAME, PHONE, CATEGORICAL, UNKNOWN
    }
}

