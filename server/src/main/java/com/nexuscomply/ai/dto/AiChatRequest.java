package com.nexuscomply.ai.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO for interactive multi-turn Gemini chat endpoint (POST /api/cyber/ai/chat).
 */
public class AiChatRequest {

    private String userMessage;
    private Map<String, Object> context;
    private List<Map<String, String>> history;

    public AiChatRequest() {}

    public AiChatRequest(String userMessage, Map<String, Object> context) {
        this.userMessage = userMessage;
        this.context = context;
    }

    public String getUserMessage() {
        if (userMessage != null && !userMessage.isBlank()) {
            return userMessage;
        }
        return "";
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public void setMessage(String message) {
        if (this.userMessage == null || this.userMessage.isBlank()) {
            this.userMessage = message;
        }
    }

    public void setQuery(String query) {
        if (this.userMessage == null || this.userMessage.isBlank()) {
            this.userMessage = query;
        }
    }

    public void setPrompt(String prompt) {
        if (this.userMessage == null || this.userMessage.isBlank()) {
            this.userMessage = prompt;
        }
    }

    public void setQuestion(String question) {
        if (this.userMessage == null || this.userMessage.isBlank()) {
            this.userMessage = question;
        }
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public void setContext(Map<String, Object> context) {
        this.context = context;
    }

    public List<Map<String, String>> getHistory() {
        return history;
    }

    public void setHistory(List<Map<String, String>> history) {
        this.history = history;
    }
}
