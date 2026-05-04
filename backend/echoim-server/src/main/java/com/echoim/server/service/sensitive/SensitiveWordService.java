package com.echoim.server.service.sensitive;

import java.util.List;

public interface SensitiveWordService {

    /**
     * Check if content contains sensitive words
     * @param content the content to check
     * @return true if content contains sensitive words that should be blocked
     */
    boolean containsBlockedWords(String content);

    /**
     * Filter content by replacing sensitive words with asterisks
     * @param content the content to filter
     * @return the filtered content
     */
    String filterContent(String content);

    /**
     * Get all sensitive words (for admin management)
     * @return list of sensitive word strings
     */
    List<String> getAllSensitiveWords();

    /**
     * Add a new sensitive word
     * @param word the word to add
     * @param category the category
     * @param level the level (1=normal, 2=severe)
     * @param action the action (1=mark, 2=block)
     * @param createdBy the creator user ID
     */
    void addSensitiveWord(String word, String category, Integer level, Integer action, Long createdBy);

    /**
     * Remove a sensitive word
     * @param wordId the word ID to remove
     */
    void removeSensitiveWord(Long wordId);

    /**
     * Reload the sensitive word cache
     */
    void reloadCache();
}
