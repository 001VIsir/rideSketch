# rideSketch API 测试完整指南

> 本文档面向测试工程师，从零开始讲解如何像大公司测开一样进行API测试。包括工具选择、测试用例设计、接口测试、自动化测试、持续集成等完整流程。

---

## 目录

1. [测试前准备](#1-测试前准备)
2. [Postman 接口测试](#2-postman-接口测试)
3. [JMeter 性能测试](#3-jmeter-性能测试)
4. [自动化测试](#4-自动化测试)
5. [测试用例设计](#5-测试用例设计)
6. [测试报告模板](#6-测试报告模板)
7. [CI持续集成](#7-ci持续集成)

---

## 1. 测试前准备

### 1.1 确保服务已启动

在开始测试前，确保以下服务已启动：

```bash
# 1. 启动MySQL
# 方式1: Docker
docker run -d --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=Cqian1231 mysql:8

# 2. 启动Redis
docker run -d --name redis -p 6379:6379 redis:alpine

# 3. 启动Ollama (如需测试AI功能)
ollama serve
ollama pull qwen3:8b
ollama pull nomic-embed-text

# 4. 启动后端
cd D:\IdeaProjects\rideSketch
.\mvnw.cmd spring-boot:run

# 5. 启动前端 (可选，如需测试完整流程)
cd frontend
npm run dev
```

### 1.2 安装测试工具

| 工具 | 用途 | 下载地址 |
|------|------|---------|
| Postman | 接口测试、功能验证 | https://www.postman.com/downloads/ |
| JMeter | 性能测试、压力测试 | https://jmeter.apache.org/ |
| Newman | Postman命令行运行 | `npm install -g newman` |
| IntelliJ IDEA | Java单元测试 | 内置 |

### 1.3 项目API清单

测试前先了解项目的所有API接口：

```
骑迹 rideSketch API 接口清单
==========================

基础信息
- 基础URL: http://localhost:8080
- 认证方式: JWT Token (Bearer Token)

一、认证接口 (auth)
------------------
1. 用户注册
   POST /api/auth/register
   Body: {"username": "test", "email": "test@test.com", "password": "123456", "nickname": "测试"}

2. 用户登录
   POST /api/auth/login
   Body: {"usernameOrEmail": "test", "password": "123456"}
   返回: {"success": true, "data": {"token": "xxx", "userId": 1, "username": "test"}}

3. 获取当前用户信息
   GET /api/auth/me
   Header: Authorization: Bearer <token>

4. 更新用户信息
   PUT /api/auth/me
   Header: Authorization: Bearer <token>
   Body: {"nickname": "新昵称", "avatar": "url"}

二、地图接口 (map)
------------------
5. 地址搜索
   GET /api/map/search?keyword=天安门&city=北京

6. 地理编码 (地址→坐标)
   GET /api/map/geocode?address=北京市天安门

7. 逆地理编码 (坐标→地址)
   GET /api/map/regeo?longitude=116.397&latitude=39.909

三、路线规划接口 (route)
------------------------
8. 骑行路线规划
   GET /api/route/riding?origin=116.397,39.909&destination=116.418,39.928

9. 步行路线规划
   GET /api/route/walking?origin=116.397,39.909&destination=116.418,39.928

10. 多途经点路线规划
    POST /api/route/plan
    Body: {
        "origin": "116.397,39.909",
        "destination": "116.418,39.928",
        "waypoints": "116.405,39.910|116.410,39.915",
        "mode": "riding"
    }

11. AI智能路线规划
    POST /api/route/ai-plan
    Body: {"description": "从天安门骑到颐和园，看看风景"}

12. 图案路书生成
    POST /api/route/pattern
    Body: {
        "pattern": "心形",
        "city": "北京",
        "description": "浪漫骑行"
    }

四、社区接口 (community)
------------------------
13. 获取路线列表
    GET /api/community/routes?page=1&size=10

14. 获取我的路线 (需登录)
    GET /api/community/my-routes
    Header: Authorization: Bearer <token>

15. 获取路线详情
    GET /api/community/route/{id}

16. 发布路线 (需登录)
    POST /api/community/route
    Header: Authorization: Bearer <token>
    Body: {
        "title": "测试路线",
        "description": "很棒的路线",
        "startPoint": "天安门",
        "endPoint": "颐和园",
        "waypoints": "[]",
        "routePath": "116.397,39.909;116.398,39.910",
        "totalDistance": 25.5,
        "estimatedTime": 90,
        "difficulty": 1,
        "tags": "[\"休闲\"]",
        "isPublic": 1
    }

17. 更新路线 (需登录，仅作者)
    PUT /api/community/route/{id}
    Header: Authorization: Bearer <token>

18. 删除路线 (需登录，仅作者)
    DELETE /api/community/route/{id}
    Header: Authorization: Bearer <token>

19. 点赞/取消点赞 (需登录)
    POST /api/community/route/{id}/like
    Header: Authorization: Bearer <token>

20. 获取评论列表
    GET /api/community/route/{id}/comments

21. 添加评论 (需登录)
    POST /api/community/route/{id}/comment
    Header: Authorization: Bearer <token>
    Body: {"content": "评论内容", "parentId": null}

22. 删除评论 (需登录，仅作者)
    DELETE /api/community/comment/{id}
    Header: Authorization: Bearer <token>

五、RAG知识库接口 (rag)
-----------------------
23. 问答
    GET /api/rag/question?question=如何保护膝盖？

24. 语义搜索
    GET /api/rag/search?keyword=骑行安全

25. 获取知识分类
    GET /api/rag/categories

26. 健康检查
    GET /api/rag/health

六、Agent接口 (agent)
---------------------
27. 智能对话
    POST /api/agent/chat
    Header: Authorization: Bearer <token> (可选)
    Body: {"message": "帮我规划一条路线"}

28. 清除记忆
    DELETE /api/agent/memory
    Header: Authorization: Bearer <token>

29. 获取建议问题
    GET /api/agent/suggestions

30. 健康检查
    GET /api/agent/health
```

---

## 2. Postman 接口测试

### 2.1 Postman 基础使用

#### 步骤1: 创建Collection

1. 打开Postman
2. 点击左侧 `Collections` → `+` 创建新集合
3. 命名为 `rideSketch API Test`
4. 可以按模块创建子文件夹: `Auth`, `Map`, `Route`, `Community`, `RAG`

#### 步骤2: 创建环境变量

1. 点击右上角 `Environments` → `+` 创建环境
2. 命名为 `rideSketch-Dev`
3. 添加变量：

| 变量名 | 初始值 | 说明 |
|--------|--------|------|
| `baseUrl` | http://localhost:8080 | 基础URL |
| `token` | (留空) | 登录后自动填充 |
| `userId` | (留空) | 登录后自动填充 |

4. 在请求中使用: `{{baseUrl}}/api/auth/login`

#### 步骤3: 测试登录接口

1. 在Collection中创建新请求
2. 配置:

```
请求名称: 01-用户登录
请求方法: POST
请求URL: {{baseUrl}}/api/auth/login
请求体 (Body): raw - JSON
{
    "usernameOrEmail": "test",
    "password": "123456"
}
```

3. 点击 `Send` 发送请求
4. **保存Response中的token**:
   - 点击 `Tests` 标签
   - 添加脚本:

```javascript
// 提取token并设置为环境变量
var jsonData = pm.response.json();
if (jsonData.success && jsonData.data.token) {
    pm.environment.set("token", jsonData.data.token);
    pm.environment.set("userId", jsonData.data.userId);
}
```

#### 步骤4: 测试需要认证的接口

1. 创建新请求
2. 在 `Headers` 中添加:

| Key | Value |
|-----|-------|
| Authorization | Bearer {{token}} |

3. 发送请求测试

### 2.2 Postman 进阶: 自动化测试脚本

在每个请求的 `Tests` 标签中添加测试脚本：

#### 示例1: 测试响应状态码

```javascript
// 测试状态码为200
pm.test("状态码应为200", function() {
    pm.response.to.have.status(200);
});

// 测试业务成功
pm.test("业务应成功", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.success).to.be.true;
});
```

#### 示例2: 测试响应时间

```javascript
// 响应时间应小于500ms
pm.test("响应时间应小于500ms", function() {
    pm.expect(pm.response.responseTime).to.be.below(500);
});
```

#### 示例3: 测试返回数据格式

```javascript
// 验证返回数据结构
pm.test("返回数据包含token", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data).to.have.property('token');
    pm.expect(jsonData.data).to.have.property('userId');
});
```

#### 示例4: 测试路线列表

```javascript
// 测试路线列表接口
pm.test("路线列表应返回数组", function() {
    var jsonData = pm.response.json();
    pm.expect(jsonData.data).to.be.an('array');
});

pm.test("路线应包含必要字段", function() {
    var jsonData = pm.response.json();
    if (jsonData.data.length > 0) {
        pm.expect(jsonData.data[0]).to.have.property('id');
        pm.expect(jsonData.data[0]).to.have.property('title');
    }
});
```

### 2.3 Postman 完整测试 Collection

创建一个完整的测试Collection，包含所有接口的测试：

```javascript
// ===== 01-用户注册 =====
{
    "name": "01-用户注册",
    "request": {
        "method": "POST",
        "url": "{{baseUrl}}/api/auth/register",
        "body": {
            "mode": "raw",
            "raw": "{\"username\": \"test{{$randomInt}}\", \"email\": \"test{{$randomInt}}@test.com\", \"password\": \"123456\", \"nickname\": \"测试用户\"}"
        }
    },
    "tests": [
        "pm.test('注册成功', function() { pm.expect(pm.response.json().success).to.be.true; });"
    ]
}

// ===== 02-用户登录 =====
{
    "name": "02-用户登录",
    "request": {
        "method": "POST",
        "url": "{{baseUrl}}/api/auth/login",
        "body": {
            "mode": "raw",
            "raw": "{\"usernameOrEmail\": \"test\", \"password\": \"123456\"}"
        }
    },
    "event": [
        {
            "listen": "test",
            "script": {
                "exec": [
                    "var jsonData = pm.response.json();",
                    "if (jsonData.success && jsonData.data.token) {",
                    "    pm.environment.set('token', jsonData.data.token);",
                    "    pm.environment.set('userId', jsonData.data.userId);",
                    "}",
                    "pm.test('登录成功', function() { pm.expect(jsonData.success).to.be.true; });"
                ]
            }
        }
    ]
}

// ===== 03-获取用户信息 =====
{
    "name": "03-获取用户信息",
    "request": {
        "method": "GET",
        "url": "{{baseUrl}}/api/auth/me",
        "header": [
            {"key": "Authorization", "value": "Bearer {{token}}"}
        ]
    },
    "tests": [
        "pm.test('获取用户信息成功', function() { pm.expect(pm.response.json().success).to.be.true; });"
    ]
}

// ===== 04-地图搜索 =====
{
    "name": "04-地图搜索",
    "request": {
        "method": "GET",
        "url": "{{baseUrl}}/api/map/search",
        "params": {
            "keyword": "天安门",
            "city": "北京"
        }
    },
    "tests": [
        "pm.test('搜索成功', function() { pm.expect(pm.response.json().success).to.be.true; });"
    ]
}

// ===== 05-骑行路线规划 =====
{
    "name": "05-骑行路线规划",
    "request": {
        "method": "GET",
        "url": "{{baseUrl}}/api/route/riding",
        "params": {
            "origin": "116.397,39.909",
            "destination": "116.418,39.928"
        }
    },
    "tests": [
        "pm.test('路线规划成功', function() { pm.expect(pm.response.json().success).to.be.true; });",
        "pm.test('返回路线数据', function() { pm.expect(pm.response.json().data.route).to.exist; });"
    ]
}

// ===== 06-AI路线规划 =====
{
    "name": "06-AI路线规划",
    "request": {
        "method": "POST",
        "url": "{{baseUrl}}/api/route/ai-plan",
        "body": {
            "mode": "raw",
            "raw": "{\"description\": \"从天安门骑到颐和园，看看风景\"}"
        }
    },
    "tests": [
        "pm.test('AI规划成功', function() { pm.expect(pm.response.json().success).to.be.true; });"
    ]
}

// ===== 07-获取社区路线列表 =====
{
    "name": "07-获取路线列表",
    "request": {
        "method": "GET",
        "url": "{{baseUrl}}/api/community/routes",
        "params": {
            "page": "1",
            "size": "10"
        }
    },
    "tests": [
        "pm.test('获取列表成功', function() { pm.expect(pm.response.json().success).to.be.true; });",
        "pm.test('返回数组', function() { pm.expect(pm.response.json().data).to.be.an('array'); });"
    ]
}

// ===== 08-发布路线 =====
{
    "name": "08-发布路线",
    "request": {
        "method": "POST",
        "url": "{{baseUrl}}/api/community/route",
        "header": [
            {"key": "Authorization", "value": "Bearer {{token}}"},
            {"key": "Content-Type", "value": "application/json"}
        ],
        "body": {
            "mode": "raw",
            "raw": "{\"title\": \"测试路线{{$randomInt}}\", \"description\": \"测试描述\", \"startPoint\": \"天安门\", \"endPoint\": \"颐和园\", \"waypoints\": \"[]\", \"routePath\": \"116.397,39.909;116.398,39.910\", \"totalDistance\": 25.5, \"estimatedTime\": 90, \"difficulty\": 1, \"tags\": \"[\\\"休闲\\\"]\", \"isPublic\": 1}"
        }
    },
    "tests": [
        "pm.test('发布成功', function() { pm.expect(pm.response.json().success).to.be.true; });"
    ]
}

// ===== 09-RAG问答 =====
{
    "name": "09-RAG问答",
    "request": {
        "method": "GET",
        "url": "{{baseUrl}}/api/rag/question",
        "params": {
            "question": "如何保护膝盖？"
        }
    },
    "tests": [
        "pm.test('问答成功', function() { pm.expect(pm.response.json().success).to.be.true; });"
    ]
}
```

### 2.4 导出/导入 Collection

1. 导出: 右键Collection → Export → 选择JSON格式
2. 导入: Import → 选择JSON文件

---

## 3. JMeter 性能测试

### 3.1 JMeter 基础配置

#### 步骤1: 创建测试计划

1. 打开JMeter
2. 创建新测试计划: `rideSketch Performance Test`
3. 添加线程组: `Thread Group` (右键测试计划 → Add → Thread Group)

```
线程组配置:
- 线程数: 10 (模拟10个用户)
-  ramp-up时间: 10秒 (10秒内启动所有线程)
- 循环次数: 1 (每个线程执行1次)
```

#### 步骤2: 添加HTTP请求

1. 右键线程组 → Add → Sampler → HTTP Request
2. 配置:

```
名称: 用户登录
服务器名称或IP: localhost
端口号: 8080
方法: POST
路径: /api/auth/login
Body Data: {"usernameOrEmail": "test", "password": "123456"}
```

#### 步骤3: 添加Header

1. 右键HTTP请求 → Add → Config Element → HTTP Header Manager
2. 添加:

```
Content-Type: application/json
```

### 3.2 JMeter 高级配置

#### 场景1: 登录获取Token并保存

```
1. 添加 → Logic Controller → Loop Controller (循环2次)

2. 第一个HTTP请求: 登录
   - 路径: /api/auth/login
   - Body: {"usernameOrEmail": "test", "password": "123456"}

3. 添加 → Post Processors → JSON Extractor
   - Names of created variables: token
   - JSON Path expression: $.data.token
   - Match No: 1

4. 第二个HTTP请求: 获取用户信息
   - 路径: /api/auth/me
   - 添加HTTP Header Manager:
     Authorization: Bearer ${token}
```

#### 场景2: 社区接口性能测试

```
线程组配置:
- 线程数: 50
- ramp-up: 30秒
- 循环次数: 3

HTTP请求:
1. 获取路线列表 - GET /api/community/routes?page=1&size=20
2. 获取路线详情 - GET /api/community/route/1
3. 获取评论列表 - GET /api/community/route/1/comments
```

#### 场景3: 压力测试

```
目标: 测试系统能承受的最大并发

测试步骤:
1. 基准测试: 10并发 → 记录响应时间
2. 负载测试: 50并发 → 记录响应时间
3. 压力测试: 100并发 → 记录响应时间
4. 极限测试: 200并发 → 观察是否报错

配置:
- 线程数: 10 → 50 → 100 → 200
- ramp-up: 60秒
- 持续时间: 300秒 (5分钟)
- 勾选 "Same user on each iteration"
```

### 3.3 添加监听器

右键线程组 → Add → Listener，选择:

| 监听器 | 用途 |
|--------|------|
| View Results Tree | 查看每个请求的详细结果 |
| Summary Report | 汇总报告 (TPS、响应时间) |
| Aggregate Report | 聚合报告 (平均值、中位数) |
| Response Times Over Time | 响应时间趋势图 |
| Transactions per Second | TPS趋势图 |

### 3.4 运行测试并分析结果

#### 关键指标解读

| 指标 | 含义 | 合格标准 |
|------|------|----------|
| Samples | 请求总数 | - |
| Average | 平均响应时间 | < 500ms |
| Min | 最小响应时间 | - |
| Max | 最大响应时间 | < 2000ms |
| Error % | 错误率 | < 1% |
| Throughput | TPS (每秒处理请求数) | > 100 |

#### 生成测试报告

```bash
# 使用命令行生成HTML报告
cd /path/to/apache-jmeter/bin

# 运行测试
jmeter -n -t /path/to/testplan.jmx -l /path/to/result.jtl -e -o /path/to/html-report
```

### 3.5 JMeter 测试脚本示例

保存为 `rideSketch_Test.jmx`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jmeterTestPlan version="1.2" properties="5.0" jmeter="5.5">
  <hashTree>
    <!-- 测试计划 -->
    <TestPlan guiclass="TestPlanGui" testclass="TestPlan" testname="rideSketch API测试">
      <stringProp name="TestPlan.comments">rideSketch API性能测试</stringProp>
      <boolProp name="TestPlan.functional_mode">false</boolProp>
      <boolProp name="TestPlan.serialize_threadgroups">false</boolProp>
    </TestPlan>

    <!-- 线程组 -->
    <hashTree>
      <ThreadGroup guiclass="ThreadGroupGui" testclass="ThreadGroup" testname="基准测试-10并发">
        <stringProp name="ThreadGroup.num_threads">10</stringProp>
        <stringProp name="ThreadGroup.ramp_time">10</stringProp>
        <stringProp name="ThreadGroup.duration">60</stringProp>
        <stringProp name="ThreadGroup.on_sample_error">continue</stringProp>
      </ThreadGroup>

      <!-- HTTP Header Manager -->
      <hashTree>
        <HeaderManager guiclass="HeaderPanel" testclass="HeaderManager" testname="HTTP Header Manager">
          <collectionProp name="HeaderManager.headers">
            <elementProp name="" elementType="Header">
              <stringProp name="Header.name">Content-Type</stringProp>
              <stringProp name="Header.value">application/json</stringProp>
            </elementProp>
          </collectionProp>
        </HeaderManager>

        <!-- 登录请求 -->
        <hashTree>
          <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="01-用户登录">
            <stringProp name="HTTPSampler.domain">localhost</stringProp>
            <stringProp name="HTTPSampler.port">8080</stringProp>
            <stringProp name="HTTPSampler.path">/api/auth/login</stringProp>
            <stringProp name="HTTPSampler.method">POST</stringProp>
            <boolProp name="HTTPSampler.follow_redirects">true</boolProp>
            <boolProp name="HTTPSampler.auto_redirects">false</boolProp>
            <stringProp name="HTTPSampler.body">{"usernameOrEmail": "test", "password": "123456"}</stringProp>
          </HTTPSamplerProxy>

          <!-- 提取Token -->
          <hashTree>
            <JSONExtractor guiclass="JSONExtractorGui" testclass="JSONExtractor" testname="JSON Extractor">
              <stringProp name="JSONExtractor.names">token</stringProp>
              <stringProp name="JSONExtractor.matchNumbers">1</stringProp>
              <stringProp name="JSONExtractor.refnames">token</stringProp>
              <stringProp name="JSONExtractor.jsonPathExprs">$.data.token</stringProp>
            </JSONExtractor>
          </hashTree>
        </hashTree>

        <!-- 获取用户信息 -->
        <hashTree>
          <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="02-获取用户信息">
            <stringProp name="HTTPSampler.domain">localhost</stringProp>
            <stringProp name="HTTPSampler.port">8080</stringProp>
            <stringProp name="HTTPSampler.path">/api/auth/me</stringProp>
            <stringProp name="HTTPSampler.method">GET</stringProp>
          </HTTPSamplerProxy>
        </hashTree>

        <!-- 路线列表 -->
        <hashTree>
          <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="03-路线列表">
            <stringProp name="HTTPSampler.domain">localhost</stringProp>
            <stringProp name="HTTPSampler.port">8080</stringProp>
            <stringProp name="HTTPSampler.path">/api/community/routes</stringProp>
            <stringProp name="HTTPSampler.method">GET</stringProp>
          </HTTPSamplerProxy>
        </hashTree>

        <!-- 骑行路线规划 -->
        <hashTree>
          <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="04-骑行路线规划">
            <stringProp name="HTTPSampler.domain">localhost</stringProp>
            <stringProp name="HTTPSampler.port">8080</stringProp>
            <stringProp name="HTTPSampler.path">/api/route/riding</stringProp>
            <stringProp name="HTTPSampler.method">GET</stringProp>
            <stringProp name="HTTPSampler.url"></stringProp>
            <urlParams>
              <Prop name="origin">116.397,39.909</Prop>
              <Prop name="destination">116.418,39.928</Prop>
            </urlParams>
          </HTTPSamplerProxy>
        </hashTree>

        <!-- RAG问答 -->
        <hashTree>
          <HTTPSamplerProxy guiclass="HttpTestSampleGui" testclass="HTTPSamplerProxy" testname="05-RAG问答">
            <stringProp name="HTTPSampler.domain">localhost</stringProp>
            <stringProp name="HTTPSampler.port">8080</stringProp>
            <stringProp name="HTTPSampler.path">/api/rag/question</stringProp>
            <stringProp name="HTTPSampler.method">GET</stringProp>
            <urlParams>
              <Prop name="question">如何保护膝盖？</Prop>
            </urlParams>
          </HTTPSamplerProxy>
        </hashTree>

        <!-- 监听器 -->
        <hashTree>
          <ResultCollector guiclass="ViewResultsFullVisualizer" testclass="ResultCollector" testname="View Results Tree">
            <boolProp name="ResultCollector.error_logging">false</boolProp>
            <objProp>
              <name>saveConfig</name>
              <value class="SampleSaveConfiguration">
                <time>true</time>
                <latency>true</latency>
                <timestampFormat>ms</timestampFormat>
                <response>true</response>
                <message>true</message>
                <encoding>true</encoding>
                <queryString>true</queryString>
                <responseHeaders>false</responseHeaders>
                <samplerData>false</samplerData>
                <xml>false</xml>
                <fieldNames>true</fieldNames>
                <responseData>false</responseData>
                <samplerJson>false</samplerJson>
                < assertions>true</ assertions>
                <subresults>true</subresults>
                <saveResponseData>false</saveResponseData>
              </value>
            </objProp>
            <stringProp name="TestPlan.comments"></stringProp>
          </ResultCollector>
        </hashTree>

        <hashTree>
          <SummaryReport guiclass="SummaryReport" testclass="SummaryReport" testname="Summary Report">
          </SummaryReport>
        </hashTree>

      </hashTree>
    </hashTree>
  </hashTree>
</jmeterTestPlan>
```

---

## 4. 自动化测试

### 4.1 为什么需要自动化测试？

| 对比项 | 手工测试 | 自动化测试 |
|--------|---------|-----------|
| 重复执行 | 每次手动点 | 一键运行 |
| 回归测试 | 耗时耗力 | 几分钟完成 |
| 测试覆盖率 | 低 | 高 |
| 发现问题 | 后期发现 | 每次提交发现 |
| CI/CD集成 | 不支持 | 支持 |

### 4.2 Postman 自动化: Newman

#### 安装Newman

```bash
# 安装Node.js后执行
npm install -g newman

# 验证安装
newman --version
```

#### 运行Collection

```bash
# 运行Collection
newman run /path/to/collection.json

# 指定环境
newman run /path/to/collection.json -e /path/to/environment.json

# 生成HTML报告
newman run /path/to/collection.json -e /path/to/environment.json -r html --reporter-html-export report.html

# 生成JSON报告
newman run /path/to/collection.json -r json --reporter-json-export report.json
```

#### 完整示例

```bash
# 1. 导出Collection和环境
# 在Postman中: Collection → Export, Environment → Export

# 2. 运行测试
newman run rideSketch_API_Test.postman_collection.json \
    -e rideSketch-Dev.postman_environment.json \
    -r html,cli \
    --reporter-html-export test-report.html \
    --reporter-cli-summary \
    --iteration-count 3
```

### 4.3 Java单元测试: JUnit + MockMvc

在项目中创建单元测试:

```java
// src/test/java/org/example/ridesketch/controller/UserControllerTest.java

package org.example.ridesketch.controller;

import org.example.ridesketch.entity.User;
import org.example.ridesketch.mapper.UserMapper;
import org.example.ridesketch.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 单元测试
 *
 * 使用@WebMvcTest进行控制器层测试
 * 使用MockMvc模拟HTTP请求
 * 使用@MockBean模拟依赖
 */
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserMapper userMapper;

    /**
     * 测试用户注册接口
     *
     * 测试场景: 正常注册
     * 预期结果: 返回成功
     */
    @Test
    void testRegister_Success() throws Exception {
        // 准备测试数据
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("testuser");

        // Mock服务层行为
        when(userService.register(any())).thenReturn(mockUser);

        // 发送POST请求
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"email\":\"test@test.com\",\"password\":\"123456\"}"))
                // 验证响应
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("testuser"));

        System.out.println("✅ 用户注册测试通过");
    }

    /**
     * 测试用户登录接口
     *
     * 测试场景: 正常登录
     * 预期结果: 返回token
     */
    @Test
    void testLogin_Success() throws Exception {
        // Mock登录成功返回token
        when(userService.login(any())).thenReturn("mock-jwt-token");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"usernameOrEmail\":\"test\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        System.out.println("✅ 用户登录测试通过");
    }
}
```

### 4.4 集成测试

```java
// src/test/java/org/example/ridesketch/controller/RideControllerIntegrationTest.java

package org.example.ridesketch.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 集成测试
 *
 * 启动完整Spring上下文
 * 测试真实的HTTP请求
 */
@SpringBootTest
@AutoConfigureMockMvc
class RideControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 测试骑行路线规划接口
     *
     * 测试场景: 正常调用骑行路线规划
     * 预期结果: 返回路线数据
     */
    @Test
    void testRidingRoute_Success() throws Exception {
        mockMvc.perform(get("/api/route/riding")
                .param("origin", "116.397,39.909")
                .param("destination", "116.418,39.928"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.route").exists());

        System.out.println("✅ 骑行路线规划集成测试通过");
    }

    /**
     * 测试路线列表接口
     *
     * 测试场景: 分页查询
     * 预期结果: 返回分页数据
     */
    @Test
    void testRouteList_Pagination() throws Exception {
        mockMvc.perform(get("/api/community/routes")
                .param("page", "1")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        System.out.println("✅ 路线列表集成测试通过");
    }
}
```

### 4.5 测试覆盖率

使用JaCoCo生成测试覆盖率报告:

```xml
<!-- pom.xml 添加依赖 -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.11</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

```bash
# 运行测试并生成覆盖率报告
mvn clean test

# 查看报告 (target/site/index.html)
```

---

## 5. 测试用例设计

### 5.1 测试用例模板

| 字段 | 说明 | 示例 |
|------|------|------|
| 用例ID | 唯一标识 | TC_AUTH_001 |
| 用例名称 | 描述性名称 | 用户登录成功 |
| 模块 | 所属模块 | 认证模块 |
| 前置条件 | 测试前需满足的条件 | 用户已注册 |
| 测试步骤 | 执行步骤 | 1.打开登录页 2.输入用户名密码 3.点击登录 |
| 预期结果 | 期望的输出 | 登录成功，跳转首页 |
| 实际结果 | 实际输出 | (测试后填写) |
| 测试结果 | 通过/失败 | 通过 |
| 备注 | 补充说明 | - |

### 5.2 核心测试用例

#### 认证模块

| 用例ID | 用例名称 | 测试步骤 | 预期结果 |
|--------|---------|---------|----------|
| TC_AUTH_001 | 用户名密码正确，登录成功 | 输入正确用户名密码，点击登录 | 返回token，跳转首页 |
| TC_AUTH_002 | 密码错误，登录失败 | 输入正确用户名+错误密码 | 返回错误提示 |
| TC_AUTH_003 | 用户名不存在 | 输入不存在的用户名 | 返回错误提示 |
| TC_AUTH_004 | 用户名空 | 用户名为空 | 返回参数校验错误 |
| TC_AUTH_005 | Token有效，获取用户信息 | 带有效Token请求 | 返回用户信息 |
| TC_AUTH_006 | Token过期 | 带过期Token请求 | 返回401未授权 |
| TC_AUTH_007 | 无Token | 不带Token请求 | 返回401未授权 |

#### 路线规划模块

| 用例ID | 用例名称 | 测试步骤 | 预期结果 |
|--------|---------|---------|----------|
| TC_ROUTE_001 | 骑行路线规划 | 输入起终点坐标 | 返回骑行路线 |
| TC_ROUTE_002 | 多途经点规划 | 输入起点+途经点+终点 | 返回完整路线 |
| TC_ROUTE_003 | AI智能规划 | 输入自然语言描述 | 返回AI解析的路线 |
| TC_ROUTE_004 | 图案路书生成 | 输入图案名称和城市 | 返回图案路线 |
| TC_ROUTE_005 | 缺少必填参数 | 不输入起点 | 返回参数错误 |

#### 社区模块

| 用例ID | 用例名称 | 测试步骤 | 预期结果 |
|--------|---------|---------|----------|
| TC_COMM_001 | 发布路线 | 登录后提交路线信息 | 成功发布 |
| TC_COMM_002 | 查看路线列表 | 访问路线列表 | 返回公开路线 |
| TC_COMM_003 | 点赞路线 | 对路线点赞 | 点赞数+1 |
| TC_COMM_004 | 取消点赞 | 再次点击点赞 | 点赞数-1 |
| TC_COMM_005 | 添加评论 | 登录后提交评论 | 成功添加 |
| TC_COMM_006 | 回复评论 | 对评论进行回复 | 添加楼中楼 |
| TC_COMM_007 | 删除自己的评论 | 删除自己发的评论 | 成功删除 |
| TC_COMM_008 | 删除别人的评论 | 删除别人的评论 | 返回无权限 |

#### RAG模块

| 用例ID | 用例名称 | 测试步骤 | 预期结果 |
|--------|---------|---------|----------|
| TC_RAG_001 | 问答测试 | 提问骑行相关问题 | 返回知识库内容 |
| TC_RAG_002 | 语义搜索 | 搜索关键词 | 返回语义相关内容 |
| TC_RAG_003 | 获取分类 | 获取所有知识分类 | 返回分类列表 |

### 5.3 边界测试用例

| 场景 | 测试数据 | 预期结果 |
|------|---------|----------|
| 超长用户名 | 用户名超过50字符 | 返回参数校验错误 |
| 特殊字符 | 用户名包含<>&"' | 正确处理或过滤 |
| SQL注入 | 用户名输入 `' or '1'='1` | 正确处理 |
| XSS攻击 | 输入 `<script>alert(1)</script>` | 正确处理 |
| 空参数 | 所有参数为空 | 返回参数校验错误 |
| 极端分页 | page=999999 | 返回空列表 |
| 超大文件 | 上传超大文件 | 返回文件过大 |

### 5.4 性能测试用例

| 用例ID | 场景 | 指标 | 目标 |
|--------|------|------|------|
| TP_001 | 单用户登录 | 响应时间 | < 500ms |
| TP_002 | 10并发登录 | TPS | > 50/s |
| TP_003 | 50并发登录 | 错误率 | < 1% |
| TP_004 | 100并发路线列表 | 响应时间 | < 2s |
| TP_005 | 持续5分钟压测 | 内存使用 | < 512MB |

---

## 6. 测试报告模板

### 6.1 测试报告示例

```markdown
# rideSketch API 测试报告

## 一、测试概述

| 项目 | 内容 |
|------|------|
| 测试对象 | rideSketch 后端API |
| 测试类型 | 功能测试 + 性能测试 |
| 测试时间 | 2026-03-04 ~ 2026-03-05 |
| 测试人员 | 测试工程师 |
| 测试环境 | localhost:8080 |

## 二、测试执行情况

### 2.1 功能测试

| 模块 | 用例数 | 通过 | 失败 | 通过率 |
|------|--------|------|------|--------|
| 认证模块 | 10 | 10 | 0 | 100% |
| 地图模块 | 8 | 8 | 0 | 100% |
| 路线模块 | 15 | 14 | 1 | 93% |
| 社区模块 | 20 | 19 | 1 | 95% |
| RAG模块 | 5 | 5 | 0 | 100% |
| **总计** | **58** | **56** | **2** | **96.6%** |

### 2.2 性能测试

| 接口 | 10并发 | 50并发 | 100并发 |
|------|--------|--------|---------|
| /api/auth/login | 120ms | 250ms | 480ms |
| /api/route/riding | 350ms | 800ms | 1.5s |
| /api/community/routes | 500ms | 1.2s | 2.5s |
| /api/rag/question | 2s | 3s | 5s |

## 三、缺陷汇总

### 3.1 严重缺陷

| ID | 缺陷描述 | 模块 | 状态 |
|----|---------|------|------|
| BUG-001 | AI路线规划返回空 | 路线模块 | 已修复 |
| BUG-002 | 路线列表N+1查询 | 社区模块 | 待优化 |

### 3.2 建议优化

1. 添加Redis缓存
2. 优化数据库查询
3. 添加限流机制

## 四、测试结论

✅ **测试通过**: 所有P0级用例通过，系统可上线

---

*报告生成时间: 2026-03-05*
```

---

## 7. CI持续集成

### 7.1 GitHub Actions 自动测试

创建 `.github/workflows/test.yml`:

```yaml
name: API Tests

on:
  push:
    branches: [ main, dev ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'

      - name: Build with Maven
        run: mvn clean package -DskipTests

      - name: Run Unit Tests
        run: mvn test

      - name: Upload Test Results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-results
          path: target/surefire-reports/

      - name: Run Postman Tests
        run: |
          npm install -g newman
          newman run tests/rideSketch_API_Test.postman_collection.json \
            -e tests/rideSketch-Dev.postman_environment.json \
            --reporters json \
            --reporter-json-export newman-report.json
```

### 7.2 自动化测试脚本

创建 `run-tests.sh`:

```bash
#!/bin/bash

# rideSketch 自动化测试脚本

set -e  # 遇到错误立即退出

echo "======================================"
echo "rideSketch 自动化测试"
echo "======================================"

# 1. 启动服务 (如果未启动)
echo "[1/5] 检查服务状态..."
if ! curl -s http://localhost:8080/actuator/health > /dev/null; then
    echo "请先启动后端服务: .\mvnw.cmd spring-boot:run"
    exit 1
fi

# 2. 运行单元测试
echo "[2/5] 运行单元测试..."
mvn test

# 3. 运行Postman测试
echo "[3/5] 运行Postman接口测试..."
if command -v newman &> /dev/null; then
    newman run tests/rideSketch_API_Test.postman_collection.json \
        -e tests/rideSketch-Dev.postman_environment.json \
        --reporters html,cli \
        --reporter-html-export test-report.html
else
    echo "跳过Newman测试 (未安装)"
fi

# 4. 生成测试报告
echo "[4/5] 生成测试报告..."
mvn jacoco:report

# 5. 上传测试结果
echo "[5/5] 完成测试"

echo "======================================"
echo "测试完成!"
echo "======================================"
```

---

## 附录

### A. 快速命令参考

```bash
# 启动Postman测试
newman run collection.json -e environment.json

# 运行JMeter
jmeter -n -t test.jmx -l result.jtl -e -o report

# 运行Java单元测试
mvn test

# 运行单个测试类
mvn test -Dtest=UserControllerTest

# 生成覆盖率报告
mvn jacoco:report
```

### B. 常用工具下载

| 工具 | 下载地址 |
|------|---------|
| Postman | https://www.postman.com/downloads/ |
| JMeter | https://jmeter.apache.org/download_jmeter.cgi |
| IntelliJ IDEA | https://www.jetbrains.com/idea/download/ |

### C. 项目测试文件位置

```
rideSketch/
├── docs/
│   └── TEST.md                    # 测试文档
├── src/test/
│   └── java/.../controller/       # 单元测试
├── tests/                         # 测试数据
│   ├── rideSketch_API_Test.postman_collection.json
│   └── rideSketch-Dev.postman_environment.json
└── .github/workflows/
    └── test.yml                   # CI测试配置
```

---

*文档版本: 1.0*
*更新日期: 2026-03-04*
*作者: rideSketch 测试团队*
