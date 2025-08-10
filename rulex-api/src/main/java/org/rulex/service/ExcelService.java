package org.rulex.service;

import org.apache.poi.ss.usermodel.*;
import org.rulex.utils.ExcelServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelService {

    @Autowired
    ExcelServiceUtils excelServiceUtils;
    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelService.class);

    public void validateExcel() {

    }

    public List<String> getColumnHeaders(MultipartFile file) {

        try {
            String fileName = file.getOriginalFilename();
            LOGGER.info("File Name : {}", fileName);

            excelServiceUtils.saveExcelAtBackend(file);

            InputStream is = file.getInputStream();
            Workbook workbook = WorkbookFactory.create(is);
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(cell.getStringCellValue());
            }

            return headers;
        } catch (IOException e) {
            e.printStackTrace();
            excelServiceUtils.removeFileFromBackend(file.getOriginalFilename());
        }

        return new ArrayList<>();
    }

}
