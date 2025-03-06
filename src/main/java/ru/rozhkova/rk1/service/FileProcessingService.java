package ru.rozhkova.rk1.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FileProcessingService {

    private final WebClient webClient;

    @Autowired
    public FileProcessingService(WebClient.Builder webClientBuilder) {
        // Base URL for the LLM service (Ollama in this case)
        this.webClient = webClientBuilder.baseUrl("http://31.192.111.23:11434").build();
    }

    public String processFile(MultipartFile file, String prompt) throws Exception {
        String fileContent;
        String filename = file.getOriginalFilename();
        if (filename != null && filename.toLowerCase().endsWith(".csv")) {
            fileContent = parseCSV(file.getInputStream());
        } else if (filename != null &&
                (filename.toLowerCase().endsWith(".xls") || filename.toLowerCase().endsWith(".xlsx"))) {
            fileContent = parseExcel(file.getInputStream());
        } else {
            throw new IllegalArgumentException("Unsupported file type. Please upload CSV or Excel files.");
        }

        // Combine the user prompt with file content.
        String combinedPrompt = prompt + "\n\nFile Content:\n" + fileContent;

        // Prepare the JSON payload for the LLM request
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", "phi4coder:latest");
        payload.put("prompt", combinedPrompt);
        payload.put("stream", false);

        // Call the LLM API. Adjust the endpoint and request format as per your LLM service.
        return webClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    // Helper method to parse CSV files using Apache Commons CSV
    private String parseCSV(InputStream inputStream) throws Exception {
        StringBuilder sb = new StringBuilder();
        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .parse(new InputStreamReader(inputStream));
        for (CSVRecord record : records) {
            sb.append(record.toString()).append("\n");
        }
        return sb.toString();
    }

    // Helper method to parse Excel files using Apache POI
    private String parseExcel(InputStream inputStream) throws Exception {
        StringBuilder sb = new StringBuilder();
        Workbook workbook = WorkbookFactory.create(inputStream);
        for (Sheet sheet : workbook) {
            for (Row row : sheet) {
                List<String> cells = new ArrayList<>();
                for (Cell cell : row) {
                    cell.setCellType(CellType.STRING);
                    cells.add(cell.getStringCellValue());
                }
                sb.append(String.join(", ", cells)).append("\n");
            }
        }
        workbook.close();
        return sb.toString();
    }
}
