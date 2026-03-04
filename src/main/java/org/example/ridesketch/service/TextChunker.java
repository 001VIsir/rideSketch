package org.example.ridesketch.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能文本分块器
 * 支持多种分块策略：固定大小、滑动窗口、语义段落
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TextChunker {

    /**
     * 分块策略枚举
     */
    public enum ChunkStrategy {
        /** 固定大小分块 - 按字符数均匀切分 */
        FIXED_SIZE,
        /** 滑动窗口分块 - 有重叠区域，便于上下文理解 */
        SLIDING_WINDOW,
        /** 语义段落分块 - 按句子/段落自然分割 */
        SEMANTIC
    }

    /**
     * 分块结果
     */
    @Data
    public static class Chunk {
        private String content;
        private int startIndex;
        private int endIndex;
        private int chunkIndex;
        private double relevanceScore;

        public Chunk(String content, int startIndex, int endIndex, int chunkIndex) {
            this.content = content;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.chunkIndex = chunkIndex;
        }
    }

    /**
     * 固定大小分块
     * 将文本按固定字符数切分
     *
     * @param text 原始文本
     * @param chunkSize 每块大小（字符数）
     * @param overlap 重叠字符数
     * @return 分块列表
     */
    public List<Chunk> fixedSizeChunk(String text, int chunkSize, int overlap) {
        List<Chunk> chunks = new ArrayList<>();
        int length = text.length();
        int start = 0;
        int chunkIndex = 0;

        while (start < length) {
            int end = Math.min(start + chunkSize, length);
            String chunkText = text.substring(start, end);

            if (!chunkText.trim().isEmpty()) {
                chunks.add(new Chunk(chunkText, start, end, chunkIndex++));
            }

            // 移动窗口，考虑重叠
            start = end - overlap;
            if (start <= 0) {
                start = length; // 结束循环
            }
        }

        log.debug("固定大小分块: textLength={}, chunkSize={}, resultCount={}",
            length, chunkSize, chunks.size());
        return chunks;
    }

    /**
     * 滑动窗口分块
     * 与固定大小类似，但重叠区域更多，适合需要上下文连贯性的场景
     *
     * @param text 原始文本
     * @param windowSize 窗口大小
     * @param step 步长（每次移动的距离）
     * @return 分块列表
     */
    public List<Chunk> slidingWindowChunk(String text, int windowSize, int step) {
        List<Chunk> chunks = new ArrayList<>();
        int length = text.length();
        int start = 0;
        int chunkIndex = 0;

        while (start + windowSize <= length) {
            String chunkText = text.substring(start, start + windowSize);

            if (!chunkText.trim().isEmpty()) {
                chunks.add(new Chunk(chunkText, start, start + windowSize, chunkIndex++));
            }

            start += step;
        }

        // 处理剩余部分
        if (start < length) {
            String chunkText = text.substring(start);
            if (!chunkText.trim().isEmpty()) {
                chunks.add(new Chunk(chunkText, start, length, chunkIndex));
            }
        }

        log.debug("滑动窗口分块: textLength={}, windowSize={}, step={}, resultCount={}",
            length, windowSize, step, chunks.size());
        return chunks;
    }

    /**
     * 语义段落分块
     * 按句子、段落等自然分割，适合结构化文档
     *
     * @param text 原始文本
     * @param minChunkSize 最小分块大小
     * @return 分块列表
     */
    public List<Chunk> semanticChunk(String text, int minChunkSize) {
        List<Chunk> chunks = new ArrayList<>();

        // 按句子分割（中文句号、英文句号）
        String[] sentences = text.split("[。！？.!?\n]+");

        StringBuilder currentChunk = new StringBuilder();
        int currentStart = 0;
        int chunkIndex = 0;

        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) continue;

            int sentenceStart = text.indexOf(sentence, currentStart);
            int sentenceEnd = sentenceStart + sentence.length();

            // 如果当前块加上这个句子超过阈值，分割
            if (currentChunk.length() + sentence.length() > minChunkSize && currentChunk.length() > 0) {
                String chunkContent = currentChunk.toString().trim();
                if (!chunkContent.isEmpty()) {
                    chunks.add(new Chunk(chunkContent, currentStart,
                        sentenceStart - 1, chunkIndex++));
                }
                currentChunk = new StringBuilder();
                currentStart = sentenceEnd;
            }

            currentChunk.append(sentence).append("。");
        }

        // 添加最后一块
        if (currentChunk.length() > 0) {
            String chunkContent = currentChunk.toString().trim();
            if (!chunkContent.isEmpty()) {
                chunks.add(new Chunk(chunkContent, currentStart, text.length(), chunkIndex));
            }
        }

        log.debug("语义段落分块: textLength={}, minChunkSize={}, resultCount={}",
            text.length(), minChunkSize, chunks.size());
        return chunks;
    }

    /**
     * 智能分块 - 根据文本类型自动选择策略
     *
     * @param text 原始文本
     * @param strategy 分块策略
     * @param options 策略参数
     * @return 分块列表
     */
    public List<Chunk> smartChunk(String text, ChunkStrategy strategy, Map<String, Object> options) {
        return switch (strategy) {
            case FIXED_SIZE -> {
                int chunkSize = (int) options.getOrDefault("chunkSize", 500);
                int overlap = (int) options.getOrDefault("overlap", 50);
                yield fixedSizeChunk(text, chunkSize, overlap);
            }
            case SLIDING_WINDOW -> {
                int windowSize = (int) options.getOrDefault("windowSize", 300);
                int step = (int) options.getOrDefault("step", 150);
                yield slidingWindowChunk(text, windowSize, step);
            }
            case SEMANTIC -> {
                int minChunkSize = (int) options.getOrDefault("minChunkSize", 200);
                yield semanticChunk(text, minChunkSize);
            }
        };
    }
}
