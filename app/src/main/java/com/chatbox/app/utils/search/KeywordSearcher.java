package com.chatbox.app.utils.search;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 关键词检索器
 * 
 * 支持并发检索多个文件中的关键词，使用Aho-Corasick算法进行高效匹配。
 */
public class KeywordSearcher {
    
    private final AhoCorasickMatcher matcher;
    private final SearchOptions options;
    private final List<SearchResult> results;
    private final AtomicBoolean cancelled;
    private final AtomicInteger processedCount;
    private ExecutorService executor;
    private Future<?> searchFuture;
    
    public KeywordSearcher(SearchOptions options) {
        this.options = options != null ? options : new SearchOptions();
        this.matcher = new AhoCorasickMatcher(this.options.isCaseSensitive());
        this.results = new CopyOnWriteArrayList<>();
        this.cancelled = new AtomicBoolean(false);
        this.processedCount = new AtomicInteger(0);
    }
    
    /**
     * 设置关键词
     */
    public void setKeywords(List<String> keywords) {
        matcher.clear();
        matcher.addKeywords(keywords);
        matcher.build();
    }
    
    /**
     * 搜索文件
     */
    public void searchFiles(List<File> files, SearchCallback callback) {
        if (files == null || files.isEmpty()) {
            if (callback != null) {
                callback.onComplete(new ArrayList<>());
            }
            return;
        }
        
        cancelled.set(false);
        results.clear();
        processedCount.set(0);
        
        if (options.isConcurrentSearch()) {
            searchConcurrent(files, callback);
        } else {
            searchSequential(files, callback);
        }
    }
    
    /**
     * 并发搜索
     */
    private void searchConcurrent(List<File> files, SearchCallback callback) {
        executor = Executors.newFixedThreadPool(options.getMaxThreads());
        
        searchFuture = executor.submit(() -> {
            long startTime = System.currentTimeMillis();
            
            List<Future<?>> futures = new ArrayList<>();
            for (File file : files) {
                if (cancelled.get()) break;
                
                futures.add(executor.submit(() -> {
                    searchFile(file);
                }));
            }
            
            // 等待所有任务完成
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    // 忽略
                }
            }
            
            long searchTime = System.currentTimeMillis() - startTime;
            
            if (callback != null && !cancelled.get()) {
                callback.onComplete(new ArrayList<>(results));
                callback.onStatistics(processedCount.get(), results.size(), searchTime);
            }
        });
        
        executor.shutdown();
    }
    
    /**
     * 顺序搜索
     */
    private void searchSequential(List<File> files, SearchCallback callback) {
        long startTime = System.currentTimeMillis();
        
        for (File file : files) {
            if (cancelled.get()) break;
            searchFile(file);
        }
        
        long searchTime = System.currentTimeMillis() - startTime;
        
        if (callback != null && !cancelled.get()) {
            callback.onComplete(new ArrayList<>(results));
            callback.onStatistics(processedCount.get(), results.size(), searchTime);
        }
    }
    
    /**
     * 搜索单个文件
     */
    private void searchFile(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return;
        }
        
        // 检查文件扩展名
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {
            String ext = name.substring(dotIndex + 1).toLowerCase();
            if (!options.isExtensionSupported(ext)) {
                return;
            }
        }
        
        try {
            // 读取文件内容（自动检测编码）
            String content = EncodingDetector.readFileWithAutoEncoding(file);
            if (content == null || content.isEmpty()) {
                return;
            }
            
            // 搜索关键词
            List<AhoCorasickMatcher.Match> matches = matcher.search(content);
            
            if (!matches.isEmpty()) {
                // 按句子分割内容
                String[] sentences = content.split("[。！？\\n]");
                
                for (AhoCorasickMatcher.Match match : matches) {
                    if (cancelled.get()) return;
                    
                    SearchResult result = createSearchResult(file, content, match, sentences);
                    results.add(result);
                }
            }
            
            processedCount.incrementAndGet();
            
        } catch (Exception e) {
            // 忽略错误
        }
    }
    
    /**
     * 创建搜索结果
     */
    private SearchResult createSearchResult(File file, String content, 
                                           AhoCorasickMatcher.Match match, String[] sentences) {
        SearchResult result = new SearchResult();
        result.setFilePath(file.getAbsolutePath());
        result.setFileName(file.getName());
        result.addMatchedKeyword(match.getKeyword());
        
        // 找到匹配位置所在的句子
        int matchStart = match.getStartIndex();
        int currentPos = 0;
        int sentenceIndex = 0;
        
        for (int i = 0; i < sentences.length; i++) {
            int sentenceEnd = currentPos + sentences[i].length();
            if (matchStart >= currentPos && matchStart < sentenceEnd) {
                sentenceIndex = i;
                result.setMatchedSentence(sentences[i]);
                result.setOriginalSentence(sentences[i]);
                result.setLineNumber(i + 1);
                break;
            }
            currentPos = sentenceEnd + 1;
        }
        
        // 添加上下文
        int contextCount = options.getContextSentences();
        for (int i = Math.max(0, sentenceIndex - contextCount); i < sentenceIndex; i++) {
            result.addContextBefore(sentences[i]);
        }
        for (int i = sentenceIndex + 1; i < Math.min(sentences.length, sentenceIndex + contextCount + 1); i++) {
            result.addContextAfter(sentences[i]);
        }
        
        return result;
    }
    
    /**
     * 取消搜索
     */
    public void cancel() {
        cancelled.set(true);
        if (searchFuture != null) {
            searchFuture.cancel(true);
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }
    
    /**
     * 检查是否已取消
     */
    public boolean isCancelled() {
        return cancelled.get();
    }
    
    /**
     * 搜索回调接口
     */
    public interface SearchCallback {
        void onComplete(List<SearchResult> results);
        void onStatistics(int processedFiles, int totalMatches, long searchTime);
    }
}
