package com.TodoTalk.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.endpoint}")
    private String apiEndpoint;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public TaskAnalysisResult analyzeTaskMessage(String messageContent) {
        log.info("=== GEMINI SERVICE: Analyzing task message ===");
        log.info("Input message content: '{}'", messageContent);
        
        try {
            log.info("Building AI prompt...");
            String prompt = buildTaskAnalysisPrompt(messageContent);
            log.debug("Generated prompt: {}", prompt);
            
            log.info("Calling Gemini API...");
            String response = callGeminiAPI(prompt);
            log.info("Gemini API response received: '{}'", response);
            
            log.info("Parsing AI response...");
            TaskAnalysisResult result = parseTaskAnalysisResponse(response);
            log.info("AI Analysis completed - Description: '{}', DueDate: {}, AI Processed: {}", 
                     result.getDescription(), result.getDueDate(), result.isAiProcessed());
            
            return result;
        } catch (Exception e) {
            log.error("Error analyzing task message: {}", e.getMessage(), e);
            log.info("Falling back to simple task creation...");
            TaskAnalysisResult fallback = createFallbackTask(messageContent);
            log.info("Fallback task created - Description: '{}'", fallback.getDescription());
            return fallback;
        }
    }

    private String buildTaskAnalysisPrompt(String messageContent) {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        LocalDate nextWeek = today.plusDays(7);
        LocalDate nextMonth = today.plusMonths(1);
        LocalDate thisSaturday = today.plusDays((6 - today.getDayOfWeek().getValue()) % 7 + 1);
        
        return String.format("""
            Bạn là AI assistant giúp phân tích tin nhắn có chứa "@Todo" để trích xuất thông tin task.
            
            Tin nhắn: "%s"
            
            Hôm nay là: %s (%s)
            Ngày mai là: %s (%s)
            Tuần tới là: %s
            Tháng tới là: %s
            
            Hãy phân tích và trả về JSON với format chính xác như sau:
            {
              "description": "mô tả task đã được làm sạch (bỏ @Todo)",
              "dueDate": "YYYY-MM-DDTHH:MM:SS hoặc null nếu không có thời gian cụ thể"
            }
            
            Quy tắc phân tích thời gian tiếng Việt:
            - "hôm nay" hoặc "today": %sT23:59:59
            - "ngày mai" hoặc "tomorrow": %sT23:59:59  
            - "tuần tới" hoặc "next week": %sT23:59:59
            - "tháng tới" hoặc "next month": %sT23:59:59
            - "cuối tuần" hoặc "weekend": %sT23:59:59 (thứ 7 tuần này)
            - Thời gian cụ thể: "hôm nay lúc 15:30" → %sT15:30:00
            - Giờ sáng: "ngày mai 9h sáng" → %sT09:00:00
            - Giờ chiều: "hôm nay 3h chiều" → %sT15:00:00
            - Giờ tối: "ngày mai 8h tối" → %sT20:00:00
            - Không có thời gian cụ thể: null
            
            Lưu ý:
            - description: bỏ "@Todo" và các từ chỉ thời gian, chỉ giữ lại nội dung task
            - dueDate: phải là ISO DateTime format YYYY-MM-DDTHH:MM:SS hoặc null
            - Phân tích thời gian tiếng Việt một cách chính xác và thông minh
            - Nếu không chắc chắn về thời gian, trả về null
            
            VÍ DỤ:
            "@Todo hôm nay tôi sẽ làm bài tập" → {"description": "làm bài tập", "dueDate": "%sT23:59:59"}
            "@Todo ngày mai lúc 9h họp team" → {"description": "họp team", "dueDate": "%sT09:00:00"}
            "@Todo làm báo cáo" → {"description": "làm báo cáo", "dueDate": null}
            
            Chỉ trả về JSON, không có text khác.
            """, 
            messageContent,          // 1: tin nhắn
            today,                   // 2: hôm nay là
            today.getDayOfWeek(),    // 3: thứ mấy hôm nay
            tomorrow,                // 4: ngày mai là  
            tomorrow.getDayOfWeek(), // 5: thứ mấy ngày mai
            nextWeek,                // 6: tuần tới
            nextMonth,               // 7: tháng tới
            today,                   // 8: cho "hôm nay"
            tomorrow,                // 9: cho "ngày mai" 
            nextWeek,                // 10: cho "tuần tới"
            nextMonth,               // 11: cho "tháng tới"
            thisSaturday,            // 12: thứ 7 tuần này
            today,                   // 13: cho "hôm nay lúc XX"
            tomorrow,                // 14: cho "ngày mai XXh"
            today,                   // 15: ví dụ 1 (dueDate ví dụ hôm nay)
            tomorrow,                // 16: ví dụ 2 (dueDate ví dụ ngày mai)
            today,                   // 17: ví dụ 1 lặp lại cho JSON mẫu
            tomorrow                 // 18: ví dụ 2 lặp lại cho JSON mẫu
        );
    }

    private String callGeminiAPI(String prompt) throws Exception {
        log.info("=== CALLING GEMINI API ===");
        String url = apiEndpoint + "?key=" + apiKey.substring(0, Math.min(10, apiKey.length())) + "...";
        log.info("API URL: {}", url.replace(apiKey.substring(0, Math.min(10, apiKey.length())), "***"));
        log.info("API Endpoint: {}", apiEndpoint);
        log.info("API Key configured: {}", apiKey != null && !apiKey.isEmpty() ? "Yes" : "No");

        // Build request body theo format của Gemini API
        log.info("Building request body...");
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, Object> part = new HashMap<>();
        
        part.put("text", prompt);
        content.put("parts", List.of(part));
        requestBody.put("contents", List.of(content));

        // Set generation config
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.3);
        generationConfig.put("maxOutputTokens", 1000);
        requestBody.put("generationConfig", generationConfig);
        
        log.info("Request body prepared, making HTTP call...");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        log.info("Making HTTP POST request to Gemini API...");
        String fullUrl = apiEndpoint + "?key=" + apiKey;
        ResponseEntity<String> response = restTemplate.exchange(fullUrl, HttpMethod.POST, entity, String.class);
        
        log.info("Gemini API response status: {}", response.getStatusCode());
        log.info("Gemini API response body length: {}", response.getBody() != null ? response.getBody().length() : 0);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            log.info("Extracting text from Gemini response...");
            String extractedText = extractTextFromGeminiResponse(response.getBody());
            log.info("Extracted text: '{}'", extractedText);
            return extractedText;
        } else {
            log.error("Gemini API call failed with status: {}, body: {}", 
                     response.getStatusCode(), response.getBody());
            throw new RuntimeException("Gemini API call failed: " + response.getStatusCode());
        }
    }

    private String extractTextFromGeminiResponse(String responseBody) throws Exception {
        JsonNode rootNode = objectMapper.readTree(responseBody);
        JsonNode candidates = rootNode.path("candidates");
        
        if (candidates.isArray() && candidates.size() > 0) {
            JsonNode content = candidates.get(0).path("content");
            JsonNode parts = content.path("parts");
            
            if (parts.isArray() && parts.size() > 0) {
                return parts.get(0).path("text").asText();
            }
        }
        
        throw new RuntimeException("Could not extract text from Gemini response");
    }

    private TaskAnalysisResult parseTaskAnalysisResponse(String response) {
        try {
            // Clean response - remove markdown code blocks if present
            String cleanedResponse = response.trim();
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
            }
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
            }
            cleanedResponse = cleanedResponse.trim();

            JsonNode jsonNode = objectMapper.readTree(cleanedResponse);
            
            String description = jsonNode.path("description").asText();
            String dueDateStr = jsonNode.path("dueDate").asText();
            
            LocalDateTime dueDate = null;
            if (!"null".equals(dueDateStr) && !dueDateStr.isEmpty()) {
                try {
                    dueDate = LocalDateTime.parse(dueDateStr);
                } catch (Exception e) {
                    log.warn("Could not parse due date: {}", dueDateStr);
                }
            }
            
            return new TaskAnalysisResult(description, dueDate, true);
        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage(), e);
            throw new RuntimeException("Could not parse AI response", e);
        }
    }

    private TaskAnalysisResult createFallbackTask(String messageContent) {
        // Fallback: tạo task cơ bản nếu AI không hoạt động
        String description = messageContent.replace("@Todo", "").trim();
        return new TaskAnalysisResult(description, null, false);
    }

    // Inner class for task analysis result
    public static class TaskAnalysisResult {
        private final String description;
        private final LocalDateTime dueDate;
        private final boolean aiProcessed;

        public TaskAnalysisResult(String description, LocalDateTime dueDate, boolean aiProcessed) {
            this.description = description;
            this.dueDate = dueDate;
            this.aiProcessed = aiProcessed;
        }

        public String getDescription() { return description; }
        public LocalDateTime getDueDate() { return dueDate; }
        public boolean isAiProcessed() { return aiProcessed; }
    }
}
