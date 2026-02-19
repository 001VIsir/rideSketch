# 测试文档

本文档记录rideSketch项目的功能测试方法和测试结果。

---

## 目录

1. [测试环境](#测试环境)
2. [后端API测试](#后端api测试)
3. [AI功能测试](#ai功能测试)
4. [Chroma向量库测试](#chroma向量库测试)
5. [前端功能测试](#前端功能测试)
6. [测试结果汇总](#测试结果汇总)

---

## 测试环境

### 服务依赖

| 服务 | 地址 | 状态 | 说明 |
|------|------|------|------|
| MySQL | localhost:3306 | ✅ | 数据库：ridesketch |
| Redis | localhost:6379 | ✅ | 缓存 |
| Ollama | localhost:11434 | ✅ | 模型：qwen3:8b |
| 高德地图 | api.amap.com | ✅ | API Key已配置 |
| 后端服务 | localhost:8080 | ✅ | Spring Boot |
| 前端服务 | localhost:5173 | ✅ | Vite + Vue3 |

### 启动命令

```bash
# 后端
cd rideSketch
./mvnw spring-boot:run

# 前端
cd frontend
npm run dev

# Chroma向量服务（可选）
pip install chromadb flask
python chroma_service.py
```

---

## 后端API测试

### 1. 用户认证

#### 1.1 用户注册
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com"
  }'
```

**预期响应**：
```json
{
  "success": true,
  "message": "注册成功",
  "data": {
    "id": 1,
    "username": "testuser"
  }
}
```

#### 1.2 用户登录
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

**预期响应**：
```json
{
  "success": true,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

---

### 2. 地图功能

#### 2.1 地址搜索
```bash
curl -s "http://localhost:8080/api/map/search?keyword=天安门"
```

**预期响应**：
```json
{
  "success": true,
  "data": [
    {
      "name": "天安门广场",
      "address": "北京市东城区东长安街",
      "location": {
        "lng": 116.397428,
        "lat": 39.90923
      }
    }
  ]
}
```

#### 2.2 地理编码
```bash
curl -s "http://localhost:8080/api/map/geocode?address=北京市天安门"
```

**预期响应**：
```json
{
  "success": true,
  "data": {
    "location": {
      "lng": 116.397428,
      "lat": 39.90923
    }
  }
}
```

#### 2.3 逆地理编码
```bash
curl -s "http://localhost:8080/api/map/regeo?location=116.397428,39.90923"
```

**预期响应**：
```json
{
  "success": true,
  "data": {
    "address": "北京市东城区东长安街",
    "province": "北京市",
    "city": "北京市",
    "district": "东城区"
  }
}
```

---

### 3. 路线规划

#### 3.1 骑行路线规划
```bash
curl -s -X GET "http://localhost:8080/api/route/riding?origin=116.358104,39.961554&destination=116.427428,39.929226"
```

**预期响应**：
```json
{
  "success": true,
  "data": {
    "status": "1",
    "route": {
      "paths": [
        {
          "distance": "8800",
          "duration": "2123",
          "strategy": "10",
          "steps": [...]
        }
      ]
    }
  }
}
```

#### 3.2 多途经点路线
```bash
curl -s -X POST "http://localhost:8080/api/route/plan" \
  -H "Content-Type: application/json" \
  -d '{
    "origin": "116.358104,39.961554",
    "destination": "116.427428,39.929226",
    "waypoints": "116.373100,39.941200|116.397428,39.909226",
    "mode": "riding"
  }'
```

**预期响应**：
```json
{
  "success": true,
  "data": {
    "status": "1",
    "route": {
      "paths": [
        {
          "distance": "14900",
          "duration": "3574"
        }
      ]
    }
  }
}
```

---

## AI功能测试

### 1. Spring AI集成

#### 1.1 AI路线规划
```bash
curl -s -X POST http://localhost:8080/api/route/ai-plan \
  -H "Content-Type: application/json" \
  -d '{
    "description": "从北京邮电大学出发，骑行30公里，希望经过景点",
    "city": "北京"
  }'
```

**预期响应**：
```json
{
  "success": true,
  "data": {
    "status": "1",
    "info": "AI路线规划成功",
    "origin": "116.358104,39.961554",
    "destination": "116.427428,39.929226"
  }
}
```

**测试要点**：
- [ ] Ollama服务正常运行
- [ ] Spring AI ChatClient正确配置
- [ ] AI能够解析用户输入
- [ ] 返回结构化的路线数据

---

### 2. RAG知识库

#### 2.1 关键词检索（基础版）
```bash
curl -s -X POST http://localhost:8080/api/rag/question \
  -H "Content-Type: application/json" \
  -d '{"question": "骑行的安全注意事项有哪些？"}'
```

**预期响应**：
```json
{
  "success": true,
  "data": "骑行安全注意事项：1.始终佩戴头盔..."
}
```

**测试要点**：
- [ ] RagService正确初始化
- [ ] 知识库包含5类骑行知识
- [ ] 关键词匹配检索工作正常
- [ ] AI能够结合知识库回答

---

### 3. Chroma向量库（高级版）

> **注意**：需要先启动Chroma服务

#### 3.1 启动Chroma服务
```bash
# 安装依赖
pip install chromadb flask

# 启动服务
python chroma_service.py
```

#### 3.2 健康检查
```bash
curl -s http://localhost:5000/health
```

**预期响应**：
```json
{
  "status": "ok",
  "collection": "cycling-knowledge",
  "document_count": 7
}
```

#### 3.3 语义搜索
```bash
curl -s -X POST http://localhost:5000/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "如何保证骑行安全？",
    "n_results": 3
  }'
```

**预期响应**：
```json
{
  "success": true,
  "results": [
    {
      "id": "safety_1",
      "content": "骑行安全注意事项...",
      "metadata": {"category": "骑行安全"},
      "distance": 0.234
    }
  ]
}
```

#### 3.4 完整RAG问答
```bash
curl -s -X POST http://localhost:8080/api/chroma/question \
  -H "Content-Type: application/json" \
  -d '{"question": "初学者应该选择什么路线？"}'
```

**预期响应**：
```json
{
  "success": true,
  "data": "根据知识库，初学者建议选择天安门-故宫-景山路线..."
}
```

**测试要点**：
- [ ] Chroma服务正常启动
- [ ] 知识库初始化7条文档
- [ ] 向量相似度搜索工作正常
- [ ] RAG问答能够检索相关知识并生成回答

---

## 前端功能测试

### 测试方法

1. 打开浏览器访问 `http://localhost:5173`
2. 检查以下功能

#### 1. 地图显示
- [ ] 地图正确加载
- [ ] 高德API Key有效
- [ ] 地图交互正常（缩放、平移）

#### 2. 地址搜索
- [ ] 搜索框可用
- [ ] 搜索结果正确显示
- [ ] 点击结果地图定位正确

#### 3. 坐标拾取
- [ ] 拾取模式可切换
- [ ] 点击地图获取坐标
- [ ] 坐标显示正确

#### 4. 路线规划（如果已实现）
- [ ] 路线规划面板显示
- [ ] 起点/终点选择功能
- [ ] 出行方式切换（骑行/步行）
- [ ] 路线结果显示
- [ ] 地图上绘制路线

#### 5. AI路线规划（如果已实现）
- [ ] AI规划面板显示
- [ ] 描述输入框可用
- [ ] AI返回结果正确展示

---

## 测试结果汇总

### 后端API测试结果

| 功能 | 测试用例 | 状态 | 备注 |
|------|---------|------|------|
| 用户注册 | F001 | ✅ 通过 | |
| 用户登录 | F002 | ✅ 通过 | JWT token生成正常 |
| 地址搜索 | F101 | ✅ 通过 | |
| 地理编码 | F102 | ✅ 通过 | |
| 逆地理编码 | F103 | ✅ 通过 | |
| 坐标拾取 | F104 | ✅ 通过 | |
| 骑行路线规划 | F201 | ✅ 通过 | 8.8km路线正常 |
| 步行路线规划 | F201 | ⚠️ 限制 | 高德API限制 |
| 多途经点路线 | F203 | ✅ 通过 | 14.9km路线正常 |
| AI路线规划 | F202 | ✅ 通过 | Ollama工作正常 |

### AI功能测试结果

| 功能 | 状态 | 备注 |
|------|------|------|
| Spring AI集成 | ✅ 通过 | ChatClient配置正确 |
| RAG知识库 | ✅ 通过 | 5类知识已加载 |
| 关键词检索 | ✅ 通过 | Map-based匹配 |
| Chroma向量库 | ✅ 通过 | 7条知识已初始化 |
| 语义搜索 | ✅ 通过 | 相似度计算正常 |

### 前端测试结果

| 功能 | 状态 | 备注 |
|------|------|------|
| 地图显示 | ❌ | 需要有效的API Key |
| 地址搜索 | ✅ 通过 | |
| 坐标拾取 | ✅ 通过 | |

---

## 已知问题

### 1. 高德地图API Key
- **问题**：地图显示时报错 `USERKEY_PLAT_NOMATCH`
- **解决方案**：申请新的Web端JS API Key

### 2. 步行路线规划
- **问题**：高德API返回 `SERVICE_NOT_AVAILABLE`
- **原因**：可能是API配额或服务问题
- **解决方案**：使用骑行路线作为备选

### 3. Chroma服务依赖
- **问题**：需要单独启动Python服务
- **解决方案**：
  - 开发环境：手动启动 `python chroma_service.py`
  - 生产环境：可以使用Docker容器化

---

## 测试报告模板

### 新功能测试报告

```
## [功能名称] 测试报告

### 测试环境
- 日期：
- 测试人员：
- 版本：

### 测试用例

| 用例ID | 用例描述 | 预期结果 | 实际结果 | 状态 |
|--------|---------|---------|---------|------|
|        |         |         |         |      |

### 问题记录

| 问题ID | 问题描述 | 严重程度 | 解决方案 |
|--------|---------|---------|---------|
|        |         |         |         |

### 测试结论
- 通过：X 个
- 失败：X 个
- 总计：X 个
```

---

*文档更新于：2026-02-20*
*作者：Claude Code*
*版本：1.0*
