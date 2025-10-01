package kaiquebt.dev.termolivre.service;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class TermoFilter {
    
    // Palavras resposta do termo (hardcoded para exemplo)
    // Na prática, isso poderia vir de um banco de dados ou ser atualizado diariamente
    private final Set<String> termoAnswers = new HashSet<>(Arrays.asList(
        "casa", "porta", "livro", "mesa", "cadeira", 
        "banco", "praia", "flore", "vento", "chuva"
    ));
    
    public boolean isMessageSafe(String message) {
        if (message == null || message.trim().isEmpty()) {
            return true;
        }
        
        String cleanedMessage = cleanMessage(message);
        
        // Verificar se a mensagem contém diretamente alguma das palavras resposta
        for (String answer : termoAnswers) {
            if (containsWord(cleanedMessage, answer)) {
                return false;
            }
        }
        
        // Verificar por tentativas comuns de ofuscação
        return !containsObfuscatedTermoAnswer(cleanedMessage);
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
