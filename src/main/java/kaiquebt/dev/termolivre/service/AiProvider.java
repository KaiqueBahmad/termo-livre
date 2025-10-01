package kaiquebt.dev.termolivre.service;

import java.util.List;

public interface AiProvider {
    List<Boolean> analyzeMessagesForTermoAnswers(List<String> messages);
}
