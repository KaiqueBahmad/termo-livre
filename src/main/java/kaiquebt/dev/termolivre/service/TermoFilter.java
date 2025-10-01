package kaiquebt.dev.termolivre.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class TermoFilter {
    
    @Autowired
    private AiProvider aiProvider;

    // Palavras resposta do termo (hardcoded para exemplo)
    // Na prática, isso poderia vir de um banco de dados ou ser atualizado diariamente
    private final Set<String> termoAnswers = new HashSet<>(Arrays.asList(
        "casa", "porta", "livro", "mesa", "cadeira", 
        "banco", "praia", "flore", "vento", "chuva"
    ));
    
    public boolean isMessageSafe(String message) {
        if (message == null || message.trim().isEmpty()) {
            System.out.println("Message is null or empty, returning true");
            return true;
        }
        
        String cleanedMessage = cleanMessage(message);
        System.out.println("Cleaned message: " + cleanedMessage);
        
        // Verificar se a mensagem contém diretamente alguma das palavras resposta
        for (String answer : termoAnswers) {
            if (containsWord(cleanedMessage, answer)) {
                System.out.println("Found direct answer match: " + answer);
                return false;
            }
        }
        
        // Verificar por tentativas comuns de ofuscação
        if (containsObfuscatedTermoAnswer(cleanedMessage)) {
            System.out.println("Found obfuscated answer");
            return false;
        }
        
        // Se passou pelas verificações básicas, usar IA para análise mais sofisticada
        System.out.println("Using AI for advanced analysis");
        List<String> singleMessageList = Collections.singletonList(message);
        List<Boolean> aiResults = aiProvider.analyzeMessagesForTermoAnswers(singleMessageList);
        boolean result = !aiResults.get(0);
        System.out.println("AI analysis result: " + result);
        return result;
    }
    public List<Boolean> areMessagesSafe(List<String> messages) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Boolean> results = new ArrayList<>();
        List<String> toAiAnalysis = new ArrayList<>();
        List<Integer> aiAnalysisIndices = new ArrayList<>();
        
        // Primeira verificação: regras básicas
        for (int i = 0; i < messages.size(); i++) {
            String message = messages.get(i);
            if (message == null || message.trim().isEmpty()) {
                results.add(true);
                continue;
            }
            
            String cleanedMessage = cleanMessage(message);
            boolean isUnsafe = false;
            
            // Verificar palavras resposta diretas
            for (String answer : termoAnswers) {
                if (containsWord(cleanedMessage, answer)) {
                    isUnsafe = true;
                    break;
                }
            }
            
            // Verificar ofuscações
            if (!isUnsafe) {
                isUnsafe = containsObfuscatedTermoAnswer(cleanedMessage);
            }
            
            if (isUnsafe) {
                results.add(false);
            } else {
                // Marcar para análise pela IA
                toAiAnalysis.add(message);
                aiAnalysisIndices.add(i);
                results.add(true); // Temporariamente true, será atualizado pela IA
            }
        }
        
        // Análise pela IA em lote
        if (!toAiAnalysis.isEmpty()) {
            List<Boolean> aiResults = aiProvider.analyzeMessagesForTermoAnswers(toAiAnalysis);
            for (int i = 0; i < aiResults.size(); i++) {
                int originalIndex = aiAnalysisIndices.get(i);
                // Se a IA detectou que é unsafe, atualizar o resultado
                if (aiResults.get(i)) {
                    results.set(originalIndex, false);
                }
            }
        }
        
        return results;
    }
    
    private String cleanMessage(String message) {
        // Converter para minúsculas e remover acentos
        String cleaned = message.toLowerCase()
                .replaceAll("[áàâãä]", "a")
                .replaceAll("[éèêë]", "e")
                .replaceAll("[íìîï]", "i")
                .replaceAll("[óòôõö]", "o")
                .replaceAll("[úùûü]", "u")
                .replaceAll("[ç]", "c")
                .replaceAll("[^a-z0-9 ]", " ") // Remove caracteres especiais, mantém espaços
                .replaceAll("\\s+", " ") // Normaliza espaços múltiplos
                .trim();
        return cleaned;
    }
    
    private boolean containsWord(String text, String word) {
        // Verifica se a palavra está presente como uma palavra completa
        String pattern = "\\b" + Pattern.quote(word) + "\\b";
        return Pattern.compile(pattern).matcher(text).find();
    }
    
    private boolean containsObfuscatedTermoAnswer(String message) {
        // Verificar por ofuscações comuns
        
        for (String answer : termoAnswers) {
            // 1. Verificar se a mensagem contém a palavra com espaços entre as letras
            String spaced = String.join(" ", answer.split(""));
            if (message.contains(spaced)) {
                return true;
            }
            
            // 2. Verificar se a mensagem contém a palavra com caracteres repetidos
            // Exemplo: "ccaassaa" para "casa"
            String doubled = doubleCharacters(answer);
            if (message.contains(doubled)) {
                return true;
            }
            
            // 3. Verificar por substituições comuns de caracteres
            // Exemplo: substituir 'a' por '@', 'o' por '0', etc.
            String leet = toLeetSpeak(answer);
            if (message.contains(leet)) {
                return true;
            }
            
            // 4. Verificar se a palavra está escrita ao contrário
            String reversed = new StringBuilder(answer).reverse().toString();
            if (containsWord(message, reversed)) {
                return true;
            }
            
            // 5. Verificar por remoção de vogais (tentativa comum)
            String withoutVowels = removeVowels(answer);
            if (message.contains(withoutVowels) && withoutVowels.length() >= 3) {
                return true;
            }
        }
        
        return false;
    }
    
    private String doubleCharacters(String word) {
        StringBuilder result = new StringBuilder();
        for (char c : word.toCharArray()) {
            result.append(c).append(c);
        }
        return result.toString();
    }
    
    private String toLeetSpeak(String word) {
        return word.toLowerCase()
                .replace('a', '@')
                .replace('e', '3')
                .replace('i', '1')
                .replace('o', '0')
                .replace('s', '5')
                .replace('t', '7');
    }
    
    private String removeVowels(String word) {
        return word.replaceAll("[aeiou]", "");
    }
}
