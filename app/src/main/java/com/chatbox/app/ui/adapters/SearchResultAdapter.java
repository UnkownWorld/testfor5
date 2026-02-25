package com.chatbox.app.ui.adapters;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chatbox.app.R;
import com.chatbox.app.utils.search.SearchResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果适配器
 */
public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {
    
    private List<SearchResult> results = new ArrayList<>();
    private boolean highlightKeywords = true;
    private OnResultClickListener listener;
    
    public interface OnResultClickListener {
        void onResultClick(SearchResult result);
        void onResultLongClick(SearchResult result, View anchor);
    }
    
    public void setResults(List<SearchResult> results) {
        this.results = results != null ? results : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void setHighlightKeywords(boolean highlight) {
        this.highlightKeywords = highlight;
        notifyDataSetChanged();
    }
    
    public void setOnResultClickListener(OnResultClickListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_result, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        SearchResult result = results.get(position);
        holder.bind(result);
    }
    
    @Override
    public int getItemCount() {
        return results.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        
        private final TextView textFileName;
        private final TextView textLineNumber;
        private final TextView textKeywords;
        private final TextView textContextBefore;
        private final TextView textMatchedSentence;
        private final TextView textContextAfter;
        private final TextView textFilePath;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textFileName = itemView.findViewById(R.id.textFileName);
            textLineNumber = itemView.findViewById(R.id.textLineNumber);
            textKeywords = itemView.findViewById(R.id.textKeywords);
            textContextBefore = itemView.findViewById(R.id.textContextBefore);
            textMatchedSentence = itemView.findViewById(R.id.textMatchedSentence);
            textContextAfter = itemView.findViewById(R.id.textContextAfter);
            textFilePath = itemView.findViewById(R.id.textFilePath);
            
            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onResultClick(results.get(pos));
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onResultLongClick(results.get(pos), itemView);
                    return true;
                }
                return false;
            });
        }
        
        void bind(SearchResult result) {
            // 文件名和行号
            textFileName.setText(result.getFileName());
            textLineNumber.setText(itemView.getContext().getString(R.string.line_number, result.getLineNumber()));
            
            // 匹配关键词
            if (!result.getMatchedKeywords().isEmpty()) {
                textKeywords.setVisibility(View.VISIBLE);
                textKeywords.setText(itemView.getContext().getString(
                        R.string.matched_keywords,
                        joinKeywords(result.getMatchedKeywords())));
            } else {
                textKeywords.setVisibility(View.GONE);
            }
            
            // 上文
            if (!result.getContextBefore().isEmpty()) {
                textContextBefore.setVisibility(View.VISIBLE);
                textContextBefore.setText(joinSentences(result.getContextBefore()));
            } else {
                textContextBefore.setVisibility(View.GONE);
            }
            
            // 匹配句子（根据设置决定是否高亮）
            if (highlightKeywords && !result.getMatchedKeywords().isEmpty()) {
                textMatchedSentence.setText(highlightKeywords(result.getOriginalSentence(), result.getMatchedKeywords()));
            } else {
                textMatchedSentence.setText(result.getOriginalSentence());
            }
            
            // 下文
            if (!result.getContextAfter().isEmpty()) {
                textContextAfter.setVisibility(View.VISIBLE);
                textContextAfter.setText(joinSentences(result.getContextAfter()));
            } else {
                textContextAfter.setVisibility(View.GONE);
            }
            
            // 文件路径
            textFilePath.setText(result.getFilePath());
        }
        
        private String joinKeywords(List<String> keywords) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < keywords.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(keywords.get(i));
            }
            return sb.toString();
        }
        
        private String joinSentences(List<String> sentences) {
            StringBuilder sb = new StringBuilder();
            for (String s : sentences) {
                sb.append(s).append(" ");
            }
            return sb.toString().trim();
        }
        
        private SpannableString highlightKeywords(String text, List<String> keywords) {
            SpannableString spannable = new SpannableString(text);
            
            for (String keyword : keywords) {
                String lowerText = text.toLowerCase();
                String lowerKeyword = keyword.toLowerCase();
                int start = 0;
                
                while ((start = lowerText.indexOf(lowerKeyword, start)) != -1) {
                    int end = start + keyword.length();
                    spannable.setSpan(
                            new BackgroundColorSpan(0xFFFFFF00), // 黄色高亮
                            start, end,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    start = end;
                }
            }
            
            return spannable;
        }
    }
}
