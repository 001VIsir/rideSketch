package org.example.ridesketch.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ridesketch.service.EmbeddingService;
import org.example.ridesketch.service.KnowledgeBaseLoader;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 检索结果重排服务
 * 实现多维度结果优化：相关性、多样性、位置权重
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResultReranker {

    private final EmbeddingService embeddingService;

    /**
     * 重排配置
     */
    @lombok.Data
    public static class RerankConfig {
        /** 相关性权重 */
        private double relevanceWeight = 0.6;
        /** 多样性权重 */
        private double diversityWeight = 0.3;
        /** 位置权重（早期结果更重要） */
        private double positionWeight = 0.1;
        /** 最终返回数量 */
        private int topK = 5;
    }

    /**
     * MMR (Maximum Marginal Relevance) 重排
     * 在相关性和多样性之间取得平衡
     *
     * @param results 初始检索结果
     * @param query 用户查询
     * @param topK 返回数量
     * @return 重排后的结果
     */
    public List<KnowledgeBaseLoader.KnowledgeEntry> mmrRerank(
            List<KnowledgeBaseLoader.KnowledgeEntry> results,
            String query,
            int topK) {

        if (results == null || results.isEmpty()) {
            return results;
        }

        if (results.size() <= topK) {
            return results;
        }

        try {
            // 生成查询向量
            float[] queryVector = embeddingService.embed(query);

            // 生成结果向量
            Map<KnowledgeBaseLoader.KnowledgeEntry, float[]> resultVectors = new HashMap<>();
            for (KnowledgeBaseLoader.KnowledgeEntry entry : results) {
                float[] vector = embeddingService.embed(entry.getContent());
                resultVectors.put(entry, vector);
            }

            // MMR 算法
            List<KnowledgeBaseLoader.KnowledgeEntry> selected = new ArrayList<>();
            Set<Integer> remainingIndices = new HashSet<>();

            // 初始化：选择相关性最高的
            double maxRelevance = -1;
            int bestIndex = 0;
            for (int i = 0; i < results.size(); i++) {
                double relevance = results.get(i).getSimilarity();
                if (relevance > maxRelevance) {
                    maxRelevance = relevance;
                    bestIndex = i;
                }
            }

            selected.add(results.get(bestIndex));
            remainingIndices.add(bestIndex);

            // 迭代选择
            while (selected.size() < topK && !remainingIndices.isEmpty()) {
                double bestScore = -1;
                int bestNextIndex = -1;

                for (int i : remainingIndices) {
                    KnowledgeBaseLoader.KnowledgeEntry entry = results.get(i);

                    // 相关性分数（归一化）
                    double relevance = entry.getSimilarity();

                    // 与已选结果的最大相似度（用于衡量多样性）
                    double maxSimilarityToSelected = 0;
                    for (KnowledgeBaseLoader.KnowledgeEntry selectedEntry : selected) {
                        float[] selectedVector = resultVectors.get(selectedEntry);
                        float[] candidateVector = resultVectors.get(entry);
                        double similarity = embeddingService.cosineSimilarity(selectedVector, candidateVector);
                        maxSimilarityToSelected = Math.max(maxSimilarityToSelected, similarity);
                    }

                    // MMR 分数 = 相关性 - λ * 多样性
                    double lambda = 0.5; // 平衡因子
                    double mmrScore = lambda * relevance - (1 - lambda) * maxSimilarityToSelected;

                    if (mmrScore > bestScore) {
                        bestScore = mmrScore;
                        bestNextIndex = i;
                    }
                }

                if (bestNextIndex != -1) {
                    selected.add(results.get(bestNextIndex));
                    remainingIndices.remove(bestNextIndex);
                } else {
                    break;
                }
            }

            log.debug("MMR重排: 原始数量={}, 重排后={}", results.size(), selected.size());
            return selected;

        } catch (Exception e) {
            log.warn("MMR重排失败，使用原始排序: {}", e.getMessage());
            return results.subList(0, Math.min(topK, results.size()));
        }
    }

    /**
     * 综合重排 - 考虑相关性、多样性、位置
     *
     * @param results 初始检索结果
     * @param query 用户查询
     * @param config 重排配置
     * @return 重排后的结果
     */
    public List<KnowledgeBaseLoader.KnowledgeEntry> comprehensiveRerank(
            List<KnowledgeBaseLoader.KnowledgeEntry> results,
            String query,
            RerankConfig config) {

        if (results == null || results.isEmpty()) {
            return results;
        }

        int topK = config.getTopK();
        if (results.size() <= topK) {
            return results;
        }

        try {
            // 计算每个结果的重排分数
            List<RerankedEntry> scoredResults = new ArrayList<>();

            for (int i = 0; i < results.size(); i++) {
                KnowledgeBaseLoader.KnowledgeEntry entry = results.get(i);

                // 1. 相关性分数（使用原始相似度）
                double relevanceScore = entry.getSimilarity();

                // 2. 多样性分数（与同类别的距离）
                double diversityScore = calculateDiversityScore(results, i);

                // 3. 位置分数（早期结果更重要）
                double positionScore = 1.0 - (double) i / results.size();

                // 综合分数
                double finalScore =
                    relevanceScore * config.getRelevanceWeight() +
                    diversityScore * config.getDiversityWeight() +
                    positionScore * config.getPositionWeight();

                scoredResults.add(new RerankedEntry(entry, finalScore));
            }

            // 按分数降序排序
            scoredResults.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

            // 返回 Top-K
            List<KnowledgeBaseLoader.KnowledgeEntry> reranked = scoredResults.stream()
                .limit(topK)
                .map(RerankedEntry::getEntry)
                .collect(Collectors.toList());

            log.debug("综合重排: 原始数量={}, 重排后={}", results.size(), reranked.size());
            return reranked;

        } catch (Exception e) {
            log.warn("综合重排失败，使用原始排序: {}", e.getMessage());
            return results.subList(0, Math.min(topK, results.size()));
        }
    }

    /**
     * 计算多样性分数
     * 衡量与同类别的其他结果的差异程度
     */
    private double calculateDiversityScore(List<KnowledgeBaseLoader.KnowledgeEntry> results, int index) {
        KnowledgeBaseLoader.KnowledgeEntry target = results.get(index);
        String targetCategory = target.getCategory();

        // 计算与同类别的比例（同类越少，多样性越高）
        long sameCategoryCount = results.stream()
            .filter(e -> e.getCategory().equals(targetCategory))
            .count();

        return 1.0 - (double) sameCategoryCount / results.size();
    }

    /**
     * 重排结果包装类
     */
    private static class RerankedEntry {
        private final KnowledgeBaseLoader.KnowledgeEntry entry;
        private final double score;

        public RerankedEntry(KnowledgeBaseLoader.KnowledgeEntry entry, double score) {
            this.entry = entry;
            this.score = score;
        }

        public KnowledgeBaseLoader.KnowledgeEntry getEntry() {
            return entry;
        }

        public double getScore() {
            return score;
        }
    }

    /**
     * 简单重排 - 按相关性排序（无额外计算）
     *
     * @param results 初始检索结果
     * @param topK 返回数量
     * @return 重排后的结果
     */
    public List<KnowledgeBaseLoader.KnowledgeEntry> simpleRerank(
            List<KnowledgeBaseLoader.KnowledgeEntry> results,
            int topK) {

        if (results == null || results.isEmpty()) {
            return results;
        }

        // 按相似度降序排序
        List<KnowledgeBaseLoader.KnowledgeEntry> sorted = new ArrayList<>(results);
        sorted.sort((a, b) -> Float.compare(b.getSimilarity(), a.getSimilarity()));

        return sorted.stream()
            .limit(topK)
            .collect(Collectors.toList());
    }
}
