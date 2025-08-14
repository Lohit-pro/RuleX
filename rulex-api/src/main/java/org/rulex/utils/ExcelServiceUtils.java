package org.rulex.utils;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;

@Service
public class ExcelServiceUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelServiceUtils.class);
    private static final String XL_FILES_PATH = "D:/RuleX/rulex-api/src/main/resources/xlfiles/";

    public void saveExcelAtBackend(MultipartFile file) throws IOException {
        try {
            File dir = new File(XL_FILES_PATH);
            if (!dir.exists()) {
                LOGGER.info("Creating a directory to save files");
                dir.mkdir();
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

    public boolean isNumber(Cell cell) {
        return cell.getCellType() == CellType.NUMERIC;
    }

    public boolean isUpperAlphabet(Cell cell) {
        String cellValue = cell.getStringCellValue().trim();
        return cellValue.matches("[A-Z]+");
    }

    public boolean isLowerAlphabet(Cell cell) {
        String cellValue = cell.getStringCellValue().trim();
        return cellValue.matches("[a-z]+");
    }

    public boolean isAlphabet(Cell cell) {
        String cellValue = cell.getStringCellValue().trim();
        return cellValue.matches("[\\p{L}\\s]+");
    }

    public boolean isEmail(Cell cell) {
        String cellValue = cell.getStringCellValue().trim();
        return cellValue.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    public boolean isDate(Cell cell) {
        return false;
    }

    public boolean isNonEmpty(Cell cell) {
        return !cell.getStringCellValue().isEmpty();
    }

    public boolean isInRange(Integer num, Integer min, Integer max) {
        return min <= num && num <= max;
    }

}
