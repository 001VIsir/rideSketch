# Spring AI 升级规划方案

## 目标
使用Spring AI全面升级rideSketch项目的AI能力，引入向量数据库支持RAG（检索增强生成），提升AI路线规划的智能化水平。

## 当前状态
- 使用RestTemplate手动调用Ollama API
- Prompt硬编码在代码中
- 缺乏结构化输出支持
- 无向量存储能力

## 升级计划

### Phase 1: 基础Spring AI集成
1. **添加Spring AI依赖**
   - spring-ai-ollama-spring-boot-starter
   - spring-ai-spring-boot-starter
   - 配置Ollama连接

2. **重构AIRouteServiceImpl**
   - 使用OllamaChatClient替代RestTemplate
   - 使用PromptTemplate简化Prompt管理
   - 使用Spring AI的输出转换器

### Phase 2: 结构化输出与Function Calling
3. **实现Function Calling**
   - 定义GeocodeFunction、POISearchFunction等
   - 让LLM主动调用地理编码函数
   - 实现更智能的路线推荐

4. **结构化输出**
   - 使用AiResponse提取结构化数据
   - 简化JSON解析逻辑

### Phase 3: 向量数据库与RAG
5. **集成向量数据库**
   - 选择Chroma或Milvus（本地部署）
   - 或使用Pgvector（PostgreSQL插件）

6. **构建知识库**
   - 导入景点POI数据到向量库
   - 实现相似地点检索

7. **RAG流程**
   - 用户查询 -> 向量检索 -> 上下文增强 -> LLM生成

### Phase 4: AI Agent工作流
8. **实现AI Agent**
   - 使用Spring AI的Agent框架
   - 实现多步骤路线规划Agent
   - 支持对话式交互

## 技术选型
- Spring AI Version: 1.0.0-M4+ (最新稳定版)
- 向量数据库: Chroma (本地轻量) 或 Pgvector
- LLM: Ollama (本地) / OpenAI (可选)

## 预期收益
- 代码更简洁，AI调用更规范
- 支持Function Calling，LLM可主动查询地理位置
- RAG增强，提升推荐准确性
- 支持对话式路线规划

## 风险与注意事项
- Spring AI版本迭代较快，需注意兼容性
- 向量数据库需要额外部署
- RAG需要预先构建知识库数据
