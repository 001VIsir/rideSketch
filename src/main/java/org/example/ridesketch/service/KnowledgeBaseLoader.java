package org.example.ridesketch.service;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库加载器
 * 负责加载、存储和管理骑行知识库
 * 使用Redis存储向量数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeBaseLoader {

    private static final String VECTOR_KEY_PREFIX = "rag:vector:";
    private static final String TEXT_KEY_PREFIX = "rag:text:";
    private static final String CATEGORY_KEY = "rag:categories";

    private final EmbeddingService embeddingService;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 骑行知识库 - 按类别组织（扩展到100+条）
     */
    private static final Map<String, List<String>> KNOWLEDGE_BASE = new HashMap<>();

    static {
        // 骑行技巧
        KNOWLEDGE_BASE.put("骑行技巧", Arrays.asList(
                "保持正确的骑行姿势，身体略微前倾约15-30度，肘部微屈",
                "踏频保持在80-100 RPM之间最省力效率最高",
                "爬坡时使用轻齿轮，sit/stand交替发力可以节省体力",
                "下坡时优先使用后刹车，控制速度在安全范围内",
                "长时间骑行记得补充水分和电解质，每15-20分钟补充一次",
                "学会使用脚跟抬高法来确定正确的坐垫高度",
                "摇车时身体与自行车保持协调，重心略前移",
                "高速骑行时减少空气阻力，身体贴近车把",
                "过弯时内侧膝盖朝下，外侧脚踩实踏板",
                "平路骑行时保持均匀踏频，避免突然加减速"
        ));

        // 骑行安全
        KNOWLEDGE_BASE.put("骑行安全", Arrays.asList(
                "始终佩戴头盔，选择合适尺寸并系紧下颌带",
                "夜间骑行必须安装前后灯，前灯白色，后灯红色",
                "遵守交通规则，使用手势信号指示转弯和停车",
                "定期检查刹车系统、轮胎气压和链条状况",
                "避免戴耳机骑车，保持注意力集中",
                "与前车保持安全距离，特别是下坡时",
                "恶劣天气尽量减少外出骑行",
                "骑行时不要单手扶把，避免急刹车",
                "在路口时要左右观察，确认安全再通过",
                "雨天骑行注意防滑，降低车速",
                "山路过弯要减速鸣笛，注意对向来车",
                "夜骑时穿反光服装增加可视性",
                "保持车距，避免并行骑行阻挡道路",
                "学习使用骑行手势与队友沟通"
        ));

        // 北京骑行路线
        KNOWLEDGE_BASE.put("北京骑行路线", Arrays.asList(
                "天安门-故宫-景山：适合初学者，路程短，约5公里",
                "长安街：沿着长安街骑行，经过天安门广场",
                "奥林匹克公园：绕鸟巢、水立方骑行一圈约8公里",
                "妙峰山：经典爬坡路线，全长约20公里，爬升约800米",
                "什刹海-胡同：适合休闲骑行，穿梭老北京胡同",
                "温榆河绿道：新建成的骑行道，全长约30公里",
                "永定河绿道：沿永定河骑行，风景优美，约25公里",
                "三山五园：连接香山、颐和园等历史景点",
                "环密云水库：全程约90公里，适合进阶骑友",
                "延庆百里画廊：风景如画，约70公里，适合周末长途",
                "通州大运河：沿大运河骑行，约20公里",
                "大兴南海子公园：休闲骑行，约10公里"
        ));

        // 骑行装备
        KNOWLEDGE_BASE.put("骑行装备", Arrays.asList(
                "头盔：保护头部安全，必不可少，选择有安全认证的",
                "手套：防滑、减震，保护手部，长距离骑行必备",
                "骑行裤：带坐垫的骑行裤可减少摩擦，防止臀部疼痛",
                "车灯：夜间骑行必备，前灯至少200流明",
                "水壶：保持水分补充，建议使用骑行水壶架",
                "维修工具：备胎、充气筒、多功能工具，内六角扳手",
                "码表：记录骑行数据，推荐GPS码表",
                "骑行服：排汗性能好，减少风阻",
                "锁鞋：提高踩踏效率，防止脚滑",
                "尾灯：夜间骑行必备，红色闪烁模式",
                "眼镜：防风防虫，阻挡紫外线",
                "护膝：保护膝盖，特别适合长途骑行",
                "驮包：长途骑行装载行李"
        ));

        // 训练计划
        KNOWLEDGE_BASE.put("训练计划", Arrays.asList(
                "初学者第一周：每次骑行30分钟，距离10-15公里",
                "初学者第二周：每次骑行45分钟，距离15-20公里",
                "每次骑行前热身5-10分钟，活动关节",
                "骑行结束后放松拉伸10-15分钟，缓解肌肉酸痛",
                "循序渐进，避免过度训练导致受伤",
                "每周至少休息1-2天，让身体恢复",
                "间歇训练：全力冲刺1分钟，休息2分钟，重复8组",
                "耐力训练：每周一次长距离骑行，逐步增加里程",
                "爬坡训练：选择有坡度的路线，每周1-2次",
                "核心训练：增强腹部和背部力量，提高骑行稳定性",
                "力量训练：深蹲、硬拉增强腿部力量"
        ));

        // 健康与恢复
        KNOWLEDGE_BASE.put("健康与恢复", Arrays.asList(
                "骑行后及时补充碳水化合物和蛋白质",
                "肌肉酸痛时可以采用泡沫轴放松",
                "保证充足睡眠，每晚7-8小时",
                "长途骑行后泡热水澡促进血液循环",
                "补充电解质防止肌肉痉挛",
                "注意膝关节保护，避免过度使用",
                "定期进行身体检查，特别是心脏功能",
                "女性骑友注意会阴部位保护",
                "长时间骑行要经常变换姿势",
                "出现持续疼痛要及时休息和就医"
        ));

        // 天气与应对
        KNOWLEDGE_BASE.put("天气与应对", Arrays.asList(
                "高温骑行：提前补水，避开中午时段",
                "寒冷骑行：分层穿衣，佩戴保暖手套和头巾",
                "雨天骑行：穿防水装备，降低车速",
                "大风骑行：降低风阻，身体贴近车把",
                "沙尘天气：佩戴护目镜，口罩",
                "雾霾天气：减少户外骑行，或戴口罩",
                "雪天骑行：不建议骑行，路面湿滑",
                "夜间骑行：确保灯具充足，穿着反光服装"
        ));

        // 自行车维护
        KNOWLEDGE_BASE.put("自行车维护", Arrays.asList(
                "每次骑行前检查胎压，确保在标准范围内",
                "每周清洁链条并上油",
                "每月检查刹车片磨损情况",
                "每2000公里更换一次刹车片",
                "每5000公里更换一次链条",
                "定期检查辐条张力，保持轮组平衡",
                "注意清洁变速系统，确保换挡顺畅",
                "检查坐垫高度是否合适",
                "定期检查五通轴承，有异响及时保养",
                "长期存放时要给轮胎充气，放置阴凉处"
        ));

        // 长途骑行
        KNOWLEDGE_BASE.put("长途骑行", Arrays.asList(
                "规划详细路线，了解沿途补给点",
                "携带足够的食物和水，至少准备一天的量",
                "使用驮包装载行李，重物放在中间位置",
                "每天骑行距离控制在80-100公里为宜",
                "选择安全的露营地点或住宿点",
                "携带简易维修工具和备用零件",
                "记录骑行数据，方便回顾和分享",
                "与队友保持通讯畅通",
                "预留机动时间，应对意外情况"
        ));
    }

    /**
     * 初始化知识库，将知识条目向量化并存入Redis
     */
    @PostConstruct
    public void initKnowledgeBase() {
        log.info("开始初始化知识库...");
        try {
            int totalEntries = 0;

            for (Map.Entry<String, List<String>> entry : KNOWLEDGE_BASE.entrySet()) {
                String category = entry.getKey();
                List<String> items = entry.getValue();

                for (int i = 0; i < items.size(); i++) {
                    String text = items.get(i);
                    String vectorKey = VECTOR_KEY_PREFIX + category + ":" + i;
                    String textKey = TEXT_KEY_PREFIX + category + ":" + i;

                    // 生成向量并存储
                    float[] vector = embeddingService.embed(text);

                    // 将向量转换为字符串存储
                    String vectorStr = arrayToString(vector);
                    redisTemplate.opsForValue().set(vectorKey, vectorStr);
                    redisTemplate.opsForValue().set(textKey, text);

                    totalEntries++;
                }
            }

            // 存储类别列表
            redisTemplate.opsForSet().add(CATEGORY_KEY, KNOWLEDGE_BASE.keySet().toArray(new String[0]));

            log.info("知识库初始化完成，共加载 {} 条知识", totalEntries);

        } catch (Exception e) {
            log.error("知识库初始化失败: {}", e.getMessage());
            // 如果Redis不可用，使用内存模式
            log.warn("将使用内存模式运行RAG服务");
        }
    }

    /**
     * 获取所有知识类别
     */
    public List<String> getCategories() {
        try {
            Set<String> categories = redisTemplate.opsForSet().members(CATEGORY_KEY);
            if (categories != null && !categories.isEmpty()) {
                return new ArrayList<>(categories);
            }
        } catch (Exception e) {
            log.warn("从Redis获取类别失败: {}", e.getMessage());
        }
        return new ArrayList<>(KNOWLEDGE_BASE.keySet());
    }

    /**
     * 根据向量检索最相关的知识
     */
    public List<KnowledgeEntry> searchByVector(float[] queryVector, int topK) {
        List<KnowledgeEntry> results = new ArrayList<>();

        try {
            // 遍历所有知识条目
            for (String category : KNOWLEDGE_BASE.keySet()) {
                List<String> items = KNOWLEDGE_BASE.get(category);
                for (int i = 0; i < items.size(); i++) {
                    String text = items.get(i);

                    // 尝试从Redis获取向量
                    String vectorKey = VECTOR_KEY_PREFIX + category + ":" + i;
                    String vectorStr = redisTemplate.opsForValue().get(vectorKey);

                    float[] storedVector;
                    if (vectorStr != null) {
                        storedVector = stringToArray(vectorStr);
                    } else {
                        // 如果Redis中没有，重新生成
                        storedVector = embeddingService.embed(text);
                    }

                    // 计算相似度
                    float similarity = embeddingService.cosineSimilarity(queryVector, storedVector);

                    results.add(new KnowledgeEntry(category, text, similarity));
                }
            }

            // 按相似度排序
            results.sort((a, b) -> Float.compare(b.getSimilarity(), a.getSimilarity()));

            // 返回topK结果
            return results.subList(0, Math.min(topK, results.size()));

        } catch (Exception e) {
            log.error("向量检索失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 获取指定类别的所有知识
     */
    public List<KnowledgeEntry> getByCategory(String category) {
        List<KnowledgeEntry> results = new ArrayList<>();
        List<String> items = KNOWLEDGE_BASE.get(category);

        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                results.add(new KnowledgeEntry(category, items.get(i), 0));
            }
        }

        return results;
    }

    /**
     * 关键词搜索（备用方法）
     */
    public List<KnowledgeEntry> searchByKeyword(String keyword) {
        List<KnowledgeEntry> results = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();

        for (Map.Entry<String, List<String>> entry : KNOWLEDGE_BASE.entrySet()) {
            String category = entry.getKey();
            List<String> items = entry.getValue();

            for (String item : items) {
                if (item.toLowerCase().contains(lowerKeyword)) {
                    results.add(new KnowledgeEntry(category, item, 1.0f));
                }
            }
        }

        return results;
    }

    /**
     * 将向量数组转换为字符串
     */
    private String arrayToString(float[] array) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(array[i]);
        }
        return sb.toString();
    }

    /**
     * 将字符串转换为向量数组
     */
    private float[] stringToArray(String str) {
        String[] parts = str.split(",");
        float[] array = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            array[i] = Float.parseFloat(parts[i]);
        }
        return array;
    }

    /**
     * 知识条目
     */
    @Data
    public static class KnowledgeEntry {
        private String category;
        private String content;
        private float similarity;

        public KnowledgeEntry(String category, String content, float similarity) {
            this.category = category;
            this.content = content;
            this.similarity = similarity;
        }
    }
}
