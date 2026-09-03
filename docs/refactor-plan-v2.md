# rideSketch V2 重构计划（方案 B：Java 业务 + Python Agent 编排）

> 状态：待评审  
> 日期：2026-09-03  
> 目标分支：main（同步 GitHub：001VIsir/rideSketch）

## 1. 背景与目标

V1 已完成核心功能（用户、地图、路线规划、社区、RAG、骑行助手），但存在以下问题，本次按**方案 B**整体重构：

- Agent 与业务代码耦合、工具调用（Function Calling）未真正接通、记忆靠全量拼接 prompt；
- AI 能力散落在 Spring Boot 中，升级/换模型成本高；
- 缺少统一的接口契约、模块边界与可观测性。

### 重构原则

1. **业务（Java）与 AI 编排（Python）分离**：Spring Boot 只做业务 API、鉴权、数据、SSE 网关；对话编排、工具调度、RAG 检索全部下沉到独立 Python 服务。
2. **契约驱动**：后端统一 OpenAPI；agent-service 的工具由 backend OpenAPI 导出生成；前端类型由契约生成。
3. **地图与模型可替换**：MapProvider、ModelProvider 抽象，避免绑定高德 / Ollama。
4. **功能不缩水**：保留 V1 全部产品能力（见 §2），并补齐真正可用的 Agent 闭环。

### 技术基线

| 层 | 技术 |
|---|---|
| 前端 | Vue3 + TypeScript + Vite + Element Plus（沿用） |
| 业务后端 | Java 17 + Spring Boot 3.5 + MyBatis-Plus + Spring Security/JWT |
| Agent 服务 | Python 3.11+ / FastAPI / LangGraph / SSE |
| 数据 | MySQL 8、Redis 7（会话/缓存） |
| 向量 | Chroma（容器化，默认） |
| 模型 | Ollama（qwen3:8b / nomic-embed-text），抽象后兼容 OpenAI |
| 地图 | 高德（默认，保留现有 key），抽象后可切 OSM + MapLibre / BRouter |
| 部署 | Docker Compose 一套拉起 |

## 2. 现状与清理记录

- V1 全部代码已整体移出工作区（可恢复）：
  - `D:\IdeaProjects\rideSketch_v1_20260903`（完整副本，含 `.git`、`node_modules`、`target`）
  - `D:\IdeaProjects\rideSketch_backup_20260903`（源码 + 文档轻量快照）
- 工作区 `D:\IdeaProjects\rideSketch` 已清空，**保留原 `.git` 历史**；本文件为 V2 首个提交。
- 从 V1 迁移的输入物：
  - 数据库 Schema：`docs/init.sql`（v1 备份内）
  - 接口清单与请求示例：v1 `docs/TEST_GUIDE.md`、`docs/TEST.md`
  - 前端页面/组件结构：v1 `frontend/src/**`
  - 知识库内容与入库逻辑：v1 `chroma_service.py`、`KnowledgeBaseLoader` 的 100+ 条骑行知识
  - 路线算法：骑行/多点/图案路线逻辑（v1 `RouteServiceImpl`、`PatternRouteServiceImpl`）
  - Bruno 接口集合：v1 `bruno/`（V2 阶段重建）

## 3. 目标架构（方案 B）

```text
Browser (Vue3)
   │ REST + SSE(EventSource)
   ▼
Spring Boot backend  :8080
   ├─ 用户/认证 (JWT)      ── MySQL / Redis
   ├─ 业务 API (/api/v2/*) ── 地图 / 路线 / 社区
   └─ SSE 网关 (/api/v2/agent/chat)   ← 统一鉴权、CORS、限流
        │  内部调用（用户透传 token + 服务 JWT）
        ▼
Python agent-service :8000  (FastAPI + LangGraph)
   ├─ 编排图：意图 → 工具选择 → 执行 → RAG 增强 → 生成 → 事件流
   ├─ tools：route_plan / map_search / community / rag_search
   ├─ memory：Redis(会话) + backend(用户偏好/长期记忆) + 向量库(知识)
   └─ LLM：Ollama（可切 OpenAI 兼容）
```

### 职责边界

| 能力 | 归属 | 说明 |
|---|---|---|
| 注册/登录/JWT、用户资料 | backend | 唯一认证源 |
| 地图（地理编码/POI/选点） | backend | 业务 API；前端地图交互仍可直接调高德 JS API |
| 路线规划（骑行/步行/多点/图案） | backend | 复用 V1 算法，抽象路线引擎 |
| 社区（发布/列表/点赞/评论） | backend | 纯业务 |
| 对话编排 / 工具调度 / 多轮状态 | agent-service | LangGraph 状态机，不在 controller 里堆 prompt |
| 工具执行 | agent-service 调 backend API | 统一工具 schema（名称/描述/参数 JSON Schema） |
| RAG 入库 / 检索 / 引用回答 | agent-service | 入库脚本可独立运行 |
| 会话记忆 | agent-service + Redis | 滑动窗口 + checkpointer |
| 用户偏好（长期记忆） | backend 表 + agent 读写 | 通过偏好 API |

## 4. Monorepo 目录结构

```text
rideSketch/
├── backend/                    # Spring Boot（业务 + 鉴权 + SSE 网关），端口 8080
│   ├── src/main/java/org/ridesketch/
│   │   ├── controller/         # 薄控制器（user/map/route/community/agent-proxy）
│   │   ├── application/        # 应用服务（用例编排、事务边界）
│   │   ├── domain/             # 领域模型与规则
│   │   ├── infrastructure/     # mapper、redis、http client（高德）、security
│   │   └── common/             # Result、异常、工具
│   ├── src/main/resources/db/  # Flyway 迁移脚本（V1__*.sql ...）
│   └── pom.xml
├── agent-service/              # Python FastAPI + LangGraph，端口 8000
│   ├── app/
│   │   ├── main.py             # FastAPI 入口（/chat/stream、/health）
│   │   ├── api/                # 路由层
│   │   ├── graph/              # state / nodes / 编排图定义
│   │   ├── tools/              # route_planner.py / map_search.py / community.py / rag.py
│   │   ├── memory/             # redis.py / preference.py / vector.py
│   │   ├── llm/                # model_provider.py（Ollama / OpenAI 兼容）
│   │   └── schemas/            # 事件与请求模型
│   ├── scripts/                # 知识库入库、工具 schema 生成
│   ├── tests/                  # pytest（LLM 调用录制回放）
│   └── pyproject.toml
├── frontend/                   # Vue3 + TS + Vite + Element Plus，端口 5173
│   └── src/
│       ├── api/                # 契约生成或手写 client（/api/v2）
│       ├── views/ components/ stores/ router/ types/ utils/
├── deploy/
│   ├── docker-compose.yml      # mysql / redis / chroma / agent-service / backend / frontend
│   └── Dockerfile.*
├── docs/
│   ├── refactor-plan-v2.md     # 本文件
│   ├── api-contract.md         # 接口契约清单（v1 → v2 映射）
│   ├── db-v2.md                # 数据模型设计（v1 → v2）
│   └── adr/                    # 架构决策记录
├── bruno/                      # V2 接口测试集合（复用 V1 习惯）
└── scripts/                    # 初始化 / 备份 / 迁移辅助脚本
```

## 5. 关键设计约定

### 5.1 backend

- 分层：`controller → application service → domain → repository`；禁止 controller 直接操作 mapper。
- 统一返回 `Result<T>`，错误码集中定义；异常由全局处理器转换。
- API 前缀统一 `/api/v2`；`springdoc-openapi` 挂载 `/v3/api-docs` 供 agent 工具生成与联调。
- 认证：JWT（沿用 V1 方案）；为 agent-service 签发**服务账号 JWT**（scope=agent），同时支持透传用户 token。
- SSE 网关：`POST /api/v2/agent/chat`（SSE 响应），把用户 token 透传给 agent-service，把事件流转发前端；**backend 不包含任何 LLM 依赖**。
- ORM 默认 MyBatis-Plus（降低迁移成本），但 repository 接口显式定义，业务不依赖 MP 实体注解。

### 5.2 agent-service

- LangGraph 单图起步：`classify → plan_tools → call_tool → rag_augment → answer`；需要时再拆子图/多 Agent。
- 工具标准：名称 + 描述 + 参数 JSON Schema + backend 端点映射；工具执行统一捕获异常并返回结构化错误。
- 事件协议（SSE）：`meta`（会话/模型）、`token`（增量）、`tool_start` / `tool_end`（含入参摘要与结果摘要）、`done`、`error`。
- 记忆：会话走 Redis（TTL），多轮上下文由 checkpointer 管理；长期偏好读写 backend 偏好 API。
- 配置化：模型端点、温度、工具清单全部 env/配置文件控制，不硬编码。
- 可观测：结构化日志 +（可选）Langfuse trace；记录每次对话 token 用量到统计表。

### 5.3 数据（v1 → v2）

1. 盘点 V1 表与数据（保留原库 `ridesketch` 只读，作为迁移基准）。
2. 产出 `docs/db-v2.md`：每张表“保留/改造/废弃”决定 + 字段映射。
3. Flyway 脚本管理 schema；提供一次性数据迁移脚本与回滚方案。

### 5.4 部署

- 开发：本机 MySQL/Redis + backend/agent-service/frontend 三端 dev server。
- 联调/演示：`docker compose up` 一键拉起（Ollama 可配置为外部服务）。

## 6. 分阶段实施计划

| 阶段 | 内容 | 验收标准 |
|---|---|---|
| Phase 0 骨架 | backend / agent-service / frontend 三端空壳 + health；compose 起 MySQL/Redis/Chroma；docs 框架 | 三端 health 200；compose 各容器健康 |
| Phase 1 用户与数据 | DB V2 迁移脚本；注册/登录/JWT；用户资料 API；springdoc 就绪 | Bruno 回归：注册→登录→me 全绿 |
| Phase 2 业务 API | map、route（骑行/步行/多点/图案）、community 迁移到 /api/v2 | V1 核心用例对照测试通过；`docs/api-contract.md` 完成映射 |
| Phase 3 Agent 主体 | agent-service（FastAPI+LangGraph+SSE）+ 3 个工具（route/map/community）+ Redis 会话 + backend SSE 网关 | 对话可流式输出并真实调用路线规划工具 |
| Phase 4 RAG | 知识入库脚本（迁移 V1 知识）+ 向量检索 + 引用回答 | 抽样问答准确率达标，入库可重复执行（幂等） |
| Phase 5 前端 | 页面/组件迁移到 /api/v2；聊天页接入 SSE；状态管理收敛 | E2E：登录→规划→发布→点赞→对话 主流程通过 |
| Phase 6 观测与交付 | token 统计、pytest/JUnit、Docker 生产化、README/AGENTS/docs 更新 | 一键部署 + 全量测试报告 |

每个阶段结束：实现 → 自测 → `commit`（中文描述）→ `push origin main`。

## 7. 待确认决策（默认值）

1. **地图**：默认保留高德（key 已有）；抽象 MapProvider，备选 OSM + MapLibre / BRouter（规避高德商用授权与稳定性风险）。→ 默认：高德，Phase 2 前确认。
2. **向量库**：默认 Chroma（沿用 V1 经验，容器化）；数据量大再评估 Milvus / pgvector。
3. **模型**：默认 Ollama qwen3:8b；抽象 ModelProvider，Phase 3 联调若工具调用质量差，切换 OpenAI 兼容模型。
4. **ORM**：默认 MyBatis-Plus；若重构中希望弱化 SQL，可评估 Spring Data JPA（会显著增加迁移工作量）。
5. **分支策略**：默认继续在 `main` 提交（清理 + 计划已随本文件提交）；如需保留线上可运行版本，可改在 `refactor/v2` 分支推进，评审后合入 main。

## 8. 风险与对策

| 风险 | 对策 |
|---|---|
| 数据迁移破坏社区数据 | v1 库只读保留；映射文档先行；迁移脚本可回滚 |
| Agent 过度设计 | 先 3~5 个工具跑通闭环，再按需扩展；不做无收益的框架堆叠 |
| qwen3:8b 工具调用不稳定 | 工具描述精调、温度下调；预留云端模型开关 |
| 高德商用授权 / 地图不可用 | MapProvider 抽象，前端地图层保留替换空间 |
| Python 服务与 Java 团队技能差 | 服务规模控制（编排+工具），边界清晰，样例先行 |
| 本地模型延迟高 | SSE 流式 + 异步；关键链路可切云端模型 |

## 9. 文档与协作约定

- 提交信息使用中文、描述性。
- 每次 Phase 完成同步更新本计划状态与 `docs/progress.md`。
- 问题与踩坑记录到 `docs/problem.md`。
- 环境变量与密钥不进仓库，统一由 `.env.example` + 本地 `.env` 提供。
