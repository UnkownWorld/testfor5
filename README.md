# 聊天盒子 - Chatbox App

一款功能强大的Android聊天应用，集成AI对话和关键词检索功能。

## 功能特性

### 🤖 AI聊天功能
- 支持多种AI提供商（OpenAI、Claude、Gemini、DeepSeek等）
- 流式响应，实时显示生成内容
- Markdown渲染和代码高亮
- 多会话管理

### 🔍 关键词检索功能
- **Aho-Corasick算法** - 高效多关键词匹配，时间复杂度O(n+m+z)
- **并发检索** - 多线程并发搜索，可配置线程数
- **编码检测** - 自动检测UTF-8、GBK、GB2312、GB18030、BIG5等编码
- **本地存储** - 记住关键词历史和搜索选项
- **导出功能** - 支持TXT、CSV、Markdown格式导出
- **上下文显示** - 显示匹配句子的前后文
- **长按复制** - 复制原句（无高亮、无注释）

### 📁 文件处理
- 文件附件支持
- 智能分割（按章节、行数、字符数、正则表达式）
- 批量发送

### 🎯 技能管理
- 自定义技能文件
- 技能排序
- 系统提示词管理

## 项目结构

```
app/src/main/java/com/chatbox/app/
├── ui/
│   ├── activities/           # Activity界面
│   │   ├── MainActivity.java
│   │   ├── ChatActivity.java
│   │   ├── SearchActivity.java
│   │   ├── SettingsActivity.java
│   │   └── ApiConfigActivity.java
│   ├── adapters/             # 列表适配器
│   │   ├── MessageAdapter.java
│   │   └── SearchResultAdapter.java
│   └── viewmodels/           # ViewModel
│       └── ChatViewModel.java
├── data/
│   ├── entity/               # 数据实体
│   ├── repository/           # 数据仓库
│   └── local/                # 本地数据库
├── network/                  # 网络请求
│   ├── ApiClient.java
│   └── ApiService.java
└── utils/
    ├── FileSplitter.java     # 文件分割器
    ├── FileContentManager.java
    ├── SkillManager.java     # 技能管理
    └── search/               # 关键词检索模块
        ├── AhoCorasickMatcher.java
        ├── EncodingDetector.java
        ├── KeywordSearcher.java
        ├── FileScanner.java
        ├── SearchOptions.java
        ├── SearchResult.java
        ├── SearchHistoryManager.java
        ├── SearchResultExporter.java
        └── SentenceSplitter.java
```

## 环境要求

- Android 7.0 (API 24) 或更高版本
- JDK 8 或更高版本
- Android SDK API 34
- Android Build Tools 34.0.0

## 编译打包

### Debug版本

```bash
# 设置SDK路径
export ANDROID_HOME=/path/to/android-sdk

# 赋予执行权限
chmod +x gradlew

# 编译Debug APK
./gradlew assembleDebug

# APK输出位置
# app/build/outputs/apk/debug/app-debug.apk
```

### Release版本

```bash
# 生成签名密钥
keytool -genkey -v -keystore chatbox.keystore -alias chatbox -keyalg RSA -keysize 2048 -validity 10000

# 编译Release APK
./gradlew assembleRelease

# APK输出位置
# app/build/outputs/apk/release/app-release.apk
```

## 安装使用

1. 下载APK文件
2. 在Android设备上打开文件管理器
3. 点击APK文件安装
4. 如提示"未知来源"，请在设置中允许安装未知来源应用
5. 打开应用，配置API提供商
6. 开始使用

## 关键词检索使用说明

1. 点击主界面的搜索按钮进入搜索界面
2. 输入关键词（支持逗号、顿号、空格分隔）
3. 选择要搜索的文件或文件夹
4. 点击搜索按钮开始检索
5. 查看搜索结果，点击查看详情
6. 长按结果可复制原句
7. 支持导出搜索结果

## 支持的AI提供商

| 提供商 | 状态 |
|--------|------|
| OpenAI | ✅ 支持 |
| Claude (Anthropic) | ✅ 支持 |
| Google Gemini | ✅ 支持 |
| Azure OpenAI | ✅ 支持 |
| DeepSeek | ✅ 支持 |
| SiliconFlow | ✅ 支持 |
| Ollama (本地) | ✅ 支持 |
| Groq | ✅ 支持 |
| Mistral AI | ✅ 支持 |
| LM Studio | ✅ 支持 |
| Perplexity | ✅ 支持 |
| xAI | ✅ 支持 |
| OpenRouter | ✅ 支持 |
| 自定义API | ✅ 支持 |

## 技术栈

- **语言**: Java
- **架构**: MVVM
- **数据库**: Room
- **网络**: Retrofit + OkHttp
- **UI**: Material Design 3
- **Markdown**: Markwon
- **图片加载**: Glide

## 许可证

MIT License

## 更新日志

### v1.0.0 (2026-02-25)
- 初始版本发布
- AI聊天功能
- 关键词检索功能
- 文件分割功能
- 技能管理功能

## GitHub仓库

https://github.com/UnkownWorld/testfor5

---

**开发者**: Chatbox Team  
**版本**: 1.0.0  
**更新日期**: 2026-02-25
