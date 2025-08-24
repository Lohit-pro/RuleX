package org.rulex.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.rulex.dto.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class ExcelServiceUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelServiceUtils.class);
    private static final String XL_FILES_PATH = "./src/main/resources/xlfiles/";
    private static final String ERROR_FILES_PATH = "./src/main/resources/errorcells/";


    public void saveExcelAtBackend(MultipartFile file) throws IOException {
        try {
            File dir = new File(XL_FILES_PATH);
            if (!dir.exists()) {
                LOGGER.info("Creating a directory to save files");
                dir.mkdirs();
            }

            Path filePath = Paths.get(XL_FILES_PATH, file.getOriginalFilename());
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            LOGGER.info("File saved at backend at : {}", filePath);

        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.info("Error while saving file at backend");
        }
    }

    public void removeFileFromBackend(String fileName) {
        try {
            File file = new File(XL_FILES_PATH, fileName);

            if(file.exists()) {
                if (file.delete()) {
                    LOGGER.info("File deleted Successfully");
                } else {
                    LOGGER.warn("Problem while deleting a file");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public File loadFileFromBackend(String fileName) {
        try {
            return new File(XL_FILES_PATH, fileName);
        } catch (Exception e) {
            LOGGER.error("Something went wrong loading the file from the backend");
            throw new RuntimeException(e);
        }
    }

    public void highlightErrorFields(File file, List<String> errorCells, String fileName) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        Workbook workbook = new XSSFWorkbook(fis);
        Sheet sheet = workbook.getSheetAt(0);

        // Create red cell style
        CellStyle errorStyle = workbook.createCellStyle();
        errorStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        errorStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Highlight error cells
        for (String cellRef : errorCells) {
            CellReference ref = new CellReference(cellRef);
            Row row = sheet.getRow(ref.getRow());
            if (row == null) continue;
            Cell cell = row.getCell(ref.getCol());
            if (cell == null) continue;
            cell.setCellStyle(errorStyle);
        }

        fis.close();

        // Save to a new file (or overwrite)
        FileOutputStream fos = new FileOutputStream("./src/main/resources/xlfiles/Report_" + fileName);
        workbook.write(fos);
        workbook.close();
        fos.close();

        LOGGER.info("Report file saved!");
    }

    public List<String> loadErrorCellsData(String fileName) throws FileNotFoundException {
        String errorFileName = "ErrorCells_" + fileName.replaceAll("\\.xlsx$", "") + ".csv";
        File errorCellsFile = new File(ERROR_FILES_PATH + errorFileName);
        List<String> cellValues = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(errorCellsFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                for (String value : values) {
                    cellValues.add(value.trim());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return cellValues;
    }


    public String getMajorityColumnDataType(MultipartFile file, int columnNumber) throws IOException {
        int numericCounter = 0;
        int alphabetCounter = 0;
        int alphanumericCounter = 0;
        int dateCounter = 0;
        int emailCounter = 0;

        InputStream is = file.getInputStream();
        Workbook workbook = WorkbookFactory.create(is);
        Sheet sheet = workbook.getSheetAt(0);

        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) continue;

            Cell cell = row.getCell(columnNumber);
            if (cell == null) continue;

            String value = getCellAsString(cell);
            if (value.isEmpty()) continue;

            if (isEmail(value)) emailCounter++;
            else if (isDate(value)) dateCounter++;
            else if (isNumeric(value)) numericCounter++;
            else if (isAlphabet(value)) alphabetCounter++;
            else alphanumericCounter++;
        }

        int max = Collections.max(Arrays.asList(numericCounter, alphabetCounter, alphanumericCounter, dateCounter, emailCounter));

        if (max == numericCounter) return "Numeric";
        if (max == alphabetCounter) return "Alphabet";
        if (max == alphanumericCounter) return "Alphanumeric";
        if (max == dateCounter) return "Date";
        if (max == emailCounter) return "Email";

        return "Unknown";
    }

    private String getCellAsString(Cell cell) {
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    private boolean isNumeric(String value) {
        return value.matches("-?\\d+(\\.\\d+)?");
    }

    public boolean isNumericWithRule(String value, Rule rule) {
        try {
            double num = Double.parseDouble(value);

            String minStr = rule.getMin();
            String maxStr = rule.getMax();

            if (minStr != null && !minStr.isEmpty()) {
                double min = Double.parseDouble(minStr);
                if (num < min) return false;
            }

            if (maxStr != null && !maxStr.isEmpty()) {
                double max = Double.parseDouble(maxStr);
                if (num > max) return false;
            }

            return true;

        } catch (NumberFormatException e) {
            return false;
        }
    }


    private boolean isAlphabet(String value) {
        return value.matches("^[A-Za-z\\s]+$");
    }

    public boolean isAlphabetWithRule(String value, Rule rule) {
        if (value == null || value.isEmpty()) return false;

        // Check if value is only alphabets
        if (!value.matches("^[a-zA-Z]+$")) return false;

        String caseType = rule.getCaseType(); // should be "upper", "lower", or "both"
        if (caseType == null) return true; // no case rule applied

        switch (caseType.toLowerCase()) {
            case "upper":
                return value.equals(value.toUpperCase());
            case "lower":
                return value.equals(value.toLowerCase());
            case "both":
                return true; // both cases allowed
            default:
                return true; // if unknown, allow it
        }
    }


    public boolean isEmail(String value) {
        return value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    public boolean isDate(String value) {
        String[] formats = {
                "dd/MM/yyyy", "dd-MM-yyyy", "MM/dd/yyyy", "MM-dd-yyyy",
                "yyyy-MM-dd", "yyyy/MM/dd"
        };
        for (String format : formats) {
            try {
                new SimpleDateFormat(format).parse(value);
                return true;
            } catch (ParseException ignored) {}
        }
        return false;
    }
}
