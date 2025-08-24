package org.rulex.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.rulex.dto.ColumnValidationDTO;
import org.rulex.service.ExcelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@CrossOrigin(origins = "https://rule-x.vercel.app/")
@RestController
@RequestMapping("/api")
public class ExcelController {

    @Autowired
    ExcelService excelService;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExcelController.class);

    @PostMapping("/headers")
    public ResponseEntity<HashMap<String, String>> getHeaders(@RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(excelService.getColumnHeadersWithDefaultType(file));
    }

    @PostMapping("/validate")
    public List<String> validateExcel(@RequestBody ColumnValidationDTO requestDTO) throws JsonProcessingException {
        System.out.println(new ObjectMapper().writeValueAsString(requestDTO));
        return excelService.validateExcel(requestDTO);
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadReport(@RequestParam String fileName) throws IOException {
        return excelService.downloadReport(fileName);
    }

    @GetMapping("/test")
    public List<Integer> test(){
        ArrayList<Integer> list = new ArrayList<Integer>(10);
        list.add(1000);
        list.add(2000);
        list.add(30000);
        return list;
    }

}
