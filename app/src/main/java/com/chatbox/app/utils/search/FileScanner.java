package com.chatbox.app.utils.search;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 文件扫描器
 * 
 * 用于扫描目录中的文件，支持递归扫描和扩展名过滤。
 */
public class FileScanner {
    
    private final SearchOptions options;
    private final AtomicBoolean cancelled;
    
    public FileScanner(SearchOptions options) {
        this.options = options != null ? options : new SearchOptions();
        this.cancelled = new AtomicBoolean(false);
    }
    
    /**
     * 扫描文件或目录
     * @param path 文件或目录路径
     * @return 文件列表
     */
    public List<File> scan(String path) {
        cancelled.set(false);
        List<File> files = new ArrayList<>();
        
        if (path == null || path.isEmpty()) {
            return files;
        }
        
        File file = new File(path);
        if (!file.exists()) {
            return files;
        }
        
        if (file.isFile()) {
            if (isFileSupported(file)) {
                files.add(file);
            }
        } else if (file.isDirectory()) {
            scanDirectory(file, files);
        }
        
        return files;
    }
    
    /**
     * 扫描多个路径
     * @param paths 路径列表
     * @return 文件列表
     */
    public List<File> scanMultiple(List<String> paths) {
        cancelled.set(false);
        List<File> allFiles = new ArrayList<>();
        
        if (paths == null || paths.isEmpty()) {
            return allFiles;
        }
        
        for (String path : paths) {
            if (cancelled.get()) break;
            allFiles.addAll(scan(path));
        }
        
        return allFiles;
    }
    
    /**
     * 递归扫描目录
     */
    private void scanDirectory(File directory, List<File> result) {
        if (cancelled.get()) return;
        
        File[] children = directory.listFiles();
        if (children == null) return;
        
        for (File child : children) {
            if (cancelled.get()) break;
            
            if (child.isFile()) {
                if (isFileSupported(child)) {
                    result.add(child);
                }
            } else if (child.isDirectory() && options.isRecursiveSearch()) {
                scanDirectory(child, result);
            }
        }
    }
    
    /**
     * 检查文件是否支持
     */
    private boolean isFileSupported(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf('.');
        
        if (dotIndex <= 0 || dotIndex >= name.length() - 1) {
            return false;
        }
        
        String extension = name.substring(dotIndex + 1).toLowerCase();
        return options.isExtensionSupported(extension);
    }
    
    /**
     * 取消扫描
     */
    public void cancel() {
        cancelled.set(true);
    }
    
    /**
     * 是否已取消
     */
    public boolean isCancelled() {
        return cancelled.get();
    }
    
    /**
     * 获取目录统计信息
     */
    public ScanStatistics getStatistics(List<File> files) {
        ScanStatistics stats = new ScanStatistics();
        
        if (files == null) return stats;
        
        stats.totalFiles = files.size();
        
        for (File file : files) {
            stats.totalSize += file.length();
            
            String name = file.getName();
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex > 0) {
                String ext = name.substring(dotIndex + 1).toLowerCase();
                stats.extensionCount.merge(ext, 1, Integer::sum);
            }
        }
        
        return stats;
    }
    
    /**
     * 扫描统计信息
     */
    public static class ScanStatistics {
        public int totalFiles;
        public long totalSize;
        public java.util.Map<String, Integer> extensionCount = new java.util.HashMap<>();
        
        public String getFormattedSize() {
            if (totalSize < 1024) {
                return totalSize + " B";
            } else if (totalSize < 1024 * 1024) {
                return String.format("%.1f KB", totalSize / 1024.0);
            } else if (totalSize < 1024 * 1024 * 1024) {
                return String.format("%.1f MB", totalSize / (1024.0 * 1024));
            } else {
                return String.format("%.1f GB", totalSize / (1024.0 * 1024 * 1024));
            }
        }
    }
}
