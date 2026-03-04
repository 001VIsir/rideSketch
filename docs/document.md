# rideSketch 骑迹 - 完整项目入门指南

> **特别说明**：本文档面向完全不了解本项目的人，从零开始详细讲解项目的每一个细节。包括为什么这样设计、优缺点分析、与其他方案的区别、业务场景等。

---

## 目录

1. [项目概述与业务背景](#1-项目概述与业务背景)
2. [技术栈选择与版本说明](#2-技术栈选择与版本说明)
3. [数据库设计详解](#3-数据库设计详解)
4. [实体类设计分析](#4-实体类设计分析)
5. [分层架构设计](#5-分层架构设计)
6. [设计模式应用详解](#6-设计模式应用详解)
7. [用户认证模块详解](#7-用户认证模块详解)
8. [地图服务模块详解](#8-地图服务模块详解)
9. [路线规划模块详解](#9-路线规划模块详解)
10. [AI智能路线规划模块](#10-ai智能路线规划模块)
11. [社区功能模块](#11-社区功能模块)
12. [配置与依赖管理](#12-配置与依赖管理)
13. [面试常见问题汇总](#13-面试常见问题汇总)

---

## 1. 项目概述与业务背景

### 1.1 项目定位

**rideSketch（骑迹）** 是一款面向骑行爱好者的智能路线规划与社区分享平台。

### 1.2 业务场景分析

#### 核心用户场景

| 场景 | 描述 | 用户痛点 |
|------|------|----------|
| 路线规划 | 用户规划骑行路线 | 不知道哪些路线好玩、距离多远 |
| AI推荐 | 用自然语言描述需求，AI推荐路线 | 输入地址太麻烦 |
| 社区分享 | 发布骑行路线到社区 | 只是自己保存太单调 |
| 互动 | 点赞、评论其他用户的路线 | 缺乏社交乐趣 |
| 图案骑行 | 骑出特定形状的路线 | 纯粹好玩 |

#### 为什么会选择这个业务？

1. **市场规模大**：骑行是近年快速增长的城市运动
2. **技术结合点好**：地图API + AI = 智能推荐
3. **社区属性强**：天然适合UGC内容

### 1.3 前后端分离架构

```
┌─────────────────────────────────────────────────────────────┐
│                         用户 (浏览器)                        │
│                     http://localhost:5173                    │
└────────────────────────────┬────────────────────────────────┘
                             │ HTTP REST API (JSON)
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                     后端 Spring Boot                         │
│                      http://localhost:8080                  │
├─────────────────────────────────────────────────────────────┤
│  Controller (API入口)                                        │
│      ↓                                                       │
│  Service (业务逻辑)                                          │
│      ↓                                                       │
│  Mapper (数据访问)                                           │
└──────────────┬──────────────────────┬───────────────────────┘
               │                      │
               ▼                      ▼
        ┌──────────┐           ┌──────────┐
        │  MySQL   │           │  Redis   │
        │ 持久化   │           │  缓存    │
        └──────────┘           └──────────┘
               │
               ▼
        ┌──────────────────────────┐
        │    高德地图 API          │
        │    (第三方服务)           │
        └──────────────────────────┘
               │
               ▼
        ┌──────────────────────────┐
        │    Ollama AI服务         │
        │    (本地大模型)           │
        └──────────────────────────┘
```

#### 为什么要用前后端分离？

| 对比项 | 前后端混合 (JSP/Thymeleaf) | 前后端分离 |
|--------|--------------------------|------------|
| 开发效率 | 前后端互相等待 | 独立开发 |
| 部署 | 必须一起部署 | 独立部署 |
| 技术选型 | 受限 | 自由 |
| 性能 | 差 | 好 |
| 本项目 | ❌ 不适合 | ✅ 适合（Vue 3 + Spring Boot） |

---

## 2. 技术栈选择与版本说明

### 2.1 技术栈总览

```xml
<!-- pom.xml 中的版本定义 -->
<properties>
    <java.version>17</java.version>
    <mybatis-plus.version>3.5.5</mybatis-plus.version>
    <jjwt.version>0.12.5</jjwt.version>
</properties>
```

#### 核心技术选型原因

| 技术 | 版本 | 为什么选择 | 替代方案 |
|------|------|-----------|----------|
| Java | 17 | LTS版本，性能好，支持新特性 | Java 11 / 21 |
| Spring Boot | 3.5.10 | 生态最强，自动配置 | Quarkus / Micronaut |
| MyBatis-Plus | 3.5.5 | CRUD零SQL，减少样板代码 | MyBatis / JPA |
| JWT | 0.12.5 | 无状态认证 | Session / OAuth2 |
| MySQL | 8.x | 成熟稳定 | PostgreSQL |
| Redis | - | 缓存首选 | Memcached |
| Spring Security | - | 安全框架 | Shiro |
| Spring AI | 1.0.0-M4 | AI集成 | 直接调用Ollama API |

### 2.2 Spring Boot 3.x 的重要变化

```java
// Spring Boot 2.x 的导入
import javax.servlet.*;

// Spring Boot 3.x 的导入
import jakarta.servlet.*;
```

**注意**：Spring Boot 3.x 要求使用 Jakarta EE 9+（即 `jakarta.*` 包名），这是因为 Java EE 规范已移交给 Eclipse Foundation 并重命名为 Jakarta EE。

### 2.3 为什么不用 Spring Data JPA？

**JPA 缺点**：
- 复杂查询能力弱
- 调试困难（生成的SQL不直观）
- 学习曲线陡峭

**MyBatis-Plus 优点**：
- SQL可控
- 性能更好
- 适合复杂业务
- 中文文档丰富

---

## 3. 数据库设计详解

### 3.1 数据库选择 MySQL 而非 PostgreSQL

| 对比 | MySQL | PostgreSQL |
|------|-------|------------|
| 读写性能 | ✅ 更好 | 一般 |
| JSON支持 | 一般 | ✅ 强大 |
| 事务 | ✅ | ✅ |
| 社区 | ✅ 中文友好 | 一般 |
| 本项目场景 | ✅ 够用 | 过度设计 |

**业务分析**：
- 本项目数据主要是结构化的路线、用户信息
- 不需要复杂的JSON查询
- MySQL完全满足需求

### 3.2 核心表结构

#### 3.2.1 用户表 (`user`)

```sql
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(50) NOT NULL,
    `email` VARCHAR(100),
    `password` VARCHAR(255) NOT NULL,     -- BCrypt加密存储
    `nickname` VARCHAR(50),
    `avatar` VARCHAR(500),
    `status` TINYINT DEFAULT 1,          -- 0=禁用, 1=正常
    `create_time` DATETIME,
    `update_time` DATETIME,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
);
```

**为什么这样设计？**

| 字段 | 设计决策 | 理由 |
|------|---------|------|
| `id` BIGINT | 用BIGINT不用INT | 支持更大用户量，INT上限21亿 |
| `username` UNIQUE | 用户名唯一 | 登录凭证之一 |
| `email` UNIQUE | 邮箱唯一 | 登录凭证/找回密码 |
| `password` VARCHAR(255) | 存加密后的密文 | BCrypt结果最长60字符，255足够 |
| `status` TINYINT | 软禁用 | 不用删除数据，可恢复 |
| `create_time`/`update_time` | 自动维护 | 审计需要 |

#### 3.2.2 路线表 (`route`)

```sql
CREATE TABLE `route` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `description` TEXT,
    `start_point` VARCHAR(200),           -- 起点文本描述
    `end_point` VARCHAR(200),             -- 终点文本描述
    `waypoints` TEXT,                     -- JSON数组格式
    `route_path` TEXT,                    -- 路线坐标轨迹
    `total_distance` DECIMAL(10,2),        -- 总距离(公里)
    `estimated_time` INT,                 -- 预计时间(分钟)
    `difficulty` TINYINT,                 -- 1=简单, 2=中等, 3=困难
    `tags` VARCHAR(500),                  -- JSON数组 ["风景", "休闲"]
    `likes` INT DEFAULT 0,
    `views` INT DEFAULT 0,
    `is_public` TINYINT DEFAULT 1,        -- 0=私有, 1=公开
    `create_time` DATETIME,
    `update_time` DATETIME,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_create_time` (`create_time`)
);
```

**为什么要存 `waypoints` 和 `route_path` 两个字段？**

```
waypoints = 用户设置的途经点
["天安门", "故宫", "景山"]

route_path = 实际骑行轨迹坐标点
"116.397,39.908;116.398,39.909;..."
```

**为什么用 TEXT 存 JSON 而不是 JSON 类型？**

- MySQL 5.7+ 支持 JSON 类型
- 但 TEXT 兼容性更好，且本项目不需要在数据库层面查询 JSON 字段

#### 3.2.3 点赞表 (`like`)

```sql
CREATE TABLE `like` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `route_id` BIGINT NOT NULL,
    `create_time` DATETIME,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_route` (`user_id`, `route_id`)
);
```

**为什么用 UNIQUE 约束？**

- 业务上每个用户只能点赞一次
- 防止重复插入，用数据库层面保证

**为什么不在 route 表存点赞数？**

```
route.likes 字段 - 冗余存储，提高查询性能
like 表 - 记录详情，支持取消点赞
```

**这种设计叫"反范式化"**，用空间换时间：

- 查路线列表时：直接读 `route.likes`
- 查是否点赞过：查 `like` 表
- 点赞/取消：同时更新两个地方

#### 3.2.4 评论表 (`comment`)

```sql
CREATE TABLE `comment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `route_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `parent_id` BIGINT DEFAULT 0,         -- 0=主评论，其他=回复
    `content` TEXT NOT NULL,
    `create_time` DATETIME,
    `update_time` DATETIME
);
```

**为什么用 `parent_id` 实现楼中楼？**

| 方案 | 优点 | 缺点 |
|------|------|------|
| 邻接表 (parent_id) | 简单，插入快 | 查询递归，层级深时慢 |
| 路径枚举 | 查询快 | 插入更新复杂 |
| 闭包表 | 查询最快 | 实现复杂 |

**本项目选择邻接表原因**：
- 评论层级不会太深（一般1-2层）
- 实现简单，够用

---

## 4. 实体类设计分析

### 4.1 User 实体类

```java
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String email;
    private String password;          // 存BCrypt加密后的密文
    private String nickname;
    private String avatar;

    @TableField("status")
    private Integer status;           // 0=禁用, 1=正常

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

**为什么要用 `LocalDateTime` 而非 `Date`？**

```java
// Date (旧API)
Date now = new Date();  // 可变对象，线程不安全

// LocalDateTime (Java 8+ 新API)
LocalDateTime now = LocalDateTime.now();  // 不可变，线程安全，更清晰
```

**为什么用 `@TableField` 注解？**

```java
// 数据库字段是 snake_case，Java属性是 camelCase
// MyBatis-Plus 默认自动映射
// 但 status 是关键字，需要指定

@TableField("status")  // 明确指定，避免SQL关键字冲突
private Integer status;
```

**为什么要配置自动填充？**

```java
// 手动写法（繁琐）
user.setCreateTime(LocalDateTime.now());
userMapper.insert(user);

// 自动填充（优雅）
@TableField(value = "create_time", fill = FieldFill.INSERT)
private LocalDateTime createTime;
```

### 4.2 Route 实体类

```java
@Data
@TableName("route")
public class Route {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;              // 外键关联
    private String title;
    private String description;
    private String startPoint;       // 起点
    private String endPoint;          // 终点
    private String waypoints;        // 途经点(JSON)
    private String routePath;        // 轨迹(JSON)
    private BigDecimal totalDistance;// 用BigDecimal不用double
    private Integer estimatedTime;
    private Integer difficulty;      // 1=简单, 2=中等, 3=困难
    private String tags;             // 标签(JSON)
    private Integer likes;           // 冗余字段
    private Integer views;           // 冗余字段
    private Integer isPublic;        // 0=私有, 1=公开
    // ... 时间字段
}
```

**为什么 `totalDistance` 用 `BigDecimal` 而非 `double`？**

```java
// double 的问题
double a = 0.1;
double b = 0.2;
System.out.println(a + b);  // 0.30000000000000004 ❌

// BigDecimal 的正确用法
BigDecimal a = new BigDecimal("0.1");
BigDecimal b = new BigDecimal("0.2");
System.out.println(a.add(b));  // 0.3 ✅

// 金额/距离等精确计算场景必须用 BigDecimal
```

### 4.3 Like 实体类

```java
@Data
@TableName("`like`")  // 加反引号，因为 LIKE 是SQL关键字
public class Like {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long routeId;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
```

**为什么表名要加反引号？**

```sql
-- LIKE 是 SQL 关键字，直接写会报错
CREATE TABLE `like`;  -- ✅ 正确
CREATE TABLE like;    -- ❌ 错误
```

---

## 5. 分层架构设计

### 5.1 为什么需要分层？

```
不分层的问题：
├── 业务逻辑和数据库操作混在一起
├── 难以测试
├── 代码重复
├── 维护困难
└── 无法复用

分层后的好处：
├── 职责清晰
├── 便于测试
├── 代码复用
├── 独立演进
└── 团队协作
```

### 5.2 本项目的分层结构

```
src/main/java/org/example/ridesketch/
├── controller/      【控制层】接收请求，调用Service，返回响应
│
├── service/         【业务层】处理业务逻辑
│   └── impl/        【实现类】具体业务逻辑
│
├── mapper/          【持久层】数据库操作
│
├── entity/          【实体层】数据库表映射对象
│
├── dto/             【数据传输对象】请求/响应对象
│
├── config/          【配置层】Spring配置类
│
├── security/        【安全层】认证授权相关
│
└── common/          【公共层】通用工具类
```

### 5.3 各层职责详解

#### Controller 层

```java
@RestController                    // @Controller + @ResponseBody
@RequestMapping("/api/auth")       // 路由前缀
@RequiredArgsConstructor           // 构造器注入
public class UserController {

    private final UserService userService;  // 依赖业务层

    @PostMapping("/register")       // POST /api/auth/register
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        // 1. 参数校验 (@Valid)
        // 2. 调用业务逻辑
        // 3. 返回响应
    }
}
```

**为什么用 `@Valid`？**

```java
// 没有 @Valid
@PostMapping("/register")
public void register(RegisterRequest req) {
    // 用户传空用户名，照样插入数据库 ❌
}

// 有 @Valid
@PostMapping("/register")
public void register(@Valid RegisterRequest req) {
    // 自动校验，失败返回400错误 ✅
}

// RegisterRequest 的校验注解
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Email
    private String email;
}
```

#### Service 层

```java
@Service              // Spring管理的Bean
@RequiredArgsConstructor  // 生成构造器
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    @Override
    public User register(RegisterRequest req) {
        // 1. 检查用户名是否存在
        if (userMapper.selectOne(...) != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 2. 加密密码
        String encodedPassword = passwordEncoder.encode(req.getPassword());

        // 3. 创建用户
        User user = new User();
        user.setPassword(encodedPassword);
        userMapper.insert(user);

        return user;
    }
}
```

**为什么用接口 + 实现类？**

```java
// 接口
public interface UserService {
    User register(RegisterRequest req);
    AuthResponse login(LoginRequest req);
}

// 实现
@Service
public class UserServiceImpl implements UserService { ... }
```

**好处**：
1. 便于测试（可以注入Mock实现）
2. 切换实现（如从MySQL切到MongoDB）
3. 解耦

#### Mapper 层

```java
@Mapper  // MyBatis扫描
public interface UserMapper extends BaseMapper<User> {
    // 基础CRUD已内置：
    // selectById(), insert(), updateById(), deleteById()
    // 无需写一行SQL！
}
```

**MyBatis-Plus 内置方法**：

| 方法 | SQL | 说明 |
|------|-----|------|
| `insert(entity)` | INSERT | 插入 |
| `updateById(entity)` | UPDATE | 更新 |
| `deleteById(id)` | DELETE | 删除 |
| `selectById(id)` | SELECT | 查询 |
| `selectList(wrapper)` | SELECT | 条件查询 |
| `selectPage(page, wrapper)` | SELECT | 分页查询 |

**为什么不需要写SQL？**

- `BaseMapper<User>` 泛型指定了表名
- 根据实体类属性自动映射
- MyBatis-Plus 根据数据库方言生成SQL

---

## 6. 设计模式应用详解

> **特别说明**：本章节详细介绍项目中使用的设计模式，讲解为什么使用这些模式、业务场景、以及代码实现。

### 6.1 策略模式 (Strategy Pattern)

#### 什么是策略模式？

策略模式是一种行为型设计模式，它定义了一系列算法，把它们一个个封装起来，并且使它们可以相互替换。策略模式使得算法可以独立于使用它的客户端而变化。

**策略模式结构**：
```
         ┌─────────────────────┐
         │   <<interface>>     │
         │   策略接口          │  ← 定义统一行为
         └──────────┬──────────┘
                    │
     ┌──────────────┼──────────────┐
     │              │              │
     ▼              ▼              ▼
┌─────────┐   ┌───────────┐   ┌───────────┐
│ 策略A   │   │  策略B    │   │  策略C    │
└─────────┘   └───────────┘   └───────────┘
      ▲            ▲             ▲
      └────────────┴─────────────┘
                   │
         ┌─────────┴─────────┐
         │   Context (上下文) │  ← 调度者/客户端
         └───────────────────┘
```

#### 业务场景：为什么需要策略模式？

在 rideSketch 项目中，路线规划有多种不同的实现方式：

| 场景 | 描述 | 算法差异 |
|------|------|----------|
| 普通路线 | 调用高德API的基础骑行/步行路线 | 直接API调用 |
| AI路线 | LLM理解自然语言 + 智能POI推荐 | AI语义理解 + API调用 |
| 图案路书 | 生成图形形状的骑行路线 | 图形算法 + API调用 |

**不用策略模式的问题**：
```java
// 如果不用策略模式，会写成这样
@Service
public class BadRouteService {

    public RoutePlanningResult planRoute(RoutePlanningRequest request) {
        if ("AI".equals(request.getType())) {
            // AI路线逻辑...
        } else if ("PATTERN".equals(request.getType())) {
            // 图案路书逻辑...
        } else {
            // 普通路线逻辑...
        }
    }
}
```

**问题**：
1. 违反开闭原则：新增路线类型需要修改现有代码
2. 代码耦合度高：所有逻辑混在一起
3. 难以测试：无法单独测试某个算法
4. 难以复用：算法无法被其他模块复用

#### 项目中的策略模式实现

**第一步：定义策略接口**

```java
// src/main/java/org/example/ridesketch/service/RouteService.java
public interface RouteService {
    /**
     * 路线规划
     */
    RoutePlanningResult planRoute(RoutePlanningRequest request);

    /**
     * 骑行路线规划
     */
    RoutePlanningResult planRidingRoute(String origin, String destination, String waypoints);

    /**
     * 步行路线规划
     */
    RoutePlanningResult planWalkingRoute(String origin, String destination, String waypoints);
}
```

```java
// src/main/java/org/example/ridesketch/service/AIRouteService.java
public interface AIRouteService {
    /**
     * AI智能路线规划
     */
    AIRoutePlanningResult planAIRoute(AIRoutePlanningRequest request);
}
```

```java
// src/main/java/org/example/ridesketch/service/PatternRouteService.java
public interface PatternRouteService {
    /**
     * 生成图案路书
     */
    PatternRouteResult generatePatternRoute(PatternRouteRequest request);
}
```

**第二步：实现具体策略**

```java
// 普通路线策略实现
@Service
public class RouteServiceImpl implements RouteService {
    // 调用高德API实现骑行/步行路线规划
}
```

```java
// AI路线策略实现
@Service
public class AIRouteServiceImpl implements AIRouteService {
    private final ChatClient chatClient;
    private final RouteService routeService;
    private final RestTemplate restTemplate;

    // 使用LLM理解用户输入，调用高德API
}
```

```java
// 图案路书策略实现
@Service
public class PatternRouteServiceImpl implements PatternRouteService {
    // 生成图形形状的骑行路线
}
```

**第三步：Context（调度者）使用策略**

```java
// src/main/java/org/example/ridesketch/controller/RouteController.java
@RestController
@RequestMapping("/api/route")
@RequiredArgsConstructor
public class RouteController {

    // 注入三个策略
    private final RouteService routeService;                    // 策略1: 普通路线
    private final AIRouteService aiRouteService;                // 策略2: AI路线
    private final PatternRouteService patternRouteService;     // 策略3: 图案路书

    // 使用普通路线策略
    @PostMapping("/plan")
    public Result<RoutePlanningResult> planRoute(@RequestBody RoutePlanningRequest request) {
        RoutePlanningResult result = routeService.planRoute(request);
        return Result.success(result);
    }

    // 使用AI路线策略
    @PostMapping("/ai-plan")
    public Result<AIRoutePlanningResult> planAIRoute(@RequestBody AIRoutePlanningRequest request) {
        AIRoutePlanningResult result = aiRouteService.planAIRoute(request);
        return Result.success(result);
    }

    // 使用图案路书策略
    @PostMapping("/pattern")
    public Result<PatternRouteResult> generatePatternRoute(@RequestBody PatternRouteRequest request) {
        PatternRouteResult result = patternRouteService.generatePatternRoute(request);
        return Result.success(result);
    }
}
```

#### 策略模式在文本分块中的应用

RAG知识库模块中，文本分块也使用了策略模式：

```java
// src/main/java/org/example/ridesketch/service/TextChunker.java

// 策略枚举
public enum ChunkStrategy {
    FIXED_SIZE,      // 固定大小分块
    SLIDING_WINDOW,  // 滑动窗口分块
    SEMANTIC         // 语义段落分块
}

// 统一入口 - 使用 switch 表达式选择策略
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
```

**三种分块策略对比**：

| 策略 | 切分方式 | 适用场景 |
|-----|---------|---------|
| `fixedSizeChunk` | 按固定字符数切分 | 结构化文档 |
| `slidingWindowChunk` | 窗口滑动，有重叠 | 需要上下文连续性 |
| `semanticChunk` | 按句子/段落切分 | 自然语言文章 |

#### 策略模式的优点

| 优点 | 说明 |
|------|------|
| **开闭原则** | 新增策略不修改现有代码 |
| **解耦** | 客户端与具体实现分离 |
| **可替换性** | 运行时切换算法 |
| **可测试** | 每个策略可以单独测试 |
| **消除if-else** | 用多态替代条件判断 |

#### 策略模式的缺点

| 缺点 | 说明 |
|------|------|
| **类数量增加** | 每个策略需要单独类 |
| **客户端需要了解策略** | 需要知道不同策略的区别 |
| **复杂度增加** | 适度使用，避免过度设计 |

---

### 6.2 其他设计模式

#### 门面模式 (Facade Pattern)

统一响应格式：

```java
// src/main/java/org/example/ridesketch/common/Result.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> Result<T> success(T data) {
        return new Result<>(true, "操作成功", data);
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(false, message, null);
    }
}
```

**作用**：统一API响应格式，简化客户端处理。

#### 依赖注入 (Dependency Injection)

Spring Boot核心模式：

```java
@Service
@RequiredArgsConstructor  // Lombok自动生成构造函数
public class AIRouteServiceImpl implements AIRouteService {
    private final ChatClient chatClient;
    private final RouteService routeService;
    private final RestTemplate restTemplate;
}
```

**作用**：解耦、可测试、灵活替换实现。

#### 管道模式 (Pipeline Pattern)

RAG服务中的检索流程：

```
知识库 → 向量检索 → MMR重排 → LLM生成
```

```java
public String answerQuestion(String question) {
    // 1. 向量检索
    String context = searchRelevantKnowledge(question);

    // 2. MMR重排
    List<KnowledgeEntry> reranked = resultReranker.mmrRerank(entries, keyword, 5);

    // 3. LLM生成
    String answer = generateAnswer(question, context);

    return answer;
}
```

**作用**：流程清晰、模块化、可组合。

---

### 6.3 设计模式选择原则

| 场景 | 推荐模式 |
|------|---------|
| 多种算法可互换 | 策略模式 |
| 统一接口响应 | 门面模式 |
| 依赖管理 | 依赖注入 |
| 多步骤处理流程 | 管道模式 |
| 单一入口 | 单例模式 |

---

## 7. 用户认证模块详解

### 6.1 认证流程概览

```
用户注册流程：
┌─────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────┐
│ 用户输入 │ -> │ Controller  │ -> │ Service     │ -> │ 加密密码│
│ 信息     │    │ 接收请求    │    │ 业务处理    │    │ 存库    │
└─────────┘    └─────────────┘    └─────────────┘    └─────────┘

用户登录流程：
┌─────────┐    ┌─────────────┐    ┌──────────────┐    ┌─────────┐
│ 用户名   │ -> │ Spring      │ -> │ 验证密码     │ -> │ 返回    │
│ 密码     │    │ Security    │    │ 生成JWT      │    │ JWT    │
└─────────┘    └─────────────┘    └──────────────┘    └─────────┘
```

### 6.2 为什么选择 JWT 而不是 Session？

#### Session 认证流程

```
用户登录 -> 服务器创建Session -> 返回SessionID ->
浏览器Cookie存SessionID ->
后续请求带Cookie -> 服务器查Session -> 验证用户
```

| 对比 | Session | JWT |
|------|---------|-----|
| 服务端存储 | 需要 | 不需要 |
| 扩展性 | 差（需共享Session） | 好 |
| 跨域 | 难 | 简单 |
| 移动端 | 难 | 简单 |
| 性能 | 每次查Session | 验证签名即可 |
| 缺点 | 服务器压力大 | 无法主动失效 |

#### 本项目的选择理由

1. **无状态**：服务器不存储Session，水平扩展方便
2. **跨域**：移动端/H5都能轻松使用
3. **简单**：比OAuth2等方案轻量

### 6.3 JWT 详解

#### JWT 结构

```
JWT = Header.Payload.Signature

Header:
{
  "alg": "HS256",   // 算法
  "typ": "JWT"      // 类型
}

Payload:
{
  "userId": 1,
  "username": "zhangsan",
  "iat": 1700000000,    // 签发时间
  "exp": 1700086400     // 过期时间
}

Signature:
HMACSHA256(
  base64UrlEncode(Header) + "." + base64UrlEncode(Payload),
  secret_key
)
```

#### JWT 生成 (JwtUtils.java)

```java
public String generateToken(Long userId, String username) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);      // 存用户ID
    claims.put("username", username);  // 存用户名

    return Jwts.builder()
            .claims(claims)             // Payload
            .subject(username)         // Subject（通常存用户名）
            .issuedAt(now)              // 签发时间
            .expiration(expiryDate)    // 过期时间
            .signWith(getSigningKey()) // 签名
            .compact();
}
```

**为什么存 userId 和 username 两个？**

```
userId: 用于数据库查询（数字比较快）
username: 用于日志/显示
```

#### JWT 验证

```java
public boolean validateToken(String token) {
    try {
        Claims claims = getClaimsFromToken(token);
        return !isTokenExpired(token);  // 检查是否过期
    } catch (Exception e) {
        return false;  // 任何异常都视为无效
    }
}
```

### 6.4 Spring Security 集成

#### SecurityConfig 核心配置

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .csrf(AbstractHttpConfigurer::disable)   // 1. 禁用CSRF
            .cors(cors -> cors.configurationSource(...)) // 2. 启用CORS
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 3. 无状态
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()  // 公开接口
                .anyRequest().authenticated()                  // 其他需认证
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class) // 4. JWT过滤器
            .build();
    }
}
```

**为什么要禁用 CSRF？**

```
传统表单：CSRF token 防止跨站请求
API场景：使用JWT，无需CSRF防护

原因：JWT在Header中，不是Cookie
即使Cookie被发送，也有CORS限制保护
```

**为什么要设置 STATELESS？**

```
STATELESS = 不创建HttpSession
每次请求都是独立的，验证JWT即可
```

**为什么 JWT Filter 要在 UsernamePasswordAuthenticationFilter 之前？**

```
Filter 顺序：
1. JwtAuthenticationFilter (先验证Token)
2. UsernamePasswordAuthenticationFilter (再验证用户名密码)

如果先验证用户名密码，那登录请求也需要Token（矛盾）
所以登录接口要设为 permitAll()
```

### 6.5 密码加密

```java
// 存密码时
passwordEncoder.encode("123456");
// 结果: $2a$10$X5wFu...  (60字符)

// 验证密码时
passwordEncoder.matches("123456", "$2a$10$X5wFu...");
```

**为什么用 BCrypt？**

| 算法 | 速度 | 盐 | 安全性 |
|------|------|-----|--------|
| MD5 | 快 | 需手动 | ❌ 可破解 |
| SHA | 快 | 需手动 | ❌ 可破解 |
| BCrypt | 慢 | 自动 | ✅ 安全 |

BCrypt 特点：
- 自动生成随机盐
- 不可逆
- 可配置工作因子（默认10）
- 每次加密结果不同

### 6.6 登录认证流程详解

```java
// UserServiceImpl.login()
public AuthResponse login(LoginRequest req) {
    // 1. 使用Spring Security验证
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            req.getUsernameOrEmail(),  // 可用用户名或邮箱登录
            req.getPassword()
        )
    );

    // 2. 查询用户（支持用户名或邮箱）
    User user = findByUsername(...);
    if (user == null) user = findByEmail(...);

    // 3. 检查状态
    if (user.getStatus() == 0) {
        throw new RuntimeException("账号已被禁用");
    }

    // 4. 生成Token
    String token = jwtUtils.generateToken(user.getId(), user.getUsername());

    // 5. 返回
    return AuthResponse.builder()
        .token(token)
        .userId(user.getId())
        ...
        .build();
}
```

**为什么先认证再查询用户？**

```
Security认证会：
1. 调用 UserDetailsService.loadUserByUsername()
2. 验证密码是否匹配
3. 检查账号是否启用

如果认证失败，直接抛异常，不需要查库
```

---

## 8. 地图服务模块详解

### 7.1 高德地图 API 选择

| 地图服务商 | 优点 | 缺点 |
|-----------|------|------|
| 高德 | 中文支持好，API丰富 | - |
| 百度 | 国内精度高 | 坐标系特殊 |
| 腾讯 | 性价比好 | 覆盖略弱 |

**为什么选择高德？**
- 中文POI数据最全
- 骑行路线规划API支持好
- 文档中文

### 7.2 三大地图服务

#### 7.2.1 地理编码 (Geocode) - 地址转坐标

```
输入：北京市天安门
输出：116.397128,39.916527
```

```java
@GetMapping("/geocode")
public Result<GeoCodeResult> geocode(@RequestParam String address) {
    // 高德API: https://restapi.amap.com/v3/geocode/geo?key=xxx&address=北京市天安门
    return Result.success(mapService.geocode(address));
}
```

#### 7.2.2 逆地理编码 (ReGeocode) - 坐标转地址

```
输入：116.397128,39.916527
输出：北京市东城区天安门
```

```java
@GetMapping("/regeocode")
public Result<ReGeoCodeResult> reGeocode(
    @RequestParam String longitude,
    @RequestParam String latitude) {
    return Result.success(mapService.reGeocode(longitude, latitude));
}
```

#### 7.2.3 关键词搜索 (Place) - 搜索POI

```
输入：天安门
输出：[{"name":"天安门","location":"116.397128,39.916527",...}]
```

```java
@GetMapping("/search")
public Result<AddressSearchResult> searchAddress(
    @RequestParam String keyword,
    @RequestParam(required = false) String city) {
    return Result.success(mapService.searchAddress(keyword, city));
}
```

### 7.3 为什么用 RestTemplate 而不是 HttpClient？

```java
// RestTemplate (Spring内置)
RestTemplate restTemplate = new RestTemplate();
ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

// HttpClient (Apache)
CloseableHttpClient client = HttpClients.createDefault();
HttpGet request = new HttpGet(url);
CloseableHttpResponse response = client.execute(request);
```

| 对比 | RestTemplate | HttpClient |
|------|-------------|------------|
| 复杂性 | 简单 | 复杂 |
| 连接池 | 需手动配置 | 需手动配置 |
| Spring集成 | 原生 | 需适配 |
| 性能 | 略差 | 略好 |

**结论**：RestTemplate 足够用，且代码更简洁。如果追求极致性能可换 HttpClient。

### 7.4 地图服务实现详解

```java
@Service
public class MapServiceImpl implements MapService {

    private static final String AMAP_BASE_URL = "https://restapi.amap.com/v3";

    @Value("${amap.key}")
    private String amapKey;

    @Override
    public AddressSearchResult searchAddress(String keyword, String city) {
        String url = AMAP_BASE_URL + "/place/text?key=" + amapKey
            + "&keywords=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return parseAddressSearchResult(response.getBody());
    }
}
```

**为什么要 URLEncoder.encode？**

```
用户输入：北京天安门
URL需要：%E5%8C%97%E4%BA%AC%E5%A4%A9%E5%AE%89%E9%97%A8

不编码会报错，甚至有安全风险
```

---

## 9. 路线规划模块详解

### 8.1 高德路径规划API

```
骑行：https://restapi.amap.com/v4/direction/bicycling?origin=xxx&destination=xxx
步行：https://restapi.amap.com/v4/direction/walking?origin=xxx&destination=xxx
```

### 8.2 单段路线规划

```java
// 无途经点，直接调用API
private RoutePlanningResult planSingleSegmentRoute(
    String origin, String destination, String mode) {

    String url = "https://restapi.amap.com/v4/direction/" + mode
        + "?key=" + amapKey
        + "&origin=" + origin        // 格式：经度,纬度
        + "&destination=" + destination;

    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
    return parseRoutePlanningResult(response.getBody());
}
```

### 8.3 多途经点规划 - 分段策略

**问题**：高德API不支持直接传多个途经点

**解决**：分段规划，然后合并

```
起点A -> 途经点1 -> 途经点2 -> 终点B

方案：
1. A -> 途经点1 (一段)
2. 途经点1 -> 途经点2 (一段)
3. 途经点2 -> B (一段)

最后合并所有段的距离、时间、路径坐标
```

```java
private RoutePlanningResult planMultiSegmentRoute(...) {
    // 1. 解析途经点
    String[] waypointArray = waypoints.split("\\|");  // 用|分隔
    List<String> allPoints = new ArrayList<>();
    allPoints.add(origin);
    allPoints.addAll(Arrays.asList(waypointArray));
    allPoints.add(destination);

    // 2. 分段调用API
    List<RoutePlanningResult> segments = new ArrayList<>();
    for (int i = 0; i < allPoints.size() - 1; i++) {
        RoutePlanningResult seg = planSingleSegmentRoute(
            allPoints.get(i),
            allPoints.get(i + 1),
            mode
        );
        segments.add(seg);
    }

    // 3. 合并结果
    return mergeSegmentResults(segments, origin, destination, waypoints);
}
```

### 8.4 结果合并算法

```java
private RoutePlanningResult mergeSegmentResults(
    List<RoutePlanningResult> segments,
    String origin, String destination, String waypoints) {

    long totalDistance = 0;
    long totalDuration = 0;
    List<StepInfo> allSteps = new ArrayList<>();
    StringBuilder allPath = new StringBuilder();

    for (RoutePlanningResult seg : segments) {
        PathInfo path = seg.getRoute().getPaths().get(0);

        totalDistance += Long.parseLong(path.getDistance());
        totalDuration += Long.parseLong(path.getDuration());

        allSteps.addAll(path.getSteps());

        if (allPath.length() > 0) allPath.append(";");
        allPath.append(path.getPath());
    }

    // 构建合并结果
    return RoutePlanningResult.builder()
        .status("1")
        .route(RouteInfo.builder()
            .origin(origin)
            .destination(destination)
            .waypoints(waypoints)
            .paths(List.of(PathInfo.builder()
                .distance(String.valueOf(totalDistance))
                .duration(String.valueOf(totalDuration))
                .steps(allSteps)
                .path(allPath.toString())
                .build()))
            .build())
        .build();
}
```

### 8.5 为什么要用 String 存储坐标而不是 List？

```java
// 存数据库（字符串）
route_path: "116.397,39.908;116.398,39.909;116.399,39.910"

// 前端解析
const points = "116.397,39.908;116.398,39.909".split(";")
    .map(p => p.split(",").map(Number));
```

**原因**：
- 数据库存字符串比存JSON更紧凑
- 前端解析成本可接受
- 避免JSON类型兼容问题

---

## 10. AI智能路线规划模块

### 9.1 架构设计

```
用户输入：
"我想从天安门骑到颐和园，看看风景"

处理流程：
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  1. AI解析   │ -> │  2. 地理编码  │ -> │  3. AI推荐  │
│  提取起点/   │    │  地址->坐标   │    │  途经点      │
│  终点/偏好  │    │              │    │              │
└──────────────┘    └──────────────┘    └──────────────┘
                                                 │
                    ┌──────────────┐             │
                    │  5. 路径规划 │ <───────────┘
                    │  生成路线    │
                    └──────────────┘
```

### 9.2 Spring AI 集成

```java
@Configuration
public class AIConfig {
    @Bean
    @ConditionalOnMissingBean
    public ChatClient chatClient(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
```

**为什么用 Spring AI 而不是直接调用 Ollama？**

| 对比 | 直接调用Ollama | Spring AI |
|------|---------------|-----------|
| 代码量 | 多 | 少 |
| 错误处理 | 需手写 | 内置 |
| 重试 | 需手写 | 可配置 |
| 响应转换 | 需手写 | 自动 |

### 9.3 AI解析用户输入

```java
private String parseUserInput(String userInput) {
    String systemPrompt = "你是一个骑行路线规划助手...";

    String userPrompt = """
        请分析以下骑行路线需求:
        用户需求: {description}
        """;

    return chatClient.prompt()
        .system(systemPrompt)
        .user(userPrompt)
        .call()
        .content();
}
```

**为什么要用 System Prompt？**

```
System Prompt = 设定AI的角色和行为
User Prompt = 用户的具体请求

效果：
System: "你是一个骑行路线规划助手"
User: "从天安门到颐和园"

vs

System: "你是一个厨师"
User: "从天安门到颐和园"
```

### 9.4 AI响应容错处理

```java
private String extractJsonFromResponse(String response) {
    // 1. 尝试找到JSON块
    Pattern jsonPattern = Pattern.compile("\\{[\\s\\S]*\\}");
    Matcher matcher = jsonPattern.matcher(response);

    if (matcher.find()) {
        return matcher.group();  // 提取JSON
    }

    return response;  // 没有JSON也返回
}
```

**为什么需要容错？**
- AI输出不一定是标准JSON
- 可能带markdown格式 ```json ... ```
- 可能输出不完整

### 9.5 RAG 知识库问答（向量检索版）

```java
// RagServiceImpl.java

// 知识库结构（源数据在内存）
private static final Map<String, List<String>> KNOWLEDGE_BASE = new HashMap<>();
static {
    KNOWLEDGE_BASE.put("骑行技巧", Arrays.asList(
        "保持正确的骑行姿势，身体略微前倾约15-30度，肘部微屈",
        "踏频保持在80-100 RPM之间最省力效率最高",
        // ... 共97条知识，9个类别
    ));
    KNOWLEDGE_BASE.put("骑行安全", Arrays.asList(...));
    // ...
}

// 向量检索核心逻辑
private String searchRelevantKnowledge(String question) {
    // 1. 使用 EmbeddingService 生成问题向量
    float[] queryVector = embeddingService.embed(question);

    // 2. 从 Redis 中检索相似向量（余弦相似度）
    List<KnowledgeEntry> results = knowledgeBaseLoader.searchByVector(queryVector, 15);

    // 3. MMR 重排（平衡相关性和多样性）
    List<KnowledgeEntry> reranked = resultReranker.mmrRerank(results, question, 8);

    // 4. 构建上下文
    StringBuilder context = new StringBuilder();
    for (KnowledgeEntry entry : reranked) {
        context.append("【").append(entry.getCategory()).append("】\n");
        context.append("- ").append(entry.getContent()).append("\n");
    }
    return context.toString();
}

// Fallback 关键词匹配（向量检索失败时使用）
private String fallbackSearch(String question) {
    Map<String, String> keywordToCategory = new HashMap<>();
    keywordToCategory.put("安全", "骑行安全");
    keywordToCategory.put("头盔", "骑行安全");
    // ... 关键词映射
    // 从 KNOWLEDGE_BASE 中匹配
}
```

#### 为什么用 Redis 存储向量？

| 方案 | 优点 | 缺点 |
|------|------|------|
| 内存Map | 零配置，查询快 | 重启丢失，不支持语义匹配 |
| Redis向量 | 支持语义搜索，重启保留 | 需要额外部署Redis |
| Chroma | 功能强大的向量数据库 | 需要额外部署 |

**当前方案**：Redis + 向量检索 + MMR重排

- **向量存储**：知识库的每条文本生成向量，存储在Redis
- **语义检索**：使用 `nomic-embed-text` 模型生成向量，余弦相似度计算
- **MMR重排**：Maximal Marginal Relevance，平衡相关性和多样性
- **Fallback**：向量检索失败时回退到关键词匹配

#### RAG 架构图

```
用户问题: "骑行时如何保护膝盖？"
     │
     ▼
┌─────────────────────────────┐
│  EmbeddingService.embed()   │  ← 使用 nomic-embed-text 模型
│  生成问题向量 [0.12, -0.34...]│
└─────────────────────────────┘
     │
     ▼
┌─────────────────────────────┐
│  KnowledgeBaseLoader       │
│  Redis 向量检索             │
│  余弦相似度计算             │
└─────────────────────────────┘
     │
     ▼
┌─────────────────────────────┐
│  ResultReranker.mmrRerank() │
│  MMR 重排（多样性）          │
└─────────────────────────────┘
     │
     ▼
┌─────────────────────────────┐
│  构建上下文 → LLM 生成回答   │
└─────────────────────────────┘
     │
     ▼
回答: "保护膝盖需要注意以下几点：..."
```

#### 关键组件

| 组件 | 文件 | 功能 |
|------|------|------|
| EmbeddingService | `service/EmbeddingService.java` | 向量生成 |
| KnowledgeBaseLoader | `service/KnowledgeBaseLoader.java` | 知识库加载、向量存储 |
| TextChunker | `service/TextChunker.java` | 文本分块 |
| ResultReranker | `service/ResultReranker.java` | MMR重排 |

---

## 11. 社区功能模块

### 10.1 功能清单

| 功能 | API | 说明 |
|------|-----|------|
| 发布路线 | POST /api/community/route | 需登录 |
| 路线列表 | GET /api/community/routes | 公开 |
| 我的路线 | GET /api/community/my-routes | 需登录 |
| 路线详情 | GET /api/community/route/{id} | 公开（+浏览量） |
| 更新路线 | PUT /api/community/route/{id} | 需登录，仅作者 |
| 删除路线 | DELETE /api/community/route/{id} | 需登录，仅作者 |
| 点赞 | POST /api/community/route/{id}/like | 需登录 |
| 评论列表 | GET /api/community/route/{id}/comments | 公开 |
| 添加评论 | POST /api/community/route/{id}/comment | 需登录 |
| 删除评论 | DELETE /api/community/comment/{id} | 需登录，仅作者 |

### 10.2 发布路线流程

```java
@PostMapping("/route")
public Result<RouteVO> publishRoute(
    @AuthenticationPrincipal CustomUserDetails userDetails,
    @RequestBody RoutePublishRequest request) {

    // 1. 检查登录
    if (userDetails == null) {
        return Result.error("请先登录");
    }

    // 2. 检查参数
    if (request.getTitle() == null || request.getTitle().isBlank()) {
        return Result.error("标题不能为空");
    }

    // 3. 保存到数据库
    RouteVO route = communityService.publishRoute(userDetails.getId(), request);

    return Result.success(route);
}
```

### 10.3 点赞实现（防重复）

```java
@Transactional
public boolean toggleLike(Long routeId, Long userId) {
    // 1. 检查路线是否存在
    Route route = routeMapper.selectById(routeId);
    if (route == null) {
        throw new RuntimeException("路线不存在");
    }

    // 2. 检查是否已点赞
    LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
    wrapper.eq(Like::getRouteId, routeId)
           .eq(Like::getUserId, userId);
    Like existingLike = likeMapper.selectOne(wrapper);

    if (existingLike != null) {
        // 取消点赞
        likeMapper.deleteById(existingLike.getId());
        route.setLikes(route.getLikes() - 1);
        routeMapper.updateById(route);
        return false;
    } else {
        // 添加点赞
        Like like = new Like();
        like.setRouteId(routeId);
        like.setUserId(userId);
        likeMapper.insert(like);
        route.setLikes(route.getLikes() + 1);
        routeMapper.updateById(route);
        return true;
    }
}
```

**为什么点赞数要冗余存储在route表？**

```
不冗余：查点赞数需要 count(like表)
冗余：直接读 route.likes

性能对比：
- 列表页：100条路线
  - 不冗余：100次count查询 ❌
  - 冗余：0次查询 ✅

空间换时间
```

### 10.4 评论与回复

```java
// 添加评论
@PostMapping("/route/{id}/comment")
public Result<CommentVO> addComment(
    @PathVariable("id") Long routeId,
    @RequestBody Map<String, Object> request) {

    String content = (String) request.get("content");
    Long parentId = request.get("parentId") != null
        ? Long.parseLong(request.get("parentId").toString())
        : null;  // null = 主评论

    CommentVO comment = communityService.addComment(routeId, userDetails.getId(), content, parentId);
    return Result.success(comment);
}
```

**楼中楼数据结构**：

```
评论表数据示例：
id=1, route_id=100, user_id=1, parent_id=0, content="写得很详细！"
id=2, route_id=100, user_id=2, parent_id=1, content="谢谢夸奖~"
id=3, route_id=100, user_id=3, parent_id=0, content="新手适合吗？"
```

---

## 12. 配置与依赖管理

### 11.1 application.properties 详解

```properties
# 应用
spring.application.name=rideSketch
server.port=8080

# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/ridesketch
    ?useUnicode=true&characterEncoding=utf8    # 中文编码
    &useSSL=false                                # 本地不需要SSL
    &serverTimezone=Asia/Shanghai               # 时区
    &allowPublicKeyRetrieval=true                # 允许拉取公钥

spring.datasource.username=root
spring.datasource.password=Cqian1231

# MyBatis
mybatis.configuration.map-underscore-to-camel-case=true
# 自动将 snake_case 转为 camelCase

# JWT
jwt.secret=rideSketch2026SecretKeyForJWTTokenGeneration
jwt.expiration=86400000  # 24小时 = 24 * 60 * 60 * 1000

# 高德地图
amap.key=30df485f0872725106bacd290344efd5

# Spring AI
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.options.model=qwen3:8b
spring.ai.ollama.chat.options.temperature=0.7
```

**为什么要配置 allowPublicKeyRetrieval=true？**

```
MySQL 8.0+ 默认使用 caching_sha2_password 认证插件
客户端连接时需要获取服务端公钥

本地开发环境：允许
生产环境：建议配置公钥路径或使用 mysql_native_password
```

### 11.2 依赖版本管理

```xml
<!-- Spring Boot Parent 统一版本管理 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.10</version>
</parent>

<!-- 只需指定 MyBatis-Plus 版本 -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
    <version>3.5.5</version>
</dependency>
```

**Spring Boot Parent 的作用**：

- 统一管理所有依赖版本
- 自动配置
- 提供插件管理

---

## 13. 面试常见问题汇总

### Q1: 项目用的是什么认证方案？为什么要用这个？

**答**：使用 JWT 无状态认证。

**选择理由**：
1. 前后端分离架构，Session 不适用
2. 移动端/H5需要跨域支持
3. 简单高效，无需服务端存储

### Q2: JWT 的优缺点？

**答**：
- **优点**：无状态、跨域、支持移动端、验证快
- **缺点**：无法主动失效（需配合黑名单）、Payload不宜放敏感信息

### Q3: 项目如何处理用户密码？

**答**：使用 BCrypt 加密存储。

```java
// 注册时
passwordEncoder.encode(rawPassword);

// 登录时
passwordEncoder.matches(rawPassword, encodedPassword);
```

**为什么不用MD5/SHA？**
- 这些算法太快，便于暴力破解
- BCrypt 有自动盐，工作因子可调

### Q4: 如何保证接口安全？

**答**：
1. **认证**：JWT Token 验证
2. **授权**：基于路径的权限配置
3. **参数校验**：@Valid 注解
4. **敏感信息**：密码加密，配置外部化

### Q5: 什么是 MyBatis-Plus？它有什么优势？

**答**：MyBatis-Plus 是 MyBatis 的增强工具。

**优势**：
1. CRUD 零 SQL
2. 分页插件简单
3. 自动填充
4. 条件构造器
5. 无侵入

### Q6: 项目的 AI 功能是怎么实现的？

**答**：使用 Spring AI 集成 Ollama 本地大模型。

```
用户输入 -> Spring AI -> Ollama (Qwen3:8b) -> 解析结果 -> 调用高德API -> 返回路线
```

### Q7: 多途经点路线如何实现？

**答**：分段规划 + 结果合并。

```
A -> B -> C -> D

实现：
1. A -> B (一段)
2. B -> C (一段)
3. C -> D (一段)

合并：距离相加、时间相加、路径坐标拼接
```

### Q8: 如果要你优化性能，你会怎么做？

**答**：
1. Redis 缓存热点数据
2. 数据库添加索引
3. 接口聚合减少请求
4. 异步处理 AI 请求
5. 分页加载

### Q9: 项目遇到的最大挑战是什么？

**答**：AI 响应不稳定。

**解决**：
1. JSON 提取容错
2. 回退机制（AI失败用关键词匹配）
3. 详细日志

### Q10: 项目的技术选型依据？

**答**：
- Spring Boot：生态最强
- MyBatis-Plus：SQL可控，中文文档好
- JWT：无状态，适合前后端分离
- 高德地图：中文POI最全
- Ollama：本地部署，成本低

---

## 14. 项目完整运行流程详解

> **特别说明**：本章面向第一次接触本项目的人，从零开始，详细讲解程序的启动、运行、请求处理全流程。每个步骤、每个组件的作用都会详细解释，确保你能讲明白整个程序的工作原理。

### 13.1 启动前的准备工作

在运行项目之前，需要确保以下服务已经启动：

#### 13.1.1 启动 MySQL 数据库

```bash
# 方式1：Docker 启动
docker run -d --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=Cqian1231 mysql:8

# 方式2：本地安装的 MySQL 服务
# Windows: 在服务中启动 MySQL 服务
# Linux/Mac: sudo systemctl start mysql
```

**MySQL 启动后会做什么**：
1. 监听 3306 端口，等待客户端连接
2. 加载 `ridesketch` 数据库
3. 执行 `init.sql` 脚本，创建 4 张表（user, route, like, comment）

#### 13.1.2 启动 Redis

```bash
# 方式1：Docker 启动
docker run -d --name redis -p 6379:6379 redis:alpine

# 方式2：本地安装的 Redis
# Windows: redis-server.exe
# Linux/Mac: redis-server
```

**Redis 启动后会做什么**：
1. 监听 6379 端口
2. 提供内存缓存功能（本项目主要用于会话缓存）
3. 默认无密码（开发环境）

#### 13.1.3 启动 Ollama（本地大模型服务）

```bash
# 启动 Ollama 服务
ollama serve

# 在另一个终端下载需要的模型
ollama pull qwen3:8b        # 对话模型
ollama pull nomic-embed-text # 向量嵌入模型
```

**Ollama 启动后会做什么**：
1. 监听 11434 端口，提供 REST API
2. 加载模型到内存（消耗约 4-8GB 内存）
3. 等待 HTTP 请求进行推理

#### 13.1.4 验证服务状态

```bash
# 测试 MySQL
mysql -h localhost -u root -p -e "SHOW DATABASES;"

# 测试 Redis
redis-cli ping
# 返回 PONG 表示正常

# 测试 Ollama
curl http://localhost:11434/api/tags
# 返回模型列表表示正常
```

---

### 13.2 后端启动流程详解

#### 13.2.1 执行启动命令

```bash
cd D:\IdeaProjects\rideSketch
.\mvnw.cmd spring-boot:run
```

#### 13.2.2 Spring Boot 启动过程（十大步骤）

**步骤1：执行 main() 方法**

```java
// RideSketchApplication.java
public static void main(String[] args) {
    SpringApplication.run(RideSketchApplication.class, args);
}
```

这一步做了什么：
- 创建一个 Spring Application 对象
- 加载所有 classpath 下的配置
- 启动 Spring Boot 自动配置

**步骤2：Spring Boot 自动配置**

Spring Boot 会扫描所有 `META-INF/spring.factories` 和 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 文件，加载自动配置类。

本项目涉及的自动配置：
- `DataSourceAutoConfiguration` - 数据库自动配置
- `RedisAutoConfiguration` - Redis 自动配置
- `SecurityAutoConfiguration` - 安全自动配置
- `WebMvcAutoConfiguration` - Web MVC 自动配置

**步骤3：启动内嵌 Tomcat 服务器**

```
INFO - Tomcat initialized with port 8080 (http)
```

- 创建内嵌 Tomcat 服务器
- 监听 8080 端口
- 配置线程池（默认 200 线程）

**步骤4：Spring 容器初始化**

```java
// 扫描并注册 Bean
@ComponentScan(basePackages = "org.example.ridesketch")
```

扫描以下包及其子包：
- `org.example.ridesketch.controller`
- `org.example.ridesketch.service`
- `org.example.ridesketch.mapper`
- `org.example.ridesketch.config`
- `org.example.ridesketch.security`
- `org.example.ridesketch.entity`
- `org.example.ridesketch.dto`
- `org.example.ridesketch.common`

**步骤5：注册 Bean（按顺序）**

```
1. 配置类 (Config)
   ├── SecurityConfig
   ├── MyBatisPlusConfig
   ├── AIConfig
   └── RestTemplateConfig

2. 实体类 (Entity)
   ├── User
   ├── Route
   ├── Like
   └── Comment

3. Mapper 接口
   ├── UserMapper
   ├── RouteMapper
   ├── LikeMapper
   └── CommentMapper

4. Service 层
   ├── UserServiceImpl
   ├── MapServiceImpl
   ├── RouteServiceImpl
   ├── AIRouteServiceImpl
   ├── PatternRouteServiceImpl
   └── CommunityServiceImpl

5. Controller 层
   ├── UserController
   ├── MapController
   ├── RouteController
   ├── CommunityController
   ├── RagController
   └── ChromaController
```

**步骤6：执行 Bean 初始化方法**

以 `SecurityConfig` 为例：

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // 1. 注入 JwtAuthenticationFilter
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 2. 注入 UserDetailsService
    private final UserDetailsService userDetailsService;

    // 3. 创建 SecurityFilterChain Bean
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        // 配置安全规则
    }

    // 4. 创建 AuthenticationManager Bean
    @Bean
    public AuthenticationManager authenticationManager(...) {
        // 创建认证管理器
    }

    // 5. 创建 PasswordEncoder Bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

**步骤7：数据库连接池初始化**

```properties
# application.properties 配置
spring.datasource.url=jdbc:mysql://localhost:3306/ridesketch
spring.datasource.username=root
spring.datasource.password=Cqian1231
```

Spring Boot 会：
1. 创建 HikariCP 连接池
2. 测试数据库连接
3. 建立初始连接（默认 10 个）

**步骤8：MyBatis-Plus 初始化**

```java
// MyBatisPlusConfig.java
@MapperScan("org.example.ridesketch.mapper")
public class MyBatisPlusConfig {
    // 配置分页插件
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
```

**步骤9：Spring Security 过滤器链注册**

```
请求流程经过的过滤器（从上到下）：

1. WebAsyncManagerIntegrationFilter
2. SecurityContextPersistenceFilter
3. HeaderWriterFilter
4. CorsFilter
5. LogoutFilter
6. JwtAuthenticationFilter      <-- 重点：处理 JWT
7. RequestCacheAwareFilter
8. AnonymousAuthenticationFilter
9. SessionManagementFilter
10. ExceptionTranslationFilter
11. FilterSecurityInterceptor
```

**步骤10：应用启动完成**

```
INFO - Started RideSketchApplication in 3.456 seconds
INFO - Application is running on port 8080
```

---

### 13.3 前端启动流程详解

#### 13.3.1 安装依赖

```bash
cd frontend
npm install
```

这一步做了什么：
1. 读取 `package.json` 中的 `dependencies` 和 `devDependencies`
2. 从 npm 仓库下载所有依赖包到 `node_modules` 目录
3. 执行 postinstall 脚本（如有）

#### 13.3.2 启动开发服务器

```bash
npm run dev
```

Vite 启动过程：

```
1. 读取 vite.config.ts 配置
2. 启动开发服务器（默认 5173 端口）
3. 监听文件变化
4. 启动热更新（HMR）服务
```

#### 13.3.3 浏览器访问

用户打开浏览器访问 `http://localhost:5173`

**Vite 处理流程**：

```
1. 浏览器请求 index.html
2. Vite 返回 index.html
3. 浏览器解析 index.html，发现 <script type="module" src="/src/main.ts">
4. 请求 /src/main.ts
5. Vite 编译 main.ts（使用 esbuild，极快）
6. 返回编译后的 JS
7. 浏览器执行 JS，渲染 Vue 应用
```

#### 13.3.4 Vue 应用初始化

```typescript
// main.ts
import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(ElementPlus)    // 安装 Element Plus UI 组件库
app.use(router)         // 安装 Vue Router

app.mount('#app')       // 挂载到 #app 元素
```

**Vue Router 初始化**：

```typescript
// router/index.ts
const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        { path: '/', redirect: '/map' },
        { path: '/map', component: MapPage },
        { path: '/login', component: LoginPage },
        // ...
    ]
})

// 全局前置守卫
router.beforeEach((to, from, next) => {
    // 检查是否需要登录
    const token = getToken()
    if (to.meta.requiresAuth && !token) {
        next('/login')
    } else {
        next()
    }
})
```

---

### 13.4 完整请求流程详解

> **核心内容**：从用户在浏览器输入到数据返回的完整流程

#### 13.4.1 登录请求流程（最详细的示例）

**场景**：用户在登录页面输入用户名和密码，点击登录按钮

**第一步：前端发起请求**

```javascript
// frontend/src/views/auth/LoginPage.vue
async function handleLogin() {
    const response = await login({
        usernameOrEmail: formData.usernameOrEmail,
        password: formData.password
    })
}
```

```typescript
// frontend/src/api/user.ts
export async function login(data: LoginRequest): Promise<AuthResponse> {
    const response = await request.post('/auth/login', data)
    // response = { success: true, message: "登录成功", data: { token: "xxx", ... } }
    if (response.data.success && response.data.data.token) {
        setToken(response.data.data.token)  // 存到 localStorage
    }
    return response.data
}
```

**请求拦截器做了什么**：

```typescript
// frontend/src/api/user.ts
const request = axios.create({
    baseURL: '/api',  // 代理到 http://localhost:8080
    timeout: 10000
})

// 请求拦截器：添加 Token
request.interceptors.request.use((config) => {
    const token = getToken()
    if (token) {
        config.headers.Authorization = `Bearer ${token}`
    }
    return config
})
```

**第二步：前端代理转发**

Vite 配置了代理：

```typescript
// vite.config.ts
export default defineConfig({
    server: {
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true
            }
        }
    }
})
```

所以 `POST /api/auth/login` 实际发送到 `http://localhost:8080/api/auth/login`

**第三步：Tomcat 接收请求**

```
浏览器 --(POST /api/auth/login)--> Vite 代理 --(POST /api/auth/login)--> Tomcat:8080
```

Tomcat 将请求交给 Spring 处理

**第四步：Spring MVC 处理**

```java
// UserController.java
@PostMapping("/login")
public ResponseEntity<Map<String, Object>> login(
        @Valid @RequestBody LoginRequest loginRequest) {

    // 1. 参数已通过 @Valid 验证
    // 2. 调用 Service 处理业务
    AuthResponse authResponse = userService.login(loginRequest);

    // 3. 返回响应
    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("message", "登录成功");
    response.put("data", authResponse);
    return ResponseEntity.ok(response);
}
```

**第五步：Service 处理业务**

```java
// UserServiceImpl.java
@Override
public AuthResponse login(LoginRequest loginRequest) {
    // 第1步：使用 Spring Security 认证
    authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(
            loginRequest.getUsernameOrEmail(),  // 可用户名或邮箱
            loginRequest.getPassword()
        )
    );

    // 第2步：查询用户（支持用户名或邮箱）
    User user = findByUsername(loginRequest.getUsernameOrEmail());
    if (user == null) {
        user = findByEmail(loginRequest.getUsernameOrEmail());
    }

    // 第3步：检查用户状态
    if (user.getStatus() == 0) {
        throw new RuntimeException("账号已被禁用");
    }

    // 第4步：生成 JWT Token
    String token = jwtUtils.generateToken(user.getId(), user.getUsername());

    // 第5步：返回认证响应
    return AuthResponse.builder()
        .token(token)
        .userId(user.getId())
        .username(user.getUsername())
        .nickname(user.getNickname())
        .avatar(user.getAvatar())
        .build();
}
```

**第六步：JWT Token 生成**

```java
// JwtUtils.java
public String generateToken(Long userId, String username) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);      // 存入用户ID
    claims.put("username", username);  // 存入用户名

    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + 86400000); // 24小时后

    return Jwts.builder()
            .claims(claims)
            .subject(username)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(getSigningKey())  // HMAC SHA256 签名
            .compact();
}
```

生成的 JWT 类似于：
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoidGVzdCJ9.xxxxx
```

**第七步：数据库查询**

```java
// UserMapper.java
// 继承自 BaseMapper<User>
public interface UserMapper extends BaseMapper<User> {
}

// 查询用户
User user = userMapper.selectOne(
    new QueryWrapper<User>()
        .eq("username", username)
);
```

执行的 SQL：
```sql
SELECT id, username, email, password, nickname, avatar, status, create_time, update_time
FROM user
WHERE username = 'test' LIMIT 1
```

**第八步：密码验证**

```java
// Spring Security 内部验证
// BCrypt 加密验证
boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
```

**第九步：响应返回**

```json
{
    "success": true,
    "message": "登录成功",
    "data": {
        "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
        "userId": 1,
        "username": "test",
        "nickname": "测试用户",
        "avatar": ""
    }
}
```

**第十步：前端处理响应**

```javascript
// LoginPage.vue
if (response.success) {
    ElMessage.success('登录成功')
    router.push('/map')  // 跳转到地图页
}
```

```typescript
// 前端保存 Token
setToken(response.data.data.token)  // localStorage.setItem('ridesketch_token', token)
setUserId(response.data.data.userId)
```

---

#### 13.4.2 访问需要认证的接口

**场景**：用户登录后，点击"发布路线"按钮

**请求流程**：

```
1. 前端：请求 /api/community/route
   Header: Authorization: Bearer eyJhbGci...

2. Vite 代理转发到 http://localhost:8080/api/community/route

3. Spring Security JwtAuthenticationFilter 拦截
```

**JwtAuthenticationFilter 详细处理**：

```java
// JwtAuthenticationFilter.java
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) {

    // 第1步：从 Header 提取 Token
    String bearerToken = request.getHeader("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
        String jwt = bearerToken.substring(7);  // 去掉 "Bearer " 前缀

        // 第2步：验证 Token
        if (jwtUtils.validateToken(jwt)) {
            // 第3步：获取用户名
            String username = jwtUtils.getUsernameFromToken(jwt);

            // 第4步：加载用户信息
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // 第5步：设置认证信息
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
                );

            // 第6步：存入 Security Context
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }

    // 第7步：放行请求
    filterChain.doFilter(request, response);
}
```

**Controller 接收请求**：

```java
@PostMapping("/route")
public Result<RouteVO> publishRoute(
    @AuthenticationPrincipal CustomUserDetails userDetails,  // 从 Security Context 获取
    @RequestBody RoutePublishRequest request) {

    // userDetails 不为 null，表示已认证
    Long userId = userDetails.getId();

    // 执行业务逻辑
    RouteVO route = communityService.publishRoute(userId, request);

    return Result.success(route);
}
```

---

#### 13.4.3 地图搜索请求流程

**场景**：用户在地图页面搜索"天安门"

**第一步：前端直接调用高德地图 JS API**

```javascript
// 前端不经过后端，直接使用高德地图 JS API
AMap.plugin(['AMap.PlaceSearch'], function() {
    const placeSearch = new AMap.PlaceSearch({
        city: '北京',
        citylimit: true
    })

    placeSearch.search('天安门', function(status, result) {
        // 返回搜索结果
    })
})
```

**第二步：后端地理编码 API**

如果需要将地址转为坐标：

```java
// MapController.java
@GetMapping("/geocode")
public Result<GeoCodeResult> geocode(@RequestParam String address) {
    GeoCodeResult result = mapService.geocode(address);
    return Result.success(result);
}
```

**MapService 实现**：

```java
// MapServiceImpl.java
@Override
public GeoCodeResult geocode(String address) {
    // 第1步：构建高德 API URL
    String url = "https://restapi.amap.com/v3/geocode/geo"
        + "?key=" + amapKey
        + "&address=" + URLEncoder.encode(address, StandardCharsets.UTF_8);

    // 第2步：调用高德 API
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    // 第3步：解析响应
    return parseGeoCodeResult(response.getBody());
}
```

**高德 API 返回**：

```json
{
    "status": "1",
    "geocodes": [{
        "province": "北京市",
        "city": "北京市",
        "district": "东城区",
        "location": "116.397128,39.916527"
    }]
}
```

---

#### 13.4.4 路线规划请求流程

**场景**：用户设置起点和终点，点击"规划路线"

**第一步：前端调用后端 API**

```typescript
// frontend/src/api/route.ts
export async function planRidingRoute(origin, destination, waypoints?) {
    return request.get('/route/riding', {
        params: { origin, destination, waypoints }
    })
}
```

**请求**：
```
GET /api/route/riding?origin=116.397,39.916&destination=116.418,39.928
```

**第二步：RouteService 处理**

```java
// RouteServiceImpl.java
@Override
public RoutePlanningResult planRidingRoute(String origin, String destination, String waypoints) {
    // 有途经点，走多段规划
    if (waypoints != null && !waypoints.isEmpty()) {
        return planMultiSegmentRoute(origin, destination, waypoints, "bicycling");
    }

    // 无途经点，单段规划
    return planSingleSegmentRoute(origin, destination, "bicycling");
}
```

**第三步：调用高德路径规划 API**

```java
private RoutePlanningResult planSingleSegmentRoute(
    String origin, String destination, String mode) {

    String url = "https://restapi.amap.com/v4/direction/" + mode
        + "?key=" + amapKey
        + "&origin=" + origin      // 格式：经度,纬度
        + "&destination=" + destination;

    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
    return parseRoutePlanningResult(response.getBody());
}
```

**高德 API 返回**：

```json
{
    "status": "1",
    "route": {
        "origin": "116.397,39.916",
        "destination": "116.418,39.928",
        "paths": [{
            "distance": "3500",
            "duration": "1080",
            "strategy": "10",
            "steps": [
                {
                    "instruction": "沿正义路向东南方向骑行240米",
                    "road": "正义路",
                    "distance": "240",
                    "duration": "36"
                },
                // ... 更多步骤
            ],
            "path": "116.397,39.916;116.398,39.917;..."
        }]
    }
}
```

**第四步：返回结果给前端**

```java
// 返回给前端的数据结构
{
    "code": 200,
    "data": {
        "status": "1",
        "route": {
            "origin": "116.397,39.916",
            "destination": "116.418,39.928",
            "paths": [{
                "distance": "3500",      // 距离（米）
                "duration": "1080",      // 时间（秒）
                "steps": [...],
                "path": "116.397,39.916;116.398,39.917;..."
            }]
        }
    }
}
```

**第五步：前端渲染路线**

```javascript
// MapPage.vue
const path = result.route.paths[0].path.split(';')
    .map(p => p.split(',').map(Number))

// 绘制折线
const polyline = new AMap.Polyline({
    path: path,
    strokeColor: '#3B7EFF',
    strokeWeight: 5
})
map.add(polyline)
```

---

#### 13.4.5 AI 智能规划流程

**场景**：用户在输入框输入"从天安门骑到颐和园，看看风景"

**第一步：前端发送请求**

```typescript
// frontend/src/api/route.ts
export async function planAIRoute(description: string) {
    return request.post('/route/ai-plan', { description })
}
```

**第二步：AIRouteService 处理**

```java
// AIRouteServiceImpl.java
@Override
public AIRoutePlanningResult planAIRoute(AIRoutePlanningRequest request) {
    String userInput = request.getDescription();

    // 第1步：调用 AI 解析用户输入
    Map<String, Object> parsed = aiService.parseUserInput(userInput);

    // 第2步：地理编码
    String originCoords = mapService.geocode(parsed.get("origin").toString());
    String destCoords = mapService.geocode(parsed.get("destination").toString());

    // 第3步：调用高德路径规划
    RoutePlanningResult routeResult = routeService.planRidingRoute(
        originCoords, destCoords, null
    );

    // 第4步：返回结果
    return convertToAIResult(routeResult, parsed);
}
```

**第三步：Spring AI 调用 Ollama**

```java
// 使用 Spring AI 调用 Ollama
@Bean
public ChatClient chatClient(OllamaChatModel chatModel) {
    return ChatClient.builder(chatModel).build();
}

// 调用
String response = chatClient.prompt()
    .system("你是一个骑行路线规划助手...")
    .user("请分析：从天安门骑到颐和园，看看风景")
    .call()
    .content();
```

**Ollama 本地处理**：
1. 加载 `qwen3:8b` 模型（需要约 4-8GB 内存）
2. 根据 prompt 生成回答
3. 返回 JSON 格式的解析结果

---

#### 13.4.6 社区功能流程

**场景**：用户发布路线到社区

**第一步：前端提交表单**

```typescript
// CommunityPage.vue
async function publishRoute() {
    const data = {
        title: form.title,
        description: form.description,
        startPoint: form.startPoint,
        endPoint: form.endPoint,
        waypoints: JSON.stringify(form.waypoints),
        routePath: form.routePath,
        totalDistance: form.totalDistance,
        estimatedTime: form.estimatedTime,
        difficulty: form.difficulty,
        tags: JSON.stringify(form.tags),
        isPublic: 1
    }

    await publishRoute(data)
}
```

**第二步：CommunityService 处理**

```java
// CommunityServiceImpl.java
@Override
@Transactional
public RouteVO publishRoute(Long userId, RoutePublishRequest request) {
    // 第1步：创建 Route 实体
    Route route = new Route();
    route.setUserId(userId);
    route.setTitle(request.getTitle());
    route.setDescription(request.getDescription());
    route.setStartPoint(request.getStartPoint());
    route.setEndPoint(request.getEndPoint());
    route.setWaypoints(request.getWaypoints());
    route.setRoutePath(request.getRoutePath());
    route.setTotalDistance(request.getTotalDistance());
    route.setEstimatedTime(request.getEstimatedTime());
    route.setDifficulty(request.getDifficulty());
    route.setTags(request.getTags());
    route.setIsPublic(request.getIsPublic());
    route.setLikes(0);
    route.setViews(0);

    // 第2步：插入数据库
    routeMapper.insert(route);

    // 第3步：构建返回对象
    return convertToVO(route);
}
```

**执行的 SQL**：

```sql
INSERT INTO route (
    user_id, title, description, start_point, end_point,
    waypoints, route_path, total_distance, estimated_time,
    difficulty, tags, likes, views, is_public,
    create_time, update_time
) VALUES (
    1, '周末骑行', '很棒的路线', '天安门', '颐和园',
    '[]', '116.397,39.916;...', 25.5, 90,
    1, '["休闲"]', 0, 0, 1,
    NOW(), NOW()
)
```

---

### 13.5 数据流转总览

#### 13.5.1 整体架构数据流

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              用户浏览器                                   │
│  http://localhost:5173                                                   │
└─────────────────────────────┬───────────────────────────────────────────┘
                              │
                              │ 1. HTTP 请求 (JSON)
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        Vite 开发服务器                                   │
│  localhost:5173 (代理 /api -> localhost:8080)                           │
│  - 请求转发                                                               │
│  - 热更新                                                                │
│  - 静态资源服务                                                           │
└─────────────────────────────┬───────────────────────────────────────────┘
                              │
                              │ 2. 代理转发
                              ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     Spring Boot 后端 (Tomcat)                           │
│  localhost:8080                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Controller 层 (接收请求)                                         │  │
│  │  - UserController                                                │  │
│  │  - MapController                                                 │  │
│  │  - RouteController                                              │  │
│  │  - CommunityController                                           │  │
│  │  - RagController                                                 │  │
│  └────────────────────────────┬─────────────────────────────────────┘  │
│                               │                                         │
│                               ▼                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Service 层 (业务逻辑)                                           │  │
│  │  - UserServiceImpl                                               │  │
│  │  - MapServiceImpl                                                │  │
│  │  - RouteServiceImpl                                              │  │
│  │  - AIRouteServiceImpl                                            │  │
│  │  - CommunityServiceImpl                                          │  │
│  │  - RagServiceImpl                                                │  │
│  └────────────────────────────┬─────────────────────────────────────┘  │
│                               │                                         │
│                               ▼                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Mapper 层 (数据访问)                                            │  │
│  │  - UserMapper                                                    │  │
│  │  - RouteMapper                                                   │  │
│  │  - LikeMapper                                                    │  │
│  │  - CommentMapper                                                 │  │
│  └────────────────────────────┬─────────────────────────────────────┘  │
└─────────────────────────────┬───────────────────────────────────────────┘
                              │
              ┌───────────────┴───────────────┐
              │                               │
              ▼                               ▼
┌─────────────────────────┐     ┌─────────────────────────┐
│     MySQL 数据库        │     │       Redis             │
│  localhost:3306         │     │  localhost:6379         │
│  - user 表              │     │  - 会话缓存             │
│  - route 表             │     │  - Token 缓存           │
│  - like 表              │     │                         │
│  - comment 表           │     │                         │
└─────────────────────────┘     └─────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                        外部服务                                          │
│                                                                          │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐    │
│  │   高德地图       │    │     Ollama       │    │    Chroma       │    │
│  │  restapi.amap.com│    │ localhost:11434  │    │ (向量数据库)    │    │
│  │  - 地理编码      │    │  - qwen3:8b      │    │ (可选)          │    │
│  │  - 路径规划      │    │  - nomic-embed   │    │                 │    │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
```

#### 13.5.2 核心请求-响应流程

**登录流程**：
```
用户输入账号密码
    ↓
前端 axios POST /api/auth/login
    ↓
Vite 代理 → http://localhost:8080/api/auth/login
    ↓
UserController.login()
    ↓
UserServiceImpl.login()
    ↓
Spring Security 验证密码 (BCrypt)
    ↓
UserMapper 查询数据库
    ↓
JwtUtils 生成 JWT Token
    ↓
返回 { token, userId, username, ... }
    ↓
前端 localStorage 保存 Token
    ↓
跳转首页
```

**发布路线流程**：
```
用户填写路线信息 → 点击发布
    ↓
前端 POST /api/community/route
    Header: Authorization: Bearer <token>
    ↓
JwtAuthenticationFilter 验证 Token
    ↓
SecurityContext 设置用户信息
    ↓
CommunityController.publishRoute()
    ↓
CommunityService.publishRoute()
    ↓
RouteMapper 插入数据库
    ↓
返回 RouteVO
    ↓
前端显示发布成功
```

**地图规划流程**：
```
用户设置起终点 → 点击规划
    ↓
前端 GET /api/route/riding?origin=...&destination=...
    ↓
RouteController.planRidingRoute()
    ↓
RouteService.planRidingRoute()
    ↓
调用高德 API (restapi.amap.com/v4/direction/bicycling)
    ↓
解析高德返回的路径数据
    ↓
返回 { distance, duration, path, steps }
    ↓
前端 AMap.Polyline 绘制路线
```

---

### 13.6 关键代码位置索引

为了方便理解，这里列出核心代码的文件位置：

#### 13.6.1 后端核心文件

| 功能 | 文件路径 | 关键代码 |
|------|---------|---------|
| 启动类 | `RideSketchApplication.java` | `SpringApplication.run()` |
| 配置 | `application.properties` | 数据库、Redis、JWT 配置 |
| 安全配置 | `SecurityConfig.java` | 过滤器链、CORS、权限 |
| JWT工具 | `JwtUtils.java` | 生成、验证 Token |
| JWT过滤器 | `JwtAuthenticationFilter.java` | Token 解析、设置 SecurityContext |
| 用户控制器 | `UserController.java` | 注册、登录、获取用户信息 |
| 用户服务 | `UserServiceImpl.java` | 业务逻辑 |
| 地图控制器 | `MapController.java` | 地理编码、逆编码、搜索 |
| 路线控制器 | `RouteController.java` | 骑行/步行规划、AI规划、图案规划 |
| 社区控制器 | `CommunityController.java` | 发布、列表、点赞、评论 |

#### 13.6.2 前端核心文件

| 功能 | 文件路径 | 关键代码 |
|------|---------|---------|
| 入口 | `main.ts` | Vue 应用创建 |
| 路由 | `router/index.ts` | 路由配置、导航守卫 |
| API封装 | `api/user.ts` | axios 拦截器、登录注册 |
| 登录页 | `views/auth/LoginPage.vue` | 登录表单、调用 API |
| 地图页 | `views/map/MapPage.vue` | 高德地图初始化、路线规划 |
| 社区页 | `views/community/CommunityPage.vue` | 路线列表、发布 |

#### 13.6.3 数据库表

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| user | 用户表 | username, email, password |
| route | 路线表 | user_id, title, route_path |
| like | 点赞表 | user_id, route_id |
| comment | 评论表 | route_id, user_id, parent_id |

---

### 13.7 常见问题解答

#### Q1: 为什么前端要通过 Vite 代理而不是直接访问后端？

**原因**：
1. **解决跨域**：浏览器有同源策略，直接访问 8080 端口会有 CORS 问题
2. **简化开发**：前端只需用相对路径 `/api/xxx`
3. **统一入口**：方便后续添加统一的请求处理（如统一错误处理）

#### Q2: 为什么 JWT Token 存在 localStorage 而不是 Cookie？

**本项目方案**：
- Token 存 localStorage
- 请求 Header: `Authorization: Bearer <token>`

**对比**：

| 方案 | 优点 | 缺点 |
|------|------|------|
| localStorage + Header | 简单、跨域支持好 | XSS 攻击风险 |
| HttpOnly Cookie | 防止 XSS | CSRF 攻击风险 |

**说明**：本项目是前后端分离的 SPA，CSRF 防护由 CORS + Token 机制提供。对于安全要求更高的场景，建议用 HttpOnly Cookie。

#### Q3: 为什么地图搜索用前端 JS API 而不是后端 API？

**原因**：
1. **用户体验**：前端直接调用 JS API 可以即时显示搜索下拉
2. **减少后端压力**：搜索是高频操作，前端直接调用减轻后端负担
3. **地图交互**：前端需要处理地图事件（点击、拖拽）

**但**：地址转坐标（Geocode）必须走后端，因为：
- 需要高德 Web API Key（后端统一管理）
- 需要对结果进行统一处理

#### Q4: 为什么选择 Ollama 本地大模型而不是云端 API？

**原因**：
1. **成本**：本地部署无需 API 调用费用
2. **隐私**：数据不出网
3. **可控**：可自由选择模型、调整参数
4. **开发**：无需网络即可使用

**缺点**：
- 需要本地 GPU/CPU 资源
- 模型能力有限（8B 参数）

#### Q5: 项目如何处理高并发？

**当前方案**（开发版）：
- 无状态设计，便于水平扩展
- 数据库连接池（HikariCP）

**优化方向**：
- Redis 缓存热点数据
- 接口限流
- 异步处理 AI 请求
- CDN 加速静态资源

---

### 13.8 总结

本节详细讲解了 rideSketch 项目从启动到运行的完整流程：

1. **启动前准备**：MySQL、Redis、Ollama 必须先启动
2. **后端启动**：Spring Boot 十大步骤，容器初始化、Bean 注册、过滤器链配置
3. **前端启动**：Vite 开发服务器、Vue 应用初始化
4. **请求流程**：从前端请求到后端处理、数据库查询、第三方服务调用的完整链路
5. **数据流转**：整体架构的数据流向图
6. **代码索引**：关键文件的路径和作用

理解了这个流程，你就可以向别人完整地介绍这个项目是如何运行的。从用户在浏览器输入，到数据存储，再到返回结果，每一个环节都清晰可见。

---

*文档版本：1.1*
*更新时间：2026-02-22*
*作者：rideSketch 开发团队*
