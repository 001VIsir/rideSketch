# 开发进度日志

## 2026-03-17

### dev_2 分支全项目优化重构（第二轮）

- [x] 后端社区查询性能优化：`CommunityServiceImpl` 消除列表 N+1（批量用户/点赞映射）
- [x] 后端健壮性增强：点赞取消路径增加下限保护，避免负数点赞
- [x] 前端 API 层收敛：新增 `frontend/src/api/http.ts`，统一 token 注入与 401 处理
- [x] 前端重复逻辑去重：`community.ts` 抽取 `toggleLike` 复用逻辑
- [x] 新增测试：`CommunityServiceImplTest`（批量查询行为 + 点赞下限保护）
- [x] 输出重构报告：`docs/dev_2_refactor_report_20260317.md`

### 验证结果

- 后端新增测试：`./mvnw.cmd test -Dtest=CommunityServiceImplTest` ✅（2 passed）
- 后端全量测试：`./mvnw.cmd test` ✅（17 run, 0 fail）
- 后端构建打包：`./mvnw.cmd clean package -DskipTests` ✅
- 前端构建：`frontend npm run build` ✅
- 前端测试脚本现状：`frontend` 当前无 `npm run test` 脚本（历史现状）

---

## 2026-02-21

### RAG知识库完善

- [x] 下载nomic-embed-text embedding模型
- [x] 添加Spring AI Ollama embedding依赖
- [x] 新增EmbeddingService向量嵌入服务
- [x] 新增KnowledgeBaseLoader知识库加载器
- [x] 重构RagServiceImpl使用向量检索
- [x] 扩展知识库到97条（9个类别）
- [x] 测试验证向量语义检索功能

### 验证结果

- 向量检索正常工作
- 相似度计算准确
- 返回top-K相关知识

### API测试结果

| 功能 | API | 状态 |
|------|-----|------|
| 地图搜索 | GET /api/map/search | ✅ 正常 |
| 地理编码 | GET /api/map/geocode | ✅ 正常 |
| 骑行路径规划 | GET /api/route/riding | ✅ 正常 |
| AI智能路径规划 | POST /api/route/ai-plan | ✅ 正常 |
| 图案路书生成 | POST /api/route/pattern | ✅ 正常 |
| RAG语义检索 | GET /api/rag/search | ✅ 正常 |
| 知识类别 | GET /api/rag/categories | ✅ 正常 |

---

## 2026-02-19

### 初始化项目

- [x] 创建项目初始结构
- [x] 配置Spring Boot基础依赖
- [x] 获取高德地图API文档
- [x] 编写需求规格说明书 (docs/requirement.md)
- [x] 编写功能列表 (docs/feature_list.md)
- [x] 启动开发环境
- [x] 验证基础功能

### 已完成

- 实现用户注册/登录功能 (F001, F002)
- 实现地图基础功能 (F101-F104)
- 实现基础路径规划功能 (F201)
- 实现AI智能路线规划功能 (F202)
- 实现多途经点路线规划功能 (F203)
- 实现图案路书生成功能 (F301)
- 实现路线量化数据展示功能 (F302)
- 实现社区论坛功能 (F401-F406)
- 实现用户信息管理功能 (F003)

### 待开始

（所有功能已实现）

### 今日完成

- 实现用户登录页面 (F002)
- 实现用户注册页面 (F001)
- 实现基础路线规划前端 (F201)
- 实现AI智能路线规划前端 (F202)
- 实现图案路书生成前端 (F301)
- 实现路线量化数据展示 (F302)
- 实现社区论坛前端 (F401-F406)
