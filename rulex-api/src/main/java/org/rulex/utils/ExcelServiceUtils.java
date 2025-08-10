package org.rulex.utils;

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

}
