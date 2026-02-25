package com.chatbox.app.utils.search;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 搜索历史管理器
 * 
 * 管理用户的搜索历史记录，包括关键词历史和搜索选项历史。
 * 使用SharedPreferences进行本地持久化存储。
 */
public class SearchHistoryManager {
    
    private static final String PREFS_NAME = "search_history_prefs";
    private static final String KEY_KEYWORDS_HISTORY = "keywords_history";
    private static final String KEY_LAST_OPTIONS = "last_options";
    private static final String KEY_SAVED_OPTIONS = "saved_options";
    private static final int MAX_HISTORY_SIZE = 50;
    
    private final SharedPreferences prefs;
    
    /**
     * 构造函数
     * @param context 上下文
     */
    public SearchHistoryManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * 添加关键词到历史记录
     * @param keyword 关键词
     */
    public void addKeywordToHistory(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        
        keyword = keyword.trim();
        List<KeywordHistoryItem> history = getKeywordsHistory();
        
        // 检查是否已存在
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).keyword.equals(keyword)) {
                // 更新使用次数和时间
                KeywordHistoryItem item = history.get(i);
                item.useCount++;
                item.lastUsedTime = System.currentTimeMillis();
                // 移到列表开头
                history.remove(i);
                history.add(0, item);
                saveKeywordsHistory(history);
                return;
            }
        }
        
        // 添加新项
        KeywordHistoryItem newItem = new KeywordHistoryItem();
        newItem.keyword = keyword;
        newItem.useCount = 1;
        newItem.lastUsedTime = System.currentTimeMillis();
        history.add(0, newItem);
        
        // 限制历史记录大小
        while (history.size() > MAX_HISTORY_SIZE) {
            history.remove(history.size() - 1);
        }
        
        saveKeywordsHistory(history);
    }
    
    /**
     * 批量添加关键词到历史记录
     * @param keywords 关键词列表
     */
    public void addKeywordsToHistory(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return;
        }
        for (String keyword : keywords) {
            addKeywordToHistory(keyword);
        }
    }
    
    /**
     * 获取关键词历史记录
     * @return 关键词历史列表
     */
    public List<KeywordHistoryItem> getKeywordsHistory() {
        List<KeywordHistoryItem> history = new ArrayList<>();
        String json = prefs.getString(KEY_KEYWORDS_HISTORY, "[]");
        
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                KeywordHistoryItem item = new KeywordHistoryItem();
                item.keyword = obj.optString("keyword", "");
                item.useCount = obj.optInt("useCount", 1);
                item.lastUsedTime = obj.optLong("lastUsedTime", 0);
                if (!item.keyword.isEmpty()) {
                    history.add(item);
                }
            }
        } catch (JSONException e) {
            // 解析失败，返回空列表
        }
        
        return history;
    }
    
    /**
     * 获取关键词历史记录（仅关键词字符串）
     * @return 关键词字符串列表
     */
    public List<String> getKeywordsHistoryStrings() {
        List<KeywordHistoryItem> history = getKeywordsHistory();
        List<String> keywords = new ArrayList<>();
        for (KeywordHistoryItem item : history) {
            keywords.add(item.keyword);
        }
        return keywords;
    }
    
    /**
     * 保存关键词历史记录
     * @param history 历史记录列表
     */
    private void saveKeywordsHistory(List<KeywordHistoryItem> history) {
        try {
            JSONArray array = new JSONArray();
            for (KeywordHistoryItem item : history) {
                JSONObject obj = new JSONObject();
                obj.put("keyword", item.keyword);
                obj.put("useCount", item.useCount);
                obj.put("lastUsedTime", item.lastUsedTime);
                array.put(obj);
            }
            prefs.edit().putString(KEY_KEYWORDS_HISTORY, array.toString()).apply();
        } catch (JSONException e) {
            // 保存失败
        }
    }
    
    /**
     * 清空关键词历史记录
     */
    public void clearKeywordsHistory() {
        prefs.edit().remove(KEY_KEYWORDS_HISTORY).apply();
    }
    
    /**
     * 从历史记录中删除指定关键词
     * @param keyword 关键词
     */
    public void removeKeywordFromHistory(String keyword) {
        List<KeywordHistoryItem> history = getKeywordsHistory();
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).keyword.equals(keyword)) {
                history.remove(i);
                break;
            }
        }
        saveKeywordsHistory(history);
    }
    
    /**
     * 保存搜索选项
     * @param options 搜索选项
     */
    public void saveLastOptions(SearchOptions options) {
        try {
            JSONObject obj = optionsToJson(options);
            prefs.edit().putString(KEY_LAST_OPTIONS, obj.toString()).apply();
        } catch (JSONException e) {
            // 保存失败
        }
    }
    
    /**
     * 获取上次使用的搜索选项
     * @return 搜索选项
     */
    public SearchOptions getLastOptions() {
        String json = prefs.getString(KEY_LAST_OPTIONS, "");
        if (json.isEmpty()) {
            return new SearchOptions();
        }
        
        try {
            return jsonToOptions(new JSONObject(json));
        } catch (JSONException e) {
            return new SearchOptions();
        }
    }
    
    /**
     * 保存命名选项配置
     * @param name 配置名称
     * @param options 搜索选项
     */
    public void saveNamedOptions(String name, SearchOptions options) {
        try {
            String savedJson = prefs.getString(KEY_SAVED_OPTIONS, "{}");
            JSONObject saved = new JSONObject(savedJson);
            JSONObject optionJson = optionsToJson(options);
            optionJson.put("name", name);
            optionJson.put("savedTime", System.currentTimeMillis());
            saved.put(name, optionJson);
            prefs.edit().putString(KEY_SAVED_OPTIONS, saved.toString()).apply();
        } catch (JSONException e) {
            // 保存失败
        }
    }
    
    /**
     * 获取所有保存的选项配置名称
     * @return 配置名称列表
     */
    public List<String> getSavedOptionsNames() {
        List<String> names = new ArrayList<>();
        String json = prefs.getString(KEY_SAVED_OPTIONS, "{}");
        
        try {
            JSONObject saved = new JSONObject(json);
            JSONArray keys = saved.names();
            if (keys != null) {
                for (int i = 0; i < keys.length(); i++) {
                    names.add(keys.getString(i));
                }
            }
        } catch (JSONException e) {
            // 解析失败
        }
        
        return names;
    }
    
    /**
     * 获取保存的选项配置
     * @param name 配置名称
     * @return 搜索选项
     */
    public SearchOptions getSavedOptions(String name) {
        String json = prefs.getString(KEY_SAVED_OPTIONS, "{}");
        
        try {
            JSONObject saved = new JSONObject(json);
            if (saved.has(name)) {
                return jsonToOptions(saved.getJSONObject(name));
            }
        } catch (JSONException e) {
            // 解析失败
        }
        
        return new SearchOptions();
    }
    
    /**
     * 删除保存的选项配置
     * @param name 配置名称
     */
    public void deleteSavedOptions(String name) {
        try {
            String json = prefs.getString(KEY_SAVED_OPTIONS, "{}");
            JSONObject saved = new JSONObject(json);
            saved.remove(name);
            prefs.edit().putString(KEY_SAVED_OPTIONS, saved.toString()).apply();
        } catch (JSONException e) {
            // 删除失败
        }
    }
    
    /**
     * 将搜索选项转换为JSON
     */
    private JSONObject optionsToJson(SearchOptions options) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("wholeWord", options.isWholeWord());
        obj.put("caseSensitive", options.isCaseSensitive());
        obj.put("useRegex", options.isUseRegex());
        obj.put("recursiveSearch", options.isRecursiveSearch());
        obj.put("contextSentences", options.getContextSentences());
        obj.put("concurrentSearch", options.isConcurrentSearch());
        obj.put("maxThreads", options.getMaxThreads());
        obj.put("highlightKeywords", options.isHighlightKeywords());
        
        JSONArray keywords = new JSONArray();
        for (String kw : options.getKeywords()) {
            keywords.put(kw);
        }
        obj.put("keywords", keywords);
        
        JSONArray extensions = new JSONArray();
        for (String ext : options.getFileExtensions()) {
            extensions.put(ext);
        }
        obj.put("fileExtensions", extensions);
        
        return obj;
    }
    
    /**
     * 从JSON解析搜索选项
     */
    private SearchOptions jsonToOptions(JSONObject obj) throws JSONException {
        SearchOptions options = new SearchOptions();
        options.setWholeWord(obj.optBoolean("wholeWord", false));
        options.setCaseSensitive(obj.optBoolean("caseSensitive", false));
        options.setUseRegex(obj.optBoolean("useRegex", false));
        options.setRecursiveSearch(obj.optBoolean("recursiveSearch", true));
        options.setContextSentences(obj.optInt("contextSentences", 1));
        options.setConcurrentSearch(obj.optBoolean("concurrentSearch", true));
        options.setMaxThreads(obj.optInt("maxThreads", 4));
        options.setHighlightKeywords(obj.optBoolean("highlightKeywords", true));
        
        List<String> keywords = new ArrayList<>();
        JSONArray keywordsArray = obj.optJSONArray("keywords");
        if (keywordsArray != null) {
            for (int i = 0; i < keywordsArray.length(); i++) {
                keywords.add(keywordsArray.getString(i));
            }
        }
        options.setKeywords(keywords);
        
        List<String> extensions = new ArrayList<>();
        JSONArray extensionsArray = obj.optJSONArray("fileExtensions");
        if (extensionsArray != null) {
            for (int i = 0; i < extensionsArray.length(); i++) {
                extensions.add(extensionsArray.getString(i));
            }
        }
        if (!extensions.isEmpty()) {
            options.setFileExtensions(extensions);
        }
        
        return options;
    }
    
    /**
     * 关键词历史项
     */
    public static class KeywordHistoryItem {
        public String keyword;
        public int useCount;
        public long lastUsedTime;
        
        @Override
        public String toString() {
            return keyword;
        }
    }
}
