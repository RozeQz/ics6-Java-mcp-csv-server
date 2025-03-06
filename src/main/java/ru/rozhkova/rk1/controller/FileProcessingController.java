package ru.rozhkova.rk1.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import lombok.extern.slf4j.Slf4j;

import ru.rozhkova.rk1.service.FileProcessingService;

@RestController
@Slf4j
public class FileProcessingController {

    private final FileProcessingService fileProcessingService;

    public FileProcessingController(FileProcessingService fileProcessingService) {
        this.fileProcessingService = fileProcessingService;
    }

    @PostMapping(value = "/process-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> processFile(@RequestParam("file") MultipartFile file,
                                         @RequestParam("prompt") String prompt) {
        log.info("Received request to process file: {}", file.getOriginalFilename());
        try {
            String llmResponse = fileProcessingService.processFile(file, prompt);
            log.info("File processed successfully: {}", file.getOriginalFilename());
            return ResponseEntity.ok(llmResponse);
        } catch (IllegalArgumentException e) {
            log.error("Unsupported file type: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error processing file: {}", file.getOriginalFilename(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing file: " + e.getMessage());
        }
    }
}
