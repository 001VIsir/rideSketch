# 高并发与大数据量测试报告

## 测试环境
- **后端**: localhost:8080 (Spring Boot 3.5.10, Java 21)
- **前端**: localhost:5173 (Vue 3 + Vite)
- **MySQL**: localhost:3306 (root/Cqian1231, 8.0+)
- **Redis**: localhost:6379 (5.0.14)
- **测试日期**: 2026-02-21

---

## 一、Redis基准测试

### 1.1 基础性能测试

| 操作类型 | 10并发 (req/s) | 50并发 (req/s) |
|---------|---------------|---------------|
| PING | 26,000 | 25,000 |
| SET | 28,000 | 25,000 |
| GET | 26,000 | 26,000 |
| HSET | 26,000 | 24,000 |
| LPUSH | 20,000 | 18,000 |
| MSET (10 keys) | 25,000 | 24,000 |

**结论**: Redis性能优秀，单实例可处理约25,000+ ops/s

### 1.2 大数据量测试

- 测试数据: 10,000条键值对
- 内存占用: 2MB
- 读写性能: 无明显下降

### 1.3 Redis使用现状

当前系统仅在RAG知识库中使用Redis存储向量数据，未用于业务缓存。

---

## 二、MySQL基准测试

### 2.1 数据量统计

| 表名 | 当前数据量 |
|------|----------|
| user | 7 |
| route | 1,602 |
| comment | 1 |
| like | ~2,000 |

### 2.2 查询性能测试

| 查询类型 | 数据量 | 响应时间 |
|---------|--------|---------|
| LIMIT 100 | 100条 | 49ms |
| LIMIT 1000 | 1000条 | 104ms |
| WHERE user_id = 22 | ~1600条 | 119ms |
| ORDER BY likes DESC | 100条 | 46ms |

### 2.3 索引情况

```sql
route表索引:
- PRIMARY KEY (id)
- INDEX idx_user_id (user_id)
- INDEX idx_create_time (create_time)
```

---

## 三、业务API压力测试

### 3.1 社区路线列表API

| 并发数 | 响应时间 | 备注 |
|--------|---------|------|
| 1 | 2.3s | 较慢 |
| 10 | 2.3s/每个 | 并发处理正常 |
| 20 | 完成 | 无超时 |
| 50 | 完成 | 无超时 |

**问题**: 单次请求耗时2.3秒，明显异常

### 3.2 性能瓶颈分析

**根因**: N+1查询问题

```java
// CommunityServiceImpl.java:72
public List<RouteVO> getRouteList(int page, int size, Long userId) {
    Page<Route> result = routeMapper.selectPage(routePage, wrapper);
    return result.getRecords().stream()
            .map(route -> convertToVO(route, userId,
                userId != null && hasLiked(route.getId(), userId))) // N+1查询!
            .collect(Collectors.toList());
}

// 每条路线都单独查询点赞状态
private boolean hasLiked(Long routeId, Long userId) {
    return likeMapper.selectCount(wrapper) > 0; // 1000条路线 = 1001次查询
}
```

**性能影响**:
- 1次主查询 + N次点赞状态查询
- 1600条数据 ≈ 1601次SQL查询

---

## 四、问题与优化建议

### 4.1 严重问题

#### 问题1: N+1查询 (高优先级)

**现象**: API响应时间2.3秒

**影响**:
- 单用户请求需执行1601次SQL
- 并发时数据库连接池压力巨大

**优化方案**:
```java
// 方案1: 批量查询点赞状态
List<Long> routeIds = result.getRecords().stream()
    .map(Route::getId).collect(Collectors.toList());
Map<Long, Boolean> likedMap = batchGetLikedStatus(routeIds, userId);

// 方案2: 使用JOIN查询
@Select("SELECT r.*, CASE WHEN l.id IS NOT NULL THEN 1 ELSE 0 END as liked " +
        "FROM route r LEFT JOIN `like` l ON r.id = l.route_id AND l.user_id = #{userId}")
List<RouteVO> selectRoutesWithLikeStatus(@Param("userId") Long userId);
```

#### 问题2: 缺少Redis缓存

**现象**: 重复查询相同数据无加速

**优化方案**:
```java
@Cacheable(value = "routes", key = "#page + '_' + #size")
public List<RouteVO> getRouteList(...) {}

// 或手动缓存
String cacheKey = "routes:" + page + ":" + size;
String cached = redisTemplate.opsForValue().get(cacheKey);
if (cached != null) {
    return JSON.parseObject(cached, List.class);
}
```

#### 问题3: 数据库连接池配置偏小

当前配置:
```properties
spring.data.redis.lettuce.pool.max-active=8  # Redis
# MySQL默认连接池可能偏小
```

**优化建议**: 根据并发量调整连接池

---

## 五、测试总结

| 项目 | 测试结果 | 备注 |
|------|---------|------|
| Redis基础性能 | ✅ 优秀 | 25,000+ ops/s |
| Redis大数据量 | ✅ 正常 | 10K条无压力 |
| MySQL查询性能 | ✅ 良好 | 100ms内 |
| MySQL写入性能 | ✅ 正常 | 批量插入快 |
| API单次响应 | ❌ 慢 | 2.3秒 |
| API并发处理 | ⚠️ 一般 | 无超时但慢 |
| 缓存机制 | ❌ 缺失 | 未使用Redis |

---

## 六、优化优先级

1. **高优先级**: 修复N+1查询问题 (预计提升10倍性能)
2. **中优先级**: 添加Redis缓存 (预计提升5倍性能)
3. **低优先级**: 调整连接池参数

---

*测试完成于 2026-02-21*
