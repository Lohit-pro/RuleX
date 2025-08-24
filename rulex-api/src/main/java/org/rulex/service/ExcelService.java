package org.rulex.service;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.rulex.dto.ColumnValidationDTO;
import org.rulex.dto.Rule;
import org.rulex.utils.ExcelServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelService {

    @Autowired
    ExcelServiceUtils excelServiceUtils;
    private static final String ERROR_FILES_PATH = "./src/main/resources/errorcells/";
    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelService.class);

    public List<String> validateExcel(ColumnValidationDTO requestDTO) {
        List<String> errorCells = new ArrayList<>();

        try {
            File file = excelServiceUtils.loadFileFromBackend(requestDTO.getFileName());
            FileInputStream fis = new FileInputStream(file);
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);

            List<ColumnValidationDTO.ColumnRule> rules = requestDTO.getRules();
            Map<String, Rule> ruleMap = new HashMap<>();
            for (ColumnValidationDTO.ColumnRule colRule : rules) {
                ruleMap.put(colRule.getColumn(), colRule.getRule());
            }

            Row headerRow = sheet.getRow(0);
            Map<String, Integer> columnIndexMap = new HashMap<>();
            for (Cell cell : headerRow) {
                columnIndexMap.put(cell.getStringCellValue(), cell.getColumnIndex());
            }

            DataFormatter formatter = new DataFormatter();

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                for (String column : ruleMap.keySet()) {
                    Integer colIndex = columnIndexMap.get(column);
                    if (colIndex == null) continue;

                    Cell cell = row.getCell(colIndex);
                    String cellValue = (cell != null) ? formatter.formatCellValue(cell).trim() : "";

                    Rule rule = ruleMap.get(column);
                    boolean isValid = true;

                    switch (rule.getType().toLowerCase()) {
                        case "numeric":
                            isValid = excelServiceUtils.isNumericWithRule(cellValue, rule);
                            break;
                        case "alphabet":
                            isValid = excelServiceUtils.isAlphabetWithRule(cellValue, rule);
                            break;
                        case "email":
                            isValid = excelServiceUtils.isEmail(cellValue);
                            break;
                        case "date":
                            isValid = excelServiceUtils.isDate(cellValue);
                            break;
                        case "non-empty":
                            isValid = !cellValue.isEmpty();
                            break;
                        default:
                            break;
                    }

                    if (!isValid) {
                        CellReference cellRef = new CellReference(i, colIndex);
                        errorCells.add(cellRef.formatAsString());
                    }
                }
            }

            fis.close();

            // Save errors to CSV file
            File dir = new File(ERROR_FILES_PATH);
            if (!dir.exists()) {
                LOGGER.info("Creating a directory to save files");
                dir.mkdirs();
            }
            File errorFile = new File(ERROR_FILES_PATH + "ErrorCells_" + requestDTO.getFileName().replaceAll("\\.xlsx$", "") + ".csv");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(errorFile))) {
                writer.write(String.join(",", errorCells));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return errorCells;
    }

    public HashMap<String, String> getColumnHeadersWithDefaultType(MultipartFile file) {

        try {
            String fileName = file.getOriginalFilename();
            LOGGER.info("File Name : {}", fileName);

            excelServiceUtils.saveExcelAtBackend(file);

            InputStream is = file.getInputStream();
            Workbook workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            HashMap<String, String> headers = new HashMap<>();
            for (Cell cell : headerRow) {
                headers.put(cell.getStringCellValue(), excelServiceUtils.getMajorityColumnDataType(file, cell.getColumnIndex()));
            }

            return headers;
        } catch (IOException e) {
            e.printStackTrace();
            excelServiceUtils.removeFileFromBackend(file.getOriginalFilename());
        }

        return new HashMap<>();
    }

    public ResponseEntity<Resource> downloadReport(String fileName) {
        try {
            File file = excelServiceUtils.loadFileFromBackend(fileName);
            List<String> errorCells = excelServiceUtils.loadErrorCellsData(fileName);
            excelServiceUtils.highlightErrorFields(file, errorCells, fileName);

            File reportFile = new File("./src/main/resources/xlfiles/Report_" + fileName);

            InputStreamResource resource = new InputStreamResource(new FileInputStream(reportFile));

            excelServiceUtils.removeFileFromBackend(fileName);
            excelServiceUtils.removeFileFromBackend("Report_" + fileName);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + reportFile.getName())
                    .contentLength(reportFile.length())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(resource);

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
