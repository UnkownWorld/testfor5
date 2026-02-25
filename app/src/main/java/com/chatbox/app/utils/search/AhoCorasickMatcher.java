package com.chatbox.app.utils.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Aho-Corasick算法实现
 * 
 * 用于高效多模式字符串匹配，特别适合同时搜索多个关键词的场景。
 * 时间复杂度为O(n + m + z)，其中n是文本长度，m是所有模式串总长度，z是匹配数量。
 * 
 * 线程安全：此类不是线程安全的，请在单线程中使用或使用同步机制。
 */
public class AhoCorasickMatcher {
    
    /**
     * AC自动机节点
     */
    private static class Node {
        // 子节点映射
        Map<Character, Node> children = new HashMap<>();
        // 失败指针
        Node failure;
        // 匹配的关键词（可能有多个关键词在此节点结束）
        List<String> outputs = new ArrayList<>();
        // 是否是某个关键词的结束节点
        boolean isEnd;
    }
    
    private Node root;
    private boolean caseSensitive;
    private boolean built;
    
    /**
     * 构造函数
     * @param caseSensitive 是否大小写敏感
     */
    public AhoCorasickMatcher(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
        this.root = new Node();
        this.built = false;
    }
    
    /**
     * 默认构造函数（大小写敏感）
     */
    public AhoCorasickMatcher() {
        this(true);
    }
    
    /**
     * 添加关键词
     * @param keyword 关键词
     */
    public void addKeyword(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return;
        }
        
        Node current = root;
        String processedKeyword = caseSensitive ? keyword : keyword.toLowerCase();
        
        for (char c : processedKeyword.toCharArray()) {
            current.children.computeIfAbsent(c, k -> new Node());
            current = current.children.get(c);
        }
        
        current.isEnd = true;
        if (!current.outputs.contains(keyword)) {
            current.outputs.add(keyword);
        }
        
        built = false;
    }
    
    /**
     * 批量添加关键词
     * @param keywords 关键词列表
     */
    public void addKeywords(List<String> keywords) {
        if (keywords == null) {
            return;
        }
        for (String keyword : keywords) {
            addKeyword(keyword);
        }
    }
    
    /**
     * 构建失败指针（构建AC自动机）
     * 必须在添加完所有关键词后调用
     */
    public void build() {
        if (built) {
            return;
        }
        
        Queue<Node> queue = new LinkedList<>();
        
        // 第一层节点的失败指针指向root
        for (Node child : root.children.values()) {
            child.failure = root;
            queue.offer(child);
        }
        
        // BFS构建失败指针
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            
            for (Map.Entry<Character, Node> entry : current.children.entrySet()) {
                char c = entry.getKey();
                Node child = entry.getValue();
                
                // 沿着失败指针回溯，找到能匹配当前字符的节点
                Node fail = current.failure;
                while (fail != null && !fail.children.containsKey(c)) {
                    fail = fail.failure;
                }
                
                if (fail == null) {
                    child.failure = root;
                } else {
                    child.failure = fail.children.get(c);
                    // 继承失败节点的输出
                    child.outputs.addAll(child.failure.outputs);
                }
                
                queue.offer(child);
            }
        }
        
        built = true;
    }
    
    /**
     * 在文本中搜索所有匹配
     * @param text 待搜索文本
     * @return 匹配结果列表
     */
    public List<Match> search(String text) {
        if (!built) {
            build();
        }
        
        List<Match> matches = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return matches;
        }
        
        String processedText = caseSensitive ? text : text.toLowerCase();
        Node current = root;
        
        for (int i = 0; i < processedText.length(); i++) {
            char c = processedText.charAt(i);
            
            // 沿着失败指针回溯，直到找到能匹配当前字符的节点或到达root
            while (current != root && !current.children.containsKey(c)) {
                current = current.failure;
            }
            
            if (current.children.containsKey(c)) {
                current = current.children.get(c);
                
                // 收集所有匹配
                for (String keyword : current.outputs) {
                    int start = i - keyword.length() + 1;
                    matches.add(new Match(keyword, start, i + 1));
                }
            }
        }
        
        return matches;
    }
    
    /**
     * 检查文本是否包含任何关键词
     * @param text 待检查文本
     * @return 是否包含关键词
     */
    public boolean containsAny(String text) {
        if (!built) {
            build();
        }
        
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        String processedText = caseSensitive ? text : text.toLowerCase();
        Node current = root;
        
        for (int i = 0; i < processedText.length(); i++) {
            char c = processedText.charAt(i);
            
            while (current != root && !current.children.containsKey(c)) {
                current = current.failure;
            }
            
            if (current.children.containsKey(c)) {
                current = current.children.get(c);
                if (!current.outputs.isEmpty()) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 清空所有关键词
     */
    public void clear() {
        root = new Node();
        built = false;
    }
    
    /**
     * 匹配结果类
     */
    public static class Match {
        private final String keyword;
        private final int startIndex;
        private final int endIndex;
        
        public Match(String keyword, int startIndex, int endIndex) {
            this.keyword = keyword;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
        
        public String getKeyword() {
            return keyword;
        }
        
        public int getStartIndex() {
            return startIndex;
        }
        
        public int getEndIndex() {
            return endIndex;
        }
        
        @Override
        public String toString() {
            return "Match{" +
                    "keyword='" + keyword + '\'' +
                    ", startIndex=" + startIndex +
                    ", endIndex=" + endIndex +
                    '}';
        }
    }
}
