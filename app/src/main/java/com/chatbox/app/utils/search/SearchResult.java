package com.chatbox.app.utils.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果类
 */
public class SearchResult implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private String filePath;
    private String fileName;
    private String matchedSentence;
    private String originalSentence;
    private List<String> contextBefore = new ArrayList<>();
    private List<String> contextAfter = new ArrayList<>();
    private List<String> matchedKeywords = new ArrayList<>();
    private int lineNumber;
    
    public SearchResult() {}
    
    // Getters and Setters
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    
    public String getMatchedSentence() { return matchedSentence; }
    public void setMatchedSentence(String matchedSentence) { this.matchedSentence = matchedSentence; }
    
    public String getOriginalSentence() { return originalSentence; }
    public void setOriginalSentence(String originalSentence) { this.originalSentence = originalSentence; }
    
    public List<String> getContextBefore() { return contextBefore; }
    public void setContextBefore(List<String> contextBefore) { this.contextBefore = contextBefore; }
    public void addContextBefore(String s) { if (s != null) contextBefore.add(s); }
    
    public List<String> getContextAfter() { return contextAfter; }
    public void setContextAfter(List<String> contextAfter) { this.contextAfter = contextAfter; }
    public void addContextAfter(String s) { if (s != null) contextAfter.add(s); }
    
    public List<String> getMatchedKeywords() { return matchedKeywords; }
    public void setMatchedKeywords(List<String> matchedKeywords) { this.matchedKeywords = matchedKeywords; }
    public void addMatchedKeyword(String keyword) { if (keyword != null && !matchedKeywords.contains(keyword)) matchedKeywords.add(keyword); }
    
    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    
    public String getFormattedSource() {
        return String.format("%s (第%d行)", fileName, lineNumber);
    }
    
    public String getFullContext() {
        StringBuilder sb = new StringBuilder();
        for (String ctx : contextBefore) sb.append(ctx).append("\n");
        sb.append(matchedSentence);
        for (String ctx : contextAfter) sb.append("\n").append(ctx);
        return sb.toString();
    }
    
    public String getPlainFullContext() {
        StringBuilder sb = new StringBuilder();
        for (String ctx : contextBefore) sb.append(ctx).append("\n");
        sb.append(originalSentence);
        for (String ctx : contextAfter) sb.append("\n").append(ctx);
        return sb.toString();
    }
}
