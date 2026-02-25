package com.chatbox.app.utils.search;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 编码检测器
 * 
 * 自动检测文件编码，支持UTF-8、GBK、GB2312、GB18030、BIG5等常见编码。
 * 使用BOM检测和字节模式分析相结合的方法。
 */
public class EncodingDetector {
    
    // BOM标记
    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final byte[] UTF16_LE_BOM = {(byte) 0xFF, (byte) 0xFE};
    private static final byte[] UTF16_BE_BOM = {(byte) 0xFE, (byte) 0xFF};
    private static final byte[] UTF32_LE_BOM = {(byte) 0xFF, (byte) 0xFE, 0x00, 0x00};
    private static final byte[] UTF32_BE_BOM = {0x00, 0x00, (byte) 0xFE, (byte) 0xFF};
    
    // 默认检测缓冲区大小
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    
    /**
     * 检测文件编码
     * @param file 文件
     * @return 检测到的编码，如果无法确定则返回UTF-8
     */
    public static Charset detectEncoding(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(fis, DEFAULT_BUFFER_SIZE)) {
            
            bis.mark(DEFAULT_BUFFER_SIZE);
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int bytesRead = bis.read(buffer);
            bis.reset();
            
            if (bytesRead <= 0) {
                return StandardCharsets.UTF_8;
            }
            
            // 1. 首先检查BOM
            Charset bomEncoding = detectByBOM(buffer, bytesRead);
            if (bomEncoding != null) {
                return bomEncoding;
            }
            
            // 2. 检查是否是有效的UTF-8
            if (isValidUTF8(buffer, bytesRead)) {
                return StandardCharsets.UTF_8;
            }
            
            // 3. 尝试检测中文编码
            Charset chineseEncoding = detectChineseEncoding(buffer, bytesRead);
            if (chineseEncoding != null) {
                return chineseEncoding;
            }
            
            // 4. 默认返回系统编码或UTF-8
            return getDefaultEncoding();
            
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }
    
    /**
     * 检测输入流编码
     * @param inputStream 输入流（需要支持mark/reset）
     * @return 检测到的编码
     */
    public static Charset detectEncoding(InputStream inputStream) {
        try {
            if (!inputStream.markSupported()) {
                return StandardCharsets.UTF_8;
            }
            
            inputStream.mark(DEFAULT_BUFFER_SIZE);
            byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
            int bytesRead = inputStream.read(buffer);
            inputStream.reset();
            
            if (bytesRead <= 0) {
                return StandardCharsets.UTF_8;
            }
            
            // 检查BOM
            Charset bomEncoding = detectByBOM(buffer, bytesRead);
            if (bomEncoding != null) {
                return bomEncoding;
            }
            
            // 检查UTF-8
            if (isValidUTF8(buffer, bytesRead)) {
                return StandardCharsets.UTF_8;
            }
            
            // 尝试检测中文编码
            Charset chineseEncoding = detectChineseEncoding(buffer, bytesRead);
            if (chineseEncoding != null) {
                return chineseEncoding;
            }
            
            return getDefaultEncoding();
            
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }
    
    /**
     * 通过BOM检测编码
     */
    private static Charset detectByBOM(byte[] buffer, int length) {
        if (length >= 4) {
            // UTF-32 BE
            if (buffer[0] == UTF32_BE_BOM[0] && buffer[1] == UTF32_BE_BOM[1] 
                && buffer[2] == UTF32_BE_BOM[2] && buffer[3] == UTF32_BE_BOM[3]) {
                return Charset.forName("UTF-32BE");
            }
            // UTF-32 LE
            if (buffer[0] == UTF32_LE_BOM[0] && buffer[1] == UTF32_LE_BOM[1] 
                && buffer[2] == UTF32_LE_BOM[2] && buffer[3] == UTF32_LE_BOM[3]) {
                return Charset.forName("UTF-32LE");
            }
        }
        
        if (length >= 3) {
            // UTF-8 BOM
            if (buffer[0] == UTF8_BOM[0] && buffer[1] == UTF8_BOM[1] && buffer[2] == UTF8_BOM[2]) {
                return StandardCharsets.UTF_8;
            }
        }
        
        if (length >= 2) {
            // UTF-16 BE
            if (buffer[0] == UTF16_BE_BOM[0] && buffer[1] == UTF16_BE_BOM[1]) {
                return StandardCharsets.UTF_16BE;
            }
            // UTF-16 LE
            if (buffer[0] == UTF16_LE_BOM[0] && buffer[1] == UTF16_LE_BOM[1]) {
                return StandardCharsets.UTF_16LE;
            }
        }
        
        return null;
    }
    
    /**
     * 检查是否是有效的UTF-8编码
     */
    private static boolean isValidUTF8(byte[] buffer, int length) {
        int i = 0;
        while (i < length) {
            byte b = buffer[i];
            
            if ((b & 0x80) == 0) {
                // ASCII字符 (0xxxxxxx)
                i++;
            } else if ((b & 0xE0) == 0xC0) {
                // 2字节UTF-8 (110xxxxx 10xxxxxx)
                if (i + 1 >= length) return false;
                if ((buffer[i + 1] & 0xC0) != 0x80) return false;
                i += 2;
            } else if ((b & 0xF0) == 0xE0) {
                // 3字节UTF-8 (1110xxxx 10xxxxxx 10xxxxxx)
                if (i + 2 >= length) return false;
                if ((buffer[i + 1] & 0xC0) != 0x80) return false;
                if ((buffer[i + 2] & 0xC0) != 0x80) return false;
                i += 3;
            } else if ((b & 0xF8) == 0xF0) {
                // 4字节UTF-8 (11110xxx 10xxxxxx 10xxxxxx 10xxxxxx)
                if (i + 3 >= length) return false;
                if ((buffer[i + 1] & 0xC0) != 0x80) return false;
                if ((buffer[i + 2] & 0xC0) != 0x80) return false;
                if ((buffer[i + 3] & 0xC0) != 0x80) return false;
                i += 4;
            } else {
                // 无效的UTF-8字节
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检测中文编码（GBK/GB2312/GB18030/BIG5）
     */
    private static Charset detectChineseEncoding(byte[] buffer, int length) {
        // 统计常见中文字符范围的出现频率
        int gbkScore = 0;
        int big5Score = 0;
        
        for (int i = 0; i < length - 1; i++) {
            byte b1 = buffer[i];
            byte b2 = buffer[i + 1];
            
            // 检查是否是GBK双字节字符
            // GBK第一字节: 0x81-0xFE, 第二字节: 0x40-0xFE (不含0x7F)
            if ((b1 & 0xFF) >= 0x81 && (b1 & 0xFF) <= 0xFE) {
                if ((b2 & 0xFF) >= 0x40 && (b2 & 0xFF) <= 0xFE && (b2 & 0xFF) != 0x7F) {
                    gbkScore++;
                    i++; // 跳过第二个字节
                }
            }
        }
        
        // 如果检测到GBK特征，返回GBK编码
        if (gbkScore > 5) {
            // 优先尝试GB18030（GBK的超集）
            try {
                return Charset.forName("GB18030");
            } catch (Exception e) {
                try {
                    return Charset.forName("GBK");
                } catch (Exception e2) {
                    return Charset.forName("GB2312");
                }
            }
        }
        
        return null;
    }
    
    /**
     * 获取默认编码
     */
    private static Charset getDefaultEncoding() {
        // 获取系统默认编码
        String defaultCharsetName = System.getProperty("file.encoding", "UTF-8");
        try {
            return Charset.forName(defaultCharsetName);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }
    
    /**
     * 获取BOM长度
     * @param charset 编码
     * @return BOM长度，如果没有BOM则返回0
     */
    public static int getBOMLength(Charset charset) {
        if (charset == null) {
            return 0;
        }
        
        String name = charset.name().toUpperCase();
        if (name.contains("UTF-8")) {
            return 3; // UTF-8 BOM
        } else if (name.contains("UTF-16") || name.contains("UTF16")) {
            return 2; // UTF-16 BOM
        } else if (name.contains("UTF-32") || name.contains("UTF32")) {
            return 4; // UTF-32 BOM
        }
        return 0;
    }
    
    /**
     * 读取文件内容（自动检测编码）
     * @param file 文件
     * @return 文件内容
     */
    public static String readFileWithAutoEncoding(File file) {
        Charset charset = detectEncoding(file);
        int bomLength = getBOMLength(charset);
        
        try (FileInputStream fis = new FileInputStream(file);
             InputStreamReader isr = new InputStreamReader(fis, charset);
             BufferedReader reader = new BufferedReader(isr)) {
            
            // 跳过BOM
            if (bomLength > 0) {
                fis.skip(bomLength);
            }
            
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[8192];
            int charsRead;
            
            while ((charsRead = reader.read(buffer)) != -1) {
                content.append(buffer, 0, charsRead);
            }
            
            return content.toString();
            
        } catch (Exception e) {
            return "";
        }
    }
}
