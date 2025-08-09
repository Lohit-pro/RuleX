package org.rulex.controller;

import org.rulex.dto.ColumnRule;
import org.rulex.dto.ValidationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class ValidationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationController.class);

    @PostMapping("/validate")
    private ResponseEntity<?> validateExcelController(@RequestBody ValidationRequest request) {
        for (ColumnRule rule : request.getColumnRules()) {
            LOGGER.info("Rule for column : {} is : {}", rule, request.getColumnRules());
        }

        return ResponseEntity.ok("Validation done");
    }

    @GetMapping("/test")
    private String test() {
        return "test";
    }

}
