package kaiquebt.dev.termolivre.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class DeepSeekAiProvider implements AiProvider {
    
    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int MAX_BATCH_SIZE = 10;
    
    @Value("${deepseek.api.key:}")
    private String apiKey;
    
    @Value("${deepseek.api.url:https://api.deepseek.com/chat/completions}")
    private String apiUrl;
    
    private final RestTemplate restTemplate;
    
    private static final Logger logger = LoggerFactory.getLogger(DeepSeekAiProvider.class);
    
    public DeepSeekAiProvider() {
        this.restTemplate = new RestTemplate();
        logger.debug("DeepSeekAiProvider constructor: RestTemplate initialized");
    }
    
    @jakarta.annotation.PostConstruct
    private void init() {
        logger.info("DeepSeekAiProvider initialized. API URL: {}, API key present: {}", apiUrl, apiKey != null && !apiKey.isEmpty());
    }
    
    @Override
    public List<Boolean> analyzeMessagesForTermoAnswers(List<String> messages) {
        logger.debug("analyzeMessagesForTermoAnswers called with {} messages", messages == null ? 0 : messages.size());
        List<Boolean> results = new ArrayList<>();
        
        // Filtrar mensagens muito longas
        List<String> filteredMessages = new ArrayList<>();
        List<Integer> originalIndices = new ArrayList<>();
        int longCount = 0;
        
        for (int i = 0; i < messages.size(); i++) {
            String message = messages.get(i);
            if (message != null && message.length() <= MAX_MESSAGE_LENGTH) {
                filteredMessages.add(message);
                originalIndices.add(i);
            } else {
                // Mensagens muito longas são consideradas seguras (false)
                results.add(false);
                longCount++;
                logger.debug("Message at index {} considered too long or null, marked safe", i);
            }
        }
        
        logger.info("Filtered out {} messages longer than {} chars (treated as safe)", longCount, MAX_MESSAGE_LENGTH);
        
        // Inicializar a lista de resultados com false para todas as posições
        // Preenchemos com false inicialmente e depois atualizamos com os resultados da IA
        for (int i = 0; i < messages.size(); i++) {
            results.add(false);
        }
        
        logger.debug("Processing {} filtered messages in batches of {}", filteredMessages.size(), MAX_BATCH_SIZE);
        // Processar em lotes
        for (int i = 0; i < filteredMessages.size(); i += MAX_BATCH_SIZE) {
            int end = Math.min(i + MAX_BATCH_SIZE, filteredMessages.size());
            List<String> batch = filteredMessages.subList(i, end);
            logger.debug("Processing batch from filtered index {} to {} (size {})", i, end - 1, batch.size());
            List<Boolean> batchResults = processBatch(batch);
            
            // Mapear os resultados de volta para as posições originais
            for (int j = 0; j < batchResults.size(); j++) {
                int originalIndex = originalIndices.get(i + j);
                results.set(originalIndex, batchResults.get(j));
            }
        }
        
        return results;
    }
    
    private List<Boolean> processBatch(List<String> messages) {
        if (messages.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Preparar o prompt
        String prompt = buildPrompt(messages);
        
        // Configurar headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.setBearerAuth(apiKey);
        }
        
        // Construir o request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-chat");
        
        List<Map<String, String>> messageList = new ArrayList<>();
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "You are a moderator for a word game stream. " +
                "Analyze if the user is trying to reveal the answer to the current word puzzle. " +
                "Respond with only 'true' or 'false' for each message, separated by commas.");
        
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        
        messageList.add(systemMessage);
        messageList.add(userMessage);
        requestBody.put("messages", messageList);
        requestBody.put("temperature", 0.1);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            logger.debug("Sending request to DeepSeek API (prompt length: {})", prompt.length());
            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.POST, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();
            logger.debug("DeepSeek API response status: {}, body: {}", response.getStatusCode(), responseBody);
            
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) firstChoice.get("message");
                    if (message != null) {
                        String content = message.get("content");
                        return parseAiResponse(content, messages.size());
                    } else {
                        logger.warn("DeepSeek response 'message' field is null in first choice");
                    }
                } else {
                    logger.warn("DeepSeek response 'choices' is empty");
                }
            } else {
                logger.warn("DeepSeek response missing 'choices' or response body is null");
            }
        } catch (Exception e) {
            logger.error("Error calling DeepSeek API", e);
        }
        
        // Em caso de erro, considerar todas as mensagens como seguras
        logger.info("Returning default safe results for batch of size {}", messages.size());
        return Collections.nCopies(messages.size(), false);
    }
    
    private String buildPrompt(List<String> messages) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the following chat messages and determine if the user is trying to reveal the answer to the word puzzle. ")
              .append("Respond with only 'true' or 'false' for each message, separated by commas. Here are the messages:\n\n");
        
        for (int i = 0; i < messages.size(); i++) {
            prompt.append(i + 1).append(". \"").append(messages.get(i)).append("\"\n");
        }
        
        prompt.append("\nResponse format: true,false,true,...");
        return prompt.toString();
    }
    
    private List<Boolean> parseAiResponse(String response, int expectedCount) {
        if (response == null) {
            logger.warn("AI response content is null; returning {} false values", expectedCount);
            return Collections.nCopies(expectedCount, false);
        }
        
        List<Boolean> results = new ArrayList<>();
        String[] parts = response.trim().split(",");
        for (String part : parts) {
            String trimmed = part.trim().toLowerCase();
            if ("true".equals(trimmed)) {
                results.add(true);
            } else {
                results.add(false);
            }
        }
        
        // Se a IA não retornou o número esperado de respostas, preencher com false
        if (results.size() < expectedCount) {
            logger.warn("AI returned {} results but {} were expected; filling remaining with false", results.size(), expectedCount);
        }
        while (results.size() < expectedCount) {
            results.add(false);
        }
        
        // Se retornou mais do que o esperado, truncar
        if (results.size() > expectedCount) {
            logger.warn("AI returned {} results but only {} expected; truncating", results.size(), expectedCount);
            results = results.subList(0, expectedCount);
        }
        
        return results;
    }
}
