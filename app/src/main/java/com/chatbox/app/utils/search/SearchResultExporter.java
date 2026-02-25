package com.chatbox.app.utils.search;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 搜索结果导出器
 * 
 * 负责将搜索结果导出为文件，统一使用系统编码，不改变原内容。
 */
public class SearchResultExporter {
    
    private static final String TAG = "SearchResultExporter";
    
    // 导出格式
    public static final int FORMAT_TXT = 0;
    public static final int FORMAT_CSV = 1;
    public static final int FORMAT_MD = 2;
    
    private final Context context;
    private final SimpleDateFormat dateFormat;
    
    public SearchResultExporter(Context context) {
        this.context = context;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
    }
    
    /**
     * 导出搜索结果
     * @param results 搜索结果列表
     * @param format 导出格式
     * @param customPath 自定义路径（null则使用默认下载目录）
     * @return 导出文件路径
     */
    public String exportResults(List<SearchResult> results, int format, String customPath) {
        if (results == null || results.isEmpty()) {
            return null;
        }
        
        // 获取系统默认编码
        Charset charset = getSystemCharset();
        
        // 生成文件名
        String timestamp = dateFormat.format(new Date());
        String extension = getExtension(format);
        String fileName = "search_result_" + timestamp + extension;
        
        // 确定输出路径
        File outputFile;
        if (customPath != null && !customPath.isEmpty()) {
            outputFile = new File(customPath, fileName);
        } else {
            outputFile = new File(getDownloadDirectory(), fileName);
        }
        
        // 确保父目录存在
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        // 导出内容
        String content = generateContent(results, format);
        
        // 写入文件（使用系统编码）
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             OutputStreamWriter osw = new OutputStreamWriter(fos, charset);
             BufferedWriter writer = new BufferedWriter(osw)) {
            
            writer.write(content);
            writer.flush();
            
            Log.i(TAG, "Exported " + results.size() + " results to: " + outputFile.getAbsolutePath());
            return outputFile.getAbsolutePath();
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to export results", e);
            return null;
        }
    }
    
    /**
     * 导出为默认格式（TXT）
     */
    public String exportResults(List<SearchResult> results) {
        return exportResults(results, FORMAT_TXT, null);
    }
    
    /**
     * 导出到下载目录
     */
    public String exportToDownloads(List<SearchResult> results, int format) {
        return exportResults(results, format, null);
    }
    
    /**
     * 导出到自定义目录
     */
    public String exportToCustomPath(List<SearchResult> results, int format, String directoryPath) {
        return exportResults(results, format, directoryPath);
    }
    
    /**
     * 生成导出内容
     */
    private String generateContent(List<SearchResult> results, int format) {
        switch (format) {
            case FORMAT_CSV:
                return generateCsvContent(results);
            case FORMAT_MD:
                return generateMarkdownContent(results);
            case FORMAT_TXT:
            default:
                return generateTxtContent(results);
        }
    }
    
    /**
     * 生成TXT格式内容
     * 保持原内容不变，不添加高亮或注释
     */
    private String generateTxtContent(List<SearchResult> results) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("========================================\n");
        sb.append("关键词检索结果\n");
        sb.append("导出时间: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
        sb.append("匹配总数: ").append(results.size()).append("\n");
        sb.append("========================================\n\n");
        
        int index = 1;
        for (SearchResult result : results) {
            sb.append("【").append(index++).append("】\n");
            sb.append("来源: ").append(result.getFilePath());
            sb.append(" (第").append(result.getLineNumber()).append("行)\n");
            sb.append("匹配关键词: ").append(joinKeywords(result.getMatchedKeywords())).append("\n");
            sb.append("----------\n");
            
            // 上文
            if (!result.getContextBefore().isEmpty()) {
                for (String ctx : result.getContextBefore()) {
                    sb.append(ctx).append("\n");
                }
            }
            
            // 匹配句子（使用原始句子，无高亮）
            sb.append(result.getOriginalSentence()).append("\n");
            
            // 下文
            if (!result.getContextAfter().isEmpty()) {
                for (String ctx : result.getContextAfter()) {
                    sb.append(ctx).append("\n");
                }
            }
            
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 生成CSV格式内容
     */
    private String generateCsvContent(List<SearchResult> results) {
        StringBuilder sb = new StringBuilder();
        
        // CSV头部
        sb.append("序号,文件路径,文件名,行号,匹配关键词,上文,匹配句子,下文\n");
        
        int index = 1;
        for (SearchResult result : results) {
            sb.append(index++).append(",");
            sb.append(escapeCsvField(result.getFilePath())).append(",");
            sb.append(escapeCsvField(result.getFileName())).append(",");
            sb.append(result.getLineNumber()).append(",");
            sb.append(escapeCsvField(joinKeywords(result.getMatchedKeywords()))).append(",");
            sb.append(escapeCsvField(joinSentences(result.getContextBefore()))).append(",");
            sb.append(escapeCsvField(result.getOriginalSentence())).append(",");
            sb.append(escapeCsvField(joinSentences(result.getContextAfter()))).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 生成Markdown格式内容
     */
    private String generateMarkdownContent(List<SearchResult> results) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("# 关键词检索结果\n\n");
        sb.append("**导出时间**: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n\n");
        sb.append("**匹配总数**: ").append(results.size()).append("\n\n");
        sb.append("---\n\n");
        
        int index = 1;
        for (SearchResult result : results) {
            sb.append("## ").append(index++).append(". ").append(result.getFileName()).append("\n\n");
            sb.append("- **文件路径**: `").append(result.getFilePath()).append("`\n");
            sb.append("- **行号**: ").append(result.getLineNumber()).append("\n");
            sb.append("- **匹配关键词**: ").append(joinKeywords(result.getMatchedKeywords())).append("\n\n");
            
            // 上文
            if (!result.getContextBefore().isEmpty()) {
                sb.append("**上文**:\n```\n");
                for (String ctx : result.getContextBefore()) {
                    sb.append(ctx).append("\n");
                }
                sb.append("```\n\n");
            }
            
            // 匹配句子
            sb.append("**匹配句子**:\n```\n");
            sb.append(result.getOriginalSentence()).append("\n");
            sb.append("```\n\n");
            
            // 下文
            if (!result.getContextAfter().isEmpty()) {
                sb.append("**下文**:\n```\n");
                for (String ctx : result.getContextAfter()) {
                    sb.append(ctx).append("\n");
                }
                sb.append("```\n\n");
            }
            
            sb.append("---\n\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 获取系统默认编码
     */
    private Charset getSystemCharset() {
        try {
            String encoding = System.getProperty("file.encoding", "UTF-8");
            return Charset.forName(encoding);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getExtension(int format) {
        switch (format) {
            case FORMAT_CSV:
                return ".csv";
            case FORMAT_MD:
                return ".md";
            case FORMAT_TXT:
            default:
                return ".txt";
        }
    }
    
    /**
     * 获取下载目录
     */
    private File getDownloadDirectory() {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloadDir == null || !downloadDir.exists()) {
            downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        }
        if (downloadDir == null) {
            downloadDir = context.getFilesDir();
        }
        return downloadDir;
    }
    
    /**
     * 连接关键词
     */
    private String joinKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keywords.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(keywords.get(i));
        }
        return sb.toString();
    }
    
    /**
     * 连接句子
     */
    private String joinSentences(List<String> sentences) {
        if (sentences == null || sentences.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sentences.size(); i++) {
            if (i > 0) sb.append(" ");
            sb.append(sentences.get(i));
        }
        return sb.toString();
    }
    
    /**
     * CSV字段转义
     */
    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        // 如果包含逗号、引号或换行符，需要用引号包围并转义内部引号
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
}
