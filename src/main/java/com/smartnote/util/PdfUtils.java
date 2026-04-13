package com.smartnote.util;

import com.smartnote.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * PDF 文件处理工具类
 */
@Slf4j
public class PdfUtils {

    //最大文件大小
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    //最大文本长度：20000 字符
    private static final int MAX_TEXT_LENGTH = 20000;

    /**
     * 从 MultipartFile 中提取 PDF 文本内容
     * 1. 验证文件是否为空
     * 2. 验证文件大小是否超限
     * 3. 验证文件类型是否为 PDF
     * 4. 读取文件输入流
     * 5. 使用 PDFBox 解析 PDF
     * 6. 提取所有页面的文本内容
     * 7. 检查文本是否为空
     * 8. 截断超长文本
     * 9. 关闭资源，释放内存
     * 10. 返回纯文本字符串
     * @param file 上传的 PDF 文件（MultipartFile 类型）
     * @return 提取的纯文本内容（已截断至最大长度）
     * @throws BusinessException 当文件验证失败或解析出错时抛出
     */
    public static String extractTextFromPdf(MultipartFile file) {
        // 检查文件是否为空
        if (file.isEmpty()) {
            throw new BusinessException("上传的文件为空");
        }
        // 检查文件大小是否超过限制
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过 10MB");
        }
        // 检查文件类型是否为 PDF
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();

        boolean isPdf = "application/pdf".equals(contentType) ||
                (originalFilename != null && originalFilename.toLowerCase().endsWith(".pdf"));

        //!"application/pdf".equals(contentType)
        if (!isPdf) {
            throw new BusinessException("仅支持 PDF 格式文件");
        }

        // 声明 PDDocument 对象，用于表示 PDF 文档
        // PDDocument 是 PDFBox 的核心类，代表一个完整的 PDF 文档
        PDDocument document = null;

        try {
            /**从 MultipartFile 获取输入流
             * InputStream 是 Java IO 的基础接口，用于读取字节数据
             * getInputStream() 会创建一个连接到上传文件的输入流
             */
            InputStream inputStream = file.getInputStream();
            /** 使用 PDFBox 加载 PDF 文档
             * PDDocument.load() 方法：
             * 1. 读取输入流的字节数据
             * 2. 解析 PDF 结构（页面对象、字体、图像等）
             * 3. 构建内存中的文档对象树
             * 4. 返回 PDDocument 对象
             */
            document = PDDocument.load(inputStream);
            /** 创建文本提取器对象
             * PDFTextStripper 是 PDFBox 提供的文本提取工具
             * 它可以：
             * 1. 遍历 PDF 的所有页面
             * 2. 提取每页的文本内容
             * 3. 保持文本的阅读顺序
             * 4. 处理换行和段落
             */
            PDFTextStripper textStripper = new PDFTextStripper();
            /** 执行文本提取
             * getText() 方法：
             * 1. 从第一页开始遍历所有页面
             * 2. 提取每页的文本内容
             * 3. 拼接成一个完整的字符串
             * 4. 保留基本的换行符
             */
            String pdfText = textStripper.getText(document);
            // 检查提取的文本是否为空
            if (pdfText == null || pdfText.trim().isEmpty()) {
                // 抛出业务异常
                throw new BusinessException("PDF 文件中未检测到文本内容（可能是扫描版图片）");
            }

            // 记录原始文本长度（用于日志）
            int originalLength = pdfText.length();

            // 如果文本长度超过限制，进行截断
            if (pdfText.length() > MAX_TEXT_LENGTH) {
                // substring(0, MAX_TEXT_LENGTH) 截取前 MAX_TEXT_LENGTH 个字符
                pdfText = pdfText.substring(0, MAX_TEXT_LENGTH);
                // 记录警告日志
                log.warn("PDF 文本过长，已从 {} 字符截断至 {} 字符", originalLength, MAX_TEXT_LENGTH);
            }
            log.info("PDF 文本提取成功: 原始长度={}, 最终长度={}", originalLength, pdfText.length());

            // 返回提取的文本内容
            return pdfText;
        } catch (IOException e) {
            // IOException 是受检异常，必须处理
            // 记录错误日志
            log.error("PDF 文件解析失败: fileName={}", file.getOriginalFilename(), e);
            // 抛出业务异常
            throw new BusinessException("PDF 文件解析失败: " + e.getMessage());
        } finally {
            // finally 块无论是否发生异常都会执行
            // 确保 PDF 文档对象被正确关闭，释放内存
            // 检查 document 是否不为 null（防止空指针异常）
            if (document != null) {
                try {
                    // 关闭 PDF 文档
                    // close() 方法：
                    // 释放文档占用的内存,关闭相关的输入流,清理临时文件,销毁文档对象树
                    document.close();
                    log.info("PDF 文档已关闭，资源已释放");
                } catch (IOException e) {
                    // 关闭失败时的处理
                    log.error("关闭 PDF 文档失败", e);
                }
            }
        }
    }

    /**
     * 从文件名中提取不带扩展名的名称
     *
     * @param originalFilename 原始文件名
     * @return 不带 .pdf 扩展名的文件名
     */
    public static String extractFileNameWithoutExtension(String originalFilename) {
        // 如果文件名为 null，返回默认名称
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "未命名文档";
        }
        // 查找最后一个 "." 的位置
        // lastIndexOf() 从字符串末尾向前搜索
        //没找到时返回 -1
        int dotIndex = originalFilename.lastIndexOf(".");
        // 如果找到 "." 且不在开头
        if (dotIndex > 0) {
            // substring(0, dotIndex) 截取 "." 之前的部分
            return originalFilename.substring(0, dotIndex);
        }
        // 如果没有扩展名，返回原文件名
        return originalFilename;
    }

}
