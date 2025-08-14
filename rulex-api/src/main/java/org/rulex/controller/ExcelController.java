package org.rulex.controller;

import org.rulex.dto.RequestDTO;
import org.rulex.service.ExcelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api")
public class ExcelController {

    @Autowired
    ExcelService excelService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelController.class);

    @PostMapping("/headers")
    public List<String> getHeaders(@RequestParam("file") MultipartFile file) throws IOException {
        return excelService.getColumnHeaders(file);
    }

    @PostMapping("/validate")
    public List<String> validateExcel(@RequestBody RequestDTO requestDTO) {
        return excelService.validateExcel(requestDTO);
    }

    @GetMapping("/test")
    public String test(){
        return "test";
    }

}
