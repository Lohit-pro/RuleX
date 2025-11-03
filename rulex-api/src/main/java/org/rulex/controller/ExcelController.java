package org.rulex.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.MessagingException;
import org.rulex.dto.ColumnInference;
import org.rulex.dto.ColumnValidationDTO;
import org.rulex.dto.CleanResultDTO;
import org.rulex.dto.RefinementRequestDTO;
import org.rulex.service.ExcelAutoCleaner;
import org.rulex.service.ExcelService;
import org.rulex.service.OpenAIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "${host.dns}")
@RestController
@RequestMapping("/api")
public class ExcelController {

    @Autowired
    ExcelService excelService;

    @Autowired
    ExcelAutoCleaner excelAutoCleaner;

    @Autowired(required = false)
    OpenAIService openAIService;

    @Value("${excel.files.path}")
    private String XL_FILES_PATH;

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

    @PostMapping("/email")
    public void sendEmail (@RequestParam String toEmail, @RequestParam String fileName) throws MessagingException, IOException {
        excelService.sendEmail(toEmail, fileName);
    }

    @GetMapping("/test")
    public List<Integer> test(){
        ArrayList<Integer> list = new ArrayList<Integer>(10);
        list.add(1000);
        list.add(2000);
        list.add(30000);
        return list;
    }

    @PostMapping("/autoclean")
    public ResponseEntity<CleanResultDTO> autoCleanExcel(@RequestParam("file") MultipartFile file,
                                                         @RequestParam(value = "aiOnly", required = false, defaultValue = "false") boolean aiOnly) throws IOException {
        try {
            // Save uploaded file
            excelService.getColumnHeadersWithDefaultType(file);
            String fileName = file.getOriginalFilename();
            
            // Prepare paths
            String inputPath = XL_FILES_PATH + fileName;
            String outputFileName = "Cleaned_" + fileName;
            String outputPath = XL_FILES_PATH + outputFileName;
            
            // Clean the Excel file
            ExcelAutoCleaner.CleanResult result = excelAutoCleaner.cleanExcel(inputPath, outputPath, aiOnly);
            
            // Convert to DTO
            CleanResultDTO dto = new CleanResultDTO();
            dto.setTotalRows(result.getTotalRows());
            dto.setCleanedRows(result.getCleanedRows());
            dto.setTotalCorrections(result.getCorrections().size());
            
            // Convert column metadata
            List<CleanResultDTO.ColumnInfo> columns = result.getColumnMetadata().stream()
                .map(meta -> new CleanResultDTO.ColumnInfo(
                    meta.getIndex() + 1,
                    meta.getType().name(),
                    meta.getCorrections()
                ))
                .collect(Collectors.toList());
            dto.setColumns(columns);
            
            // Convert sample corrections (first 20)
            List<CleanResultDTO.CorrectionInfo> corrections = result.getCorrections().stream()
                .limit(20)
                .map(c -> new CleanResultDTO.CorrectionInfo(
                    c.row + 1,
                    c.col + 1,
                    c.original,
                    c.cleaned
                ))
                .collect(Collectors.toList());
            dto.setSampleCorrections(corrections);
            
            // Store AI inference and column samples for refinement
            if (result.getAiInferences() != null && !result.getAiInferences().isEmpty()) {
                Map<String, Object> aiInferenceMap = new HashMap<>();
                for (Map.Entry<String, ColumnInference> entry : result.getAiInferences().entrySet()) {
                    Map<String, Object> inferenceData = new HashMap<>();
                    inferenceData.put("type", entry.getValue().getType());
                    inferenceData.put("cleaningRules", entry.getValue().getCleaningRules());
                    aiInferenceMap.put(entry.getKey(), inferenceData);
                }
                dto.setAiInference(aiInferenceMap);
                dto.setColumnSamples(result.getColumnSamples());
            }
            
            return ResponseEntity.ok(dto);
            
        } catch (Exception e) {
            LOGGER.error("Error auto-cleaning Excel file", e);
            throw new IOException("Failed to clean Excel file: " + e.getMessage(), e);
        }
    }

    @GetMapping("/download-cleaned")
    public ResponseEntity<Resource> downloadCleanedFile(@RequestParam String fileName) throws IOException {
        String cleanedFileName = "Cleaned_" + fileName;
        File file = new File(XL_FILES_PATH + cleanedFileName);
        
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        
        Resource resource = new FileSystemResource(file);
        
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + cleanedFileName + "\"")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(resource);
    }

    @PostMapping("/refine-inference")
    public ResponseEntity<CleanResultDTO> refineInference(@RequestBody RefinementRequestDTO request) {
        try {
            if (openAIService == null) {
                return ResponseEntity.badRequest()
                    .header("X-Error-Message", "OpenAI service is not available")
                    .build();
            }
            
            // Check if we have previous inference data
            if (request.getPreviousInference() == null || request.getPreviousInference().isEmpty()) {
                return ResponseEntity.badRequest()
                    .header("X-Error-Message", "No previous inference data available for refinement")
                    .build();
            }

            // Convert previous inference from Map to ColumnInference objects
            Map<String, ColumnInference> previousInference = new HashMap<>();
            if (request.getPreviousInference() != null) {
                for (Map.Entry<String, Object> entry : request.getPreviousInference().entrySet()) {
                    Map<String, Object> inferenceData = (Map<String, Object>) entry.getValue();
                    String type = (String) inferenceData.get("type");
                    @SuppressWarnings("unchecked")
                    List<String> cleaningRules = (List<String>) inferenceData.get("cleaningRules");
                    previousInference.put(entry.getKey(), 
                        new ColumnInference(type, cleaningRules != null ? cleaningRules : new ArrayList<>()));
                }
            }

            // Refine inference
            Map<String, ColumnInference> refinedInference = openAIService.refineInference(
                request.getColumnSamples(),
                previousInference,
                request.getFeedback(),
                request.isAutoRegenerate()
            );

            // Convert refined inference to DTO format
            Map<String, Object> aiInferenceMap = new HashMap<>();
            for (Map.Entry<String, ColumnInference> entry : refinedInference.entrySet()) {
                Map<String, Object> inferenceData = new HashMap<>();
                inferenceData.put("type", entry.getValue().getType());
                inferenceData.put("cleaningRules", entry.getValue().getCleaningRules());
                aiInferenceMap.put(entry.getKey(), inferenceData);
            }

            CleanResultDTO dto = new CleanResultDTO();
            dto.setAiInference(aiInferenceMap);
            dto.setColumnSamples(request.getColumnSamples());

            // Convert to column info format
            List<CleanResultDTO.ColumnInfo> columns = new ArrayList<>();
            int index = 1;
            for (Map.Entry<String, ColumnInference> entry : refinedInference.entrySet()) {
                columns.add(new CleanResultDTO.ColumnInfo(
                    index++,
                    entry.getValue().getType(),
                    0 // corrections will be calculated after re-cleaning
                ));
            }
            dto.setColumns(columns);

            return ResponseEntity.ok(dto);

        } catch (RuntimeException e) {
            // Handle quota/rate limit errors specifically
            if (e.getMessage() != null && (e.getMessage().contains("429") || e.getMessage().contains("quota"))) {
                LOGGER.warn("OpenAI quota exceeded during refinement: {}", e.getMessage());
                return ResponseEntity.status(429)
                    .header("X-Error-Message", "OpenAI API quota exceeded. Please check your billing or try again later.")
                    .build();
            }
            LOGGER.error("Error refining inference", e);
            return ResponseEntity.internalServerError()
                .header("X-Error-Message", "Failed to refine inference: " + e.getMessage())
                .build();
        } catch (Exception e) {
            LOGGER.error("Error refining inference", e);
            return ResponseEntity.internalServerError()
                .header("X-Error-Message", "An unexpected error occurred: " + e.getMessage())
                .build();
        }
    }

}
