package com.chatbox.app.ui.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.chatbox.app.R;
import com.chatbox.app.databinding.ActivitySearchBinding;
import com.chatbox.app.ui.adapters.SearchResultAdapter;
import com.chatbox.app.utils.search.FileScanner;
import com.chatbox.app.utils.search.KeywordSearcher;
import com.chatbox.app.utils.search.SearchHistoryManager;
import com.chatbox.app.utils.search.SearchOptions;
import com.chatbox.app.utils.search.SearchResult;
import com.chatbox.app.utils.search.SearchResultExporter;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 搜索Activity
 * 
 * 提供关键词检索功能，支持文件和文件夹搜索。
 */
public class SearchActivity extends AppCompatActivity implements SearchResultAdapter.OnResultClickListener {
    
    private static final String TAG = "SearchActivity";
    
    private ActivitySearchBinding binding;
    private SearchResultAdapter adapter;
    private SearchOptions options;
    private SearchHistoryManager historyManager;
    private KeywordSearcher searcher;
    private FileScanner scanner;
    private SearchResultExporter exporter;
    private ExecutorService executor;
    private Handler mainHandler;
    
    private List<String> selectedPaths = new ArrayList<>();
    private List<SearchResult> currentResults = new ArrayList<>();
    
    private ActivityResultLauncher<String[]> filePickerLauncher;
    private ActivityResultLauncher<Uri> folderPickerLauncher;
    private ActivityResultLauncher<String> exportFileLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        // 初始化
        historyManager = new SearchHistoryManager(this);
        options = historyManager.getLastOptions();
        scanner = new FileScanner(options);
        exporter = new SearchResultExporter(this);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        
        setupLaunchers();
        setupViews();
        setupHistory();
    }
    
    private void setupLaunchers() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        String path = uri.getPath();
                        if (path != null) {
                            selectedPaths.clear();
                            selectedPaths.add(path);
                            updateSelectedPathDisplay();
                        }
                    }
                });
        
        folderPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocumentTree(),
                uri -> {
                    if (uri != null) {
                        String path = uri.getPath();
                        if (path != null) {
                            // 转换URI路径为实际路径
                            if (path.contains(":")) {
                                String[] parts = path.split(":");
                                if (parts.length > 1) {
                                    path = "/storage/emulated/0/" + parts[1];
                                }
                            }
                            selectedPaths.clear();
                            selectedPaths.add(path);
                            updateSelectedPathDisplay();
                        }
                    }
                });
        
        exportFileLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("text/plain"),
                uri -> {
                    if (uri != null && !currentResults.isEmpty()) {
                        // 导出到指定位置
                        String content = generateExportContent();
                        try {
                            getContentResolver().openOutputStream(uri).close();
                            Toast.makeText(this, R.string.export_success, Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    
    private void setupViews() {
        // 设置RecyclerView
        adapter = new SearchResultAdapter();
        adapter.setOnResultClickListener(this);
        binding.recyclerResults.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerResults.setAdapter(adapter);
        
        // 选择文件按钮
        binding.btnSelectFile.setOnClickListener(v -> {
            filePickerLauncher.launch(new String[]{"text/*"});
        });
        
        // 选择文件夹按钮
        binding.btnSelectFolder.setOnClickListener(v -> {
            folderPickerLauncher.launch(null);
        });
        
        // 选项按钮
        binding.btnOptions.setOnClickListener(v -> showOptionsDialog());
        
        // 历史按钮
        binding.btnHistory.setOnClickListener(v -> showHistoryDialog());
        
        // 搜索按钮
        binding.btnSearch.setOnClickListener(v -> performSearch());
        
        // 输入框回车搜索
        binding.inputKeywords.setOnEditorActionListener((v, actionId, event) -> {
            performSearch();
            return true;
        });
    }
    
    private void setupHistory() {
        // 设置关键词自动完成
        List<String> historyKeywords = historyManager.getKeywordsHistoryStrings();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, historyKeywords);
        binding.inputKeywords.setAdapter(adapter);
    }
    
    private void updateSelectedPathDisplay() {
        if (!selectedPaths.isEmpty()) {
            binding.textSelectedPath.setVisibility(View.VISIBLE);
            binding.textSelectedPath.setText(selectedPaths.get(0));
        } else {
            binding.textSelectedPath.setVisibility(View.GONE);
        }
    }
    
    private void performSearch() {
        String keywordsInput = binding.inputKeywords.getText() != null ? 
                binding.inputKeywords.getText().toString().trim() : "";
        
        if (keywordsInput.isEmpty()) {
            Toast.makeText(this, R.string.please_input_keywords, Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedPaths.isEmpty()) {
            Toast.makeText(this, R.string.please_select_path, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 解析关键词（支持逗号、顿号、空格分隔）
        List<String> keywords = parseKeywords(keywordsInput);
        
        // 保存到历史
        historyManager.addKeywordsToHistory(keywords);
        historyManager.saveLastOptions(options);
        
        // 显示进度
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptyState.setVisibility(View.GONE);
        binding.recyclerResults.setVisibility(View.GONE);
        
        // 执行搜索
        executor.execute(() -> {
            // 扫描文件
            List<File> files = scanner.scanMultiple(selectedPaths);
            
            if (files.isEmpty()) {
                mainHandler.post(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.no_files_found, Toast.LENGTH_SHORT).show();
                });
                return;
            }
            
            // 创建搜索器
            searcher = new KeywordSearcher(options);
            searcher.setKeywords(keywords);
            
            // 执行搜索
            searcher.searchFiles(files, new KeywordSearcher.SearchCallback() {
                @Override
                public void onComplete(List<SearchResult> results) {
                    mainHandler.post(() -> {
                        binding.progressBar.setVisibility(View.GONE);
                        currentResults = results;
                        
                        if (results.isEmpty()) {
                            binding.emptyState.setVisibility(View.VISIBLE);
                            binding.recyclerResults.setVisibility(View.GONE);
                            binding.textStatistics.setVisibility(View.GONE);
                        } else {
                            binding.emptyState.setVisibility(View.GONE);
                            binding.recyclerResults.setVisibility(View.VISIBLE);
                            adapter.setResults(results);
                        }
                    });
                }
                
                @Override
                public void onStatistics(int processedFiles, int totalMatches, long searchTime) {
                    mainHandler.post(() -> {
                        binding.textStatistics.setVisibility(View.VISIBLE);
                        binding.textStatistics.setText(getString(
                                R.string.search_statistics,
                                processedFiles, totalMatches, searchTime));
                    });
                }
            });
        });
    }
    
    private List<String> parseKeywords(String input) {
        List<String> keywords = new ArrayList<>();
        
        // 支持逗号、顿号、空格分隔
        String[] parts = input.split("[,，、\\s]+");
        for (String part : parts) {
            String keyword = part.trim();
            if (!keyword.isEmpty()) {
                keywords.add(keyword);
            }
        }
        
        return keywords;
    }
    
    private void showOptionsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_search_options, null);
        
        // 设置当前选项值
        dialogView.<com.google.android.material.switchmaterial.SwitchMaterial>findViewById(R.id.switchWholeWord)
                .setChecked(options.isWholeWord());
        dialogView.<com.google.android.material.switchmaterial.SwitchMaterial>findViewById(R.id.switchCaseSensitive)
                .setChecked(options.isCaseSensitive());
        dialogView.<com.google.android.material.switchmaterial.SwitchMaterial>findViewById(R.id.switchUseRegex)
                .setChecked(options.isUseRegex());
        dialogView.<com.google.android.material.switchmaterial.SwitchMaterial>findViewById(R.id.switchRecursive)
                .setChecked(options.isRecursiveSearch());
        dialogView.<com.google.android.material.switchmaterial.SwitchMaterial>findViewById(R.id.switchConcurrent)
                .setChecked(options.isConcurrentSearch());
        dialogView.<com.google.android.material.switchmaterial.SwitchMaterial>findViewById(R.id.switchHighlight)
                .setChecked(options.isHighlightKeywords());
        
        TextInputEditText inputContext = dialogView.findViewById(R.id.inputContextSentences);
        inputContext.setText(String.valueOf(options.getContextSentences()));
        
        TextInputEditText inputThreads = dialogView.findViewById(R.id.inputMaxThreads);
        inputThreads.setText(String.valueOf(options.getMaxThreads()));
        
        TextInputEditText inputExtensions = dialogView.findViewById(R.id.inputExtensions);
        StringBuilder extBuilder = new StringBuilder();
        for (String ext : options.getFileExtensions()) {
            if (extBuilder.length() > 0) extBuilder.append(", ");
            extBuilder.append(ext);
        }
        inputExtensions.setText(extBuilder.toString());
        
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.search_options)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    // 保存选项
                    options.setWholeWord(dialogView.<com.google.android.material.switchmaterial.SwitchMaterial>findViewById(R.id.switchWholeWord).isChecked());
                    options.setCaseSensitive(dialogView.<com.google.android.material.switchmaterial.SwitchMaterial>findViewById(R.id.switchCaseSensitive).isChecked());
                    options.setUseRegex(dialogView.<com.google.android.material.switchmaterial.SwitchMaterial>findViewById(R.id.switchUseRegex).isChecked());
                    options.setRecursiveSearch(dialogView.<com.google.android.material.switchmaterial.SwitchMaterial>findViewById(R.id.switchRecursive).isChecked());
                    options.setConcurrentSearch(dialogView.<com.google.android.material.switchmaterial.SwitchMaterial>findViewById(R.id.switchConcurrent).isChecked());
                    options.setHighlightKeywords(dialogView.<com.google.android.material.switchmaterial.SwitchMaterial>findViewById(R.id.switchHighlight).isChecked());
                    
                    try {
                        options.setContextSentences(Integer.parseInt(inputContext.getText().toString()));
                    } catch (NumberFormatException e) {
                        options.setContextSentences(1);
                    }
                    
                    try {
                        options.setMaxThreads(Integer.parseInt(inputThreads.getText().toString()));
                    } catch (NumberFormatException e) {
                        options.setMaxThreads(4);
                    }
                    
                    // 解析扩展名
                    String extText = inputExtensions.getText().toString();
                    List<String> extensions = Arrays.asList(extText.split("[,，\\s]+"));
                    options.setFileExtensions(extensions);
                    
                    // 更新高亮设置
                    adapter.setHighlightKeywords(options.isHighlightKeywords());
                    
                    historyManager.saveLastOptions(options);
                    Toast.makeText(this, R.string.options_saved, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    private void showHistoryDialog() {
        List<SearchHistoryManager.KeywordHistoryItem> history = historyManager.getKeywordsHistory();
        
        if (history.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.search_history)
                    .setMessage(R.string.no_history)
                    .setPositiveButton(R.string.ok, null)
                    .show();
            return;
        }
        
        String[] items = new String[history.size()];
        for (int i = 0; i < history.size(); i++) {
            items[i] = history.get(i).keyword;
        }
        
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.search_history)
                .setItems(items, (dialog, which) -> {
                    binding.inputKeywords.setText(items[which]);
                })
                .setPositiveButton(R.string.clear_all, (dialog, which) -> {
                    historyManager.clearKeywordsHistory();
                    setupHistory();
                    Toast.makeText(this, R.string.history_cleared, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
    
    private void showExportDialog() {
        if (currentResults.isEmpty()) {
            Toast.makeText(this, R.string.no_results_to_export, Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] formats = {"TXT格式", "CSV格式", "Markdown格式"};
        
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.export_format)
                .setItems(formats, (dialog, which) -> {
                    int format = SearchResultExporter.FORMAT_TXT;
                    switch (which) {
                        case 1:
                            format = SearchResultExporter.FORMAT_CSV;
                            break;
                        case 2:
                            format = SearchResultExporter.FORMAT_MD;
                            break;
                    }
                    
                    String path = exporter.exportResults(currentResults, format, null);
                    if (path != null) {
                        Toast.makeText(this, getString(R.string.exported_to, path), Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, R.string.export_failed, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }
    
    private String generateExportContent() {
        StringBuilder sb = new StringBuilder();
        for (SearchResult result : currentResults) {
            sb.append(result.getPlainFullContext()).append("\n\n");
        }
        return sb.toString();
    }
    
    @Override
    public void onResultClick(SearchResult result) {
        // 显示详情
        new MaterialAlertDialogBuilder(this)
                .setTitle(result.getFileName())
                .setMessage(result.getPlainFullContext())
                .setPositiveButton(R.string.ok, null)
                .setNeutralButton(R.string.copy, (dialog, which) -> {
                    copyToClipboard(result.getPlainFullContext());
                })
                .show();
    }
    
    @Override
    public void onResultLongClick(SearchResult result, View anchor) {
        // 长按复制原句（无高亮、无注释）
        copyToClipboard(result.getOriginalSentence());
        Toast.makeText(this, R.string.copied, Toast.LENGTH_SHORT).show();
    }
    
    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Search Result", text);
        clipboard.setPrimaryClip(clip);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_search, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == android.R.id.home) {
            finish();
            return true;
        } else if (itemId == R.id.action_export) {
            showExportDialog();
            return true;
        } else if (itemId == R.id.action_clear) {
            clearResults();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void clearResults() {
        currentResults.clear();
        adapter.setResults(new ArrayList<>());
        binding.emptyState.setVisibility(View.VISIBLE);
        binding.recyclerResults.setVisibility(View.GONE);
        binding.textStatistics.setVisibility(View.GONE);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 取消搜索
        if (searcher != null) {
            searcher.cancel();
        }
        
        // 关闭线程池
        if (executor != null) {
            executor.shutdownNow();
        }
        
        // 清理引用，防止内存泄漏
        binding = null;
    }
}
