package com.smartnote.constant;

public class AiPromptConstant {

    // ================== 角色设定提示词 ==================

    // 笔记分析
    public static final String NOTE_ANALYSIS_ROLE = "你是一个专业的笔记分析助手。请帮助用户总结、分析和提炼笔记内容的核心要点。\n\n";

    // 知识检索
    public static final String KNOWLEDGE_SEARCH_ROLE = "你是一个专业的知识检索助手。请帮助用户查找、整理和总结相关知识信息。回答时要提供准确的信息来源或参考依据，如果不确定请明确说明。\n\n";

    // 聊天
    public static final String CHAT_ROLE = "你是一个友好、专业的智能助手。请用简洁清晰的语言回答用户的问题。\n\n";

    // ================== 意图回应提示词 ==================

    // 意图识别
    public static final String INTENT_RECOGNITION_TEMPLATE = "请判断以下用户输入的意图类型，只返回一个词（NOTE_ANALYSIS、KNOWLEDGE_SEARCH 或 CHAT），不要有其他内容：\n\n用户输入：";

    // ================== 笔记分析提示词 ==================

    // 笔记总结
    public static final String NOTE_SUMMARY_PROMPT = "请对以下笔记内容进行简洁总结，突出核心要点：\n\n";

    // 笔记关键点
    public static final String NOTE_KEY_POINTS_PROMPT = "请提取以下笔记的关键要点，使用列表形式呈现：\n\n";

    // 笔记标签
    public static final String NOTE_TAGS_PROMPT = "请为以下笔记生成3-5个合适的标签，用逗号分隔：\n\n";

    // 笔记默认
    public static final String NOTE_DEFAULT_PROMPT = "请分析以下笔记内容：\n\n";

    // ================== PDF 文档分析提示词 ==================

    public static final String PDF_SUMMARY_TEMPLATE = """
            你是一位专业的文档分析专家。请对以下 PDF 文档内容进行全面总结。
            
            【总结要求】
            1. 核心主题：用一句话概括文档的核心主题
            2. 关键要点：列出 5-8 个关键要点，每个要点简明扼要
            3. 重要结论：总结文档的主要结论或建议
            4. 适用场景：说明该文档适用于哪些场景或人群
            
            【输出格式】
            请使用 Markdown 格式输出，结构清晰，层次分明。
            
            【注意事项】
            - 保持客观准确，不要添加原文没有的内容
            - 语言简洁流畅，避免冗长啰嗦
            - 重点突出，便于快速阅读
            
            ---
            
            【文档内容】
            %s
            """;

    //私有构造函数，防止实例化
    private AiPromptConstant() {
    }
}
