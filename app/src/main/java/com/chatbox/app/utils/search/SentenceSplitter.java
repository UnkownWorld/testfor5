package com.chatbox.app.utils.search;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 句子分割器
 * 
 * 支持中英文标点符号分割句子。
 */
public class SentenceSplitter {
    
    // 中英文句子结束标点
    private static final Pattern SENTENCE_END_PATTERN = Pattern.compile(
        "[。！？!?.]+|" +                    // 中英文句号、感叹号、问号
        "[；;]+|" +                          // 中英文分号
        "\\n+|" +                            // 换行符
        "[\\r\\n]+"                          // 回车换行
    );
    
    // 句子分割正则（保留分隔符）
    private static final Pattern SPLIT_PATTERN = Pattern.compile(
        "([^。！？!?.\\n；;]+[。！？!?.\\n；;]*)|" +
        "([^。！？!?.\\n；;]+$)"
    );
    
    /**
     * 分割文本为句子列表
     * @param text 文本内容
     * @return 句子列表
     */
    public static List<String> split(String text) {
        List<String> sentences = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return sentences;
        }
        
        // 使用正则表达式分割
        Matcher matcher = SPLIT_PATTERN.matcher(text);
        while (matcher.find()) {
            String sentence = matcher.group();
            if (sentence != null && !sentence.trim().isEmpty()) {
                sentences.add(sentence.trim());
            }
        }
        
        // 如果正则没有匹配到，按换行符分割
        if (sentences.isEmpty()) {
            String[] lines = text.split("\\n");
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    sentences.add(line.trim());
                }
            }
        }
        
        return sentences;
    }
    
    /**
     * 分割文本并保留位置信息
     * @param text 文本内容
     * @return 带位置信息的句子列表
     */
    public static List<Sentence> splitWithPositions(String text) {
        List<Sentence> sentences = new ArrayList<>();
        
        if (text == null || text.isEmpty()) {
            return sentences;
        }
        
        int currentPos = 0;
        Matcher matcher = SPLIT_PATTERN.matcher(text);
        
        while (matcher.find()) {
            String content = matcher.group();
            if (content != null && !content.trim().isEmpty()) {
                Sentence sentence = new Sentence();
                sentence.content = content.trim();
                sentence.startIndex = matcher.start();
                sentence.endIndex = matcher.end();
                sentences.add(sentence);
            }
        }
        
        // 如果正则没有匹配到
        if (sentences.isEmpty()) {
            String[] lines = text.split("\\n");
            int pos = 0;
            for (String line : lines) {
                if (!line.trim().isEmpty()) {
                    Sentence sentence = new Sentence();
                    sentence.content = line.trim();
                    sentence.startIndex = pos;
                    sentence.endIndex = pos + line.length();
                    sentences.add(sentence);
                }
                pos += line.length() + 1; // +1 for \n
            }
        }
        
        return sentences;
    }
    
    /**
     * 根据字符位置找到所在句子
     * @param text 文本内容
     * @param charIndex 字符位置
     * @return 句子信息
     */
    public static Sentence findSentenceAt(String text, int charIndex) {
        List<Sentence> sentences = splitWithPositions(text);
        
        for (Sentence sentence : sentences) {
            if (charIndex >= sentence.startIndex && charIndex < sentence.endIndex) {
                return sentence;
            }
        }
        
        return null;
    }
    
    /**
     * 根据字符位置找到句子索引
     * @param text 文本内容
     * @param charIndex 字符位置
     * @return 句子索引（从0开始）
     */
    public static int findSentenceIndex(String text, int charIndex) {
        List<Sentence> sentences = splitWithPositions(text);
        
        for (int i = 0; i < sentences.size(); i++) {
            Sentence sentence = sentences.get(i);
            if (charIndex >= sentence.startIndex && charIndex < sentence.endIndex) {
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * 句子信息类
     */
    public static class Sentence {
        public String content;
        public int startIndex;
        public int endIndex;
        
        public int getLineNumber(String fullText) {
            if (fullText == null || startIndex < 0) {
                return 1;
            }
            String beforeContent = fullText.substring(0, startIndex);
            return beforeContent.split("\\n").length;
        }
    }
}
