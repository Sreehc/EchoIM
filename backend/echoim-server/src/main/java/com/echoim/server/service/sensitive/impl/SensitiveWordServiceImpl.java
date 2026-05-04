package com.echoim.server.service.sensitive.impl;

import com.echoim.server.entity.ImSensitiveWordEntity;
import com.echoim.server.mapper.ImSensitiveWordMapper;
import com.echoim.server.service.sensitive.SensitiveWordService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SensitiveWordServiceImpl implements SensitiveWordService {

    private final ImSensitiveWordMapper sensitiveWordMapper;

    private volatile List<SensitiveWordEntry> wordEntries = new CopyOnWriteArrayList<>();
    private volatile boolean initialized = false;

    public SensitiveWordServiceImpl(ImSensitiveWordMapper sensitiveWordMapper) {
        this.sensitiveWordMapper = sensitiveWordMapper;
    }

    @Override
    public boolean containsBlockedWords(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        ensureInitialized();
        String normalizedContent = content.toLowerCase();
        for (SensitiveWordEntry entry : wordEntries) {
            if (entry.action == 2 && normalizedContent.contains(entry.normalizedWord)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String filterContent(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }
        ensureInitialized();
        String result = content;
        for (SensitiveWordEntry entry : wordEntries) {
            if (entry.action == 1) {
                // Mark action: replace with asterisks
                String replacement = "*".repeat(entry.word.length());
                result = result.replaceAll("(?i)" + Pattern.quote(entry.word), replacement);
            }
        }
        return result;
    }

    @Override
    public List<String> getAllSensitiveWords() {
        ensureInitialized();
        return wordEntries.stream()
                .map(entry -> entry.word)
                .collect(Collectors.toList());
    }

    @Override
    public void addSensitiveWord(String word, String category, Integer level, Integer action, Long createdBy) {
        if (word == null || word.isBlank()) {
            return;
        }
        // Check if word already exists
        ImSensitiveWordEntity existing = sensitiveWordMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ImSensitiveWordEntity>()
                        .eq(ImSensitiveWordEntity::getWord, word.trim())
                        .last("LIMIT 1"));
        if (existing != null) {
            return;
        }
        ImSensitiveWordEntity entity = new ImSensitiveWordEntity();
        entity.setWord(word.trim());
        entity.setCategory(category != null ? category : "default");
        entity.setLevel(level != null ? level : 1);
        entity.setAction(action != null ? action : 1);
        entity.setStatus(1);
        entity.setCreatedBy(createdBy);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        sensitiveWordMapper.insert(entity);
        reloadCache();
    }

    @Override
    public void removeSensitiveWord(Long wordId) {
        if (wordId == null) {
            return;
        }
        sensitiveWordMapper.deleteById(wordId);
        reloadCache();
    }

    @Override
    public void reloadCache() {
        List<ImSensitiveWordEntity> entities = sensitiveWordMapper.selectAllEnabled();
        List<SensitiveWordEntry> newEntries = new CopyOnWriteArrayList<>();
        for (ImSensitiveWordEntity entity : entities) {
            SensitiveWordEntry entry = new SensitiveWordEntry();
            entry.id = entity.getId();
            entry.word = entity.getWord();
            entry.normalizedWord = entity.getWord().toLowerCase();
            entry.category = entity.getCategory();
            entry.level = entity.getLevel();
            entry.action = entity.getAction();
            newEntries.add(entry);
        }
        this.wordEntries = newEntries;
        this.initialized = true;
    }

    private void ensureInitialized() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    reloadCache();
                }
            }
        }
    }

    private static class SensitiveWordEntry {
        Long id;
        String word;
        String normalizedWord;
        String category;
        Integer level;
        Integer action;
    }
}
