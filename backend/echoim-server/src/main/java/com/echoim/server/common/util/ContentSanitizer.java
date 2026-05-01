package com.echoim.server.common.util;

/**
 * 消息内容安全工具：对用户输入做基础 XSS 防护。
 * <p>
 * 聊天消息为纯文本语义，不允许携带可执行 HTML。
 * 本工具在消息入库前对特殊字符做转义，前端通过 Vue 模板渲染时天然安全，
 * 此处作为纵深防御的第二道关卡。
 */
public final class ContentSanitizer {

    private ContentSanitizer() {
    }

    /**
     * 转义 HTML 特殊字符，防止存储型 XSS。
     * 仅处理消息正文（msgType = TEXT），富媒体消息（IMAGE/FILE/SYSTEM 等）不适用。
     */
    public static String escapeHtml(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#x27;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 判断是否为纯文本消息类型。
     */
    public static boolean isTextType(int msgType) {
        // 1 = TEXT
        return msgType == 1;
    }
}
