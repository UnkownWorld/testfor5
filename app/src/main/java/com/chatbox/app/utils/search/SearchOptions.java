package com.chatbox.app.utils.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 搜索选项配置类
 * 
 * 用于配置关键词搜索的各种参数和选项。
 */
public class SearchOptions implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // 关键词列表
    private List<String> keywords;
    
    // 是否整词匹配
    private boolean wholeWord;
    
    // 是否大小写敏感
    private boolean caseSensitive;
    
    // 是否使用正则表达式
    private boolean useRegex;
    
    // 是否递归搜索子文件夹
    private boolean recursiveSearch;
    
    // 上下文句子数（显示匹配句子的前后多少句）
    private int contextSentences;
    
    // 支持的文件扩展名
    private List<String> fileExtensions;
    
    // 是否并发搜索
    private boolean concurrentSearch;
    
    // 最大并发线程数
    private int maxThreads;
    
    // 是否高亮关键词
    private boolean highlightKeywords;
    
    /**
     * 默认构造函数
     */
    public SearchOptions() {
        this.keywords = new ArrayList<>();
        this.wholeWord = false;
        this.caseSensitive = false;
        this.useRegex = false;
        this.recursiveSearch = true;
        this.contextSentences = 1;
        this.fileExtensions = getDefaultExtensions();
        this.concurrentSearch = true;
        this.maxThreads = 4;
        this.highlightKeywords = true;
    }
    
    /**
     * 获取默认支持的文件扩展名
     */
    private List<String> getDefaultExtensions() {
        List<String> extensions = new ArrayList<>();
        extensions.add("txt");
        extensions.add("md");
        extensions.add("csv");
        extensions.add("json");
        extensions.add("xml");
        extensions.add("html");
        extensions.add("htm");
        extensions.add("py");
        extensions.add("java");
        extensions.add("js");
        extensions.add("ts");
        extensions.add("c");
        extensions.add("cpp");
        extensions.add("h");
        extensions.add("hpp");
        extensions.add("cs");
        extensions.add("go");
        extensions.add("rs");
        extensions.add("rb");
        extensions.add("php");
        extensions.add("swift");
        extensions.add("scala");
        extensions.add("sh");
        extensions.add("bat");
        extensions.add("kt");
        extensions.add("properties");
        extensions.add("yml");
        extensions.add("yaml");
        extensions.add("log");
        return extensions;
    }
    
    // Getters and Setters
    
    public List<String> getKeywords() {
        return keywords;
    }
    
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords != null ? keywords : new ArrayList<>();
    }
    
    public boolean isWholeWord() {
        return wholeWord;
    }
    
    public void setWholeWord(boolean wholeWord) {
        this.wholeWord = wholeWord;
    }
    
    public boolean isCaseSensitive() {
        return caseSensitive;
    }
    
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
    
    public boolean isUseRegex() {
        return useRegex;
    }
    
    public void setUseRegex(boolean useRegex) {
        this.useRegex = useRegex;
    }
    
    public boolean isRecursiveSearch() {
        return recursiveSearch;
    }
    
    public void setRecursiveSearch(boolean recursiveSearch) {
        this.recursiveSearch = recursiveSearch;
    }
    
    public int getContextSentences() {
        return contextSentences;
    }
    
    public void setContextSentences(int contextSentences) {
        this.contextSentences = Math.max(0, contextSentences);
    }
    
    public List<String> getFileExtensions() {
        return fileExtensions;
    }
    
    public void setFileExtensions(List<String> fileExtensions) {
        this.fileExtensions = fileExtensions != null ? fileExtensions : new ArrayList<>();
    }
    
    public boolean isConcurrentSearch() {
        return concurrentSearch;
    }
    
    public void setConcurrentSearch(boolean concurrentSearch) {
        this.concurrentSearch = concurrentSearch;
    }
    
    public int getMaxThreads() {
        return maxThreads;
    }
    
    public void setMaxThreads(int maxThreads) {
        this.maxThreads = Math.max(1, Math.min(maxThreads, 16));
    }
    
    public boolean isHighlightKeywords() {
        return highlightKeywords;
    }
    
    public void setHighlightKeywords(boolean highlightKeywords) {
        this.highlightKeywords = highlightKeywords;
    }
    
    /**
     * 检查文件扩展名是否支持
     * @param extension 文件扩展名
     * @return 是否支持
     */
    public boolean isExtensionSupported(String extension) {
        if (extension == null || extension.isEmpty()) {
            return false;
        }
        String ext = extension.toLowerCase().replace(".", "");
        return fileExtensions.contains(ext);
    }
    
    /**
     * 创建副本
     */
    public SearchOptions copy() {
        SearchOptions copy = new SearchOptions();
        copy.setKeywords(new ArrayList<>(this.keywords));
        copy.setWholeWord(this.wholeWord);
        copy.setCaseSensitive(this.caseSensitive);
        copy.setUseRegex(this.useRegex);
        copy.setRecursiveSearch(this.recursiveSearch);
        copy.setContextSentences(this.contextSentences);
        copy.setFileExtensions(new ArrayList<>(this.fileExtensions));
        copy.setConcurrentSearch(this.concurrentSearch);
        copy.setMaxThreads(this.maxThreads);
        copy.setHighlightKeywords(this.highlightKeywords);
        return copy;
    }
    
    @Override
    public String toString() {
        return "SearchOptions{" +
                "keywords=" + keywords +
                ", wholeWord=" + wholeWord +
                ", caseSensitive=" + caseSensitive +
                ", useRegex=" + useRegex +
                ", recursiveSearch=" + recursiveSearch +
                ", contextSentences=" + contextSentences +
                ", concurrentSearch=" + concurrentSearch +
                ", maxThreads=" + maxThreads +
                '}';
    }
}
