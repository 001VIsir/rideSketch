# rideSketch 项目需求规格说明书

## 1. 项目概述

- **项目名称**：rideSketch（骑迹）
- **项目目标**：打造一款智能骑行路线规划与社区分享平台，用户可通过自然语言描述需求获取个性化骑行路线，还能生成图案路书，并将路线分享到社区。
- **目标用户**：骑行爱好者、户外运动者、旅游规划者

## 2. 技术栈

### 2.1 后端技术
- **框架**：Spring Boot 3.5.x
- **数据库**：MySQL 8.x（用户：root，密码：Cqian1231）
- **缓存**：Redis（无密码）
- **ORM**：MyBatis-Plus
- **安全**：Spring Security + JWT

### 2.2 前端技术
- **框架**：Vue 3 + TypeScript
- **构建工具**：Vite
- **状态管理**：Pinia
- **UI组件**：Element Plus
- **地图SDK**：高德地图 JS API 2.0

### 2.3 AI/大模型集成
- **本地LLM**：支持接入本地部署的大语言模型（如Ollama、LM Studio等）
- **AI Agent**：用于图案路书生成的智能体

### 2.4 高德地图API
- **JS API 2.0**：前端地图展示、交互
- **Web服务API**：POI搜索、地理编码、路径规划

### 2.5 解决方案
- **高并发处理**：
  - Redis缓存热点数据
  - Redis Session存储
  - 异步任务处理（骑行路线计算）
- **分布式部署支持**

## 3. 功能模块

### 3.1 用户模块
- 用户注册（邮箱/用户名）
- 用户登录（JWT Token）
- 用户信息管理
- 头像上传

### 3.2 地图模块
- 地图展示（高德地图JS API 2.0）
- 地图基本操作（缩放、平移、拖动）
- 地址搜索（POI搜索）
- 地理编码/逆地理编码
- 坐标拾取

### 3.3 路线规划模块
- 基础路径规划（步行/骑行）
- AI智能路线规划（自然语言描述）
- 多途经点路线
- 路线偏好设置（距离、景点、难度）

### 3.4 图案路书模块
- 图案路书生成（AI Agent）
- 支持数字/字母/简单图形
- 路线量化数据展示
- 路线可视化

### 3.5 社区论坛模块
- 发布骑行路线
- 编辑/删除自己的路线
- 路线点赞
- 评论/回复
- 用户主页

## 4. 非功能性需求

### 4.1 性能
- 页面加载时间 < 3秒
- API响应时间 < 500ms（常规操作）
- 支持1000+并发用户

### 4.2 安全
- 密码加密存储
- JWT Token认证
- API接口防刷
- XSS/CSRF防护

### 4.3 可用性
- 响应式设计
- 友好的错误提示
- loading状态展示

## 5. 项目结构

```
rideSketch/
├── backend/                 # Spring Boot后端
│   ├── src/main/java/
│   │   └── org/example/ridesketch/
│   │       ├── controller/  # 控制器
│   │       ├── service/    # 业务逻辑
│   │       ├── mapper/     # 数据访问
│   │       ├── entity/    # 实体类
│   │       ├── dto/       # 数据传输对象
│   │       ├── config/    # 配置类
│   │       └── security/  # 安全相关
│   └── resources/
│       ├── mapper/         # MyBatis映射
│       └── application.yml
├── frontend/                # Vue前端
│   ├── src/
│   │   ├── components/     # 组件
│   │   ├── views/         # 页面
│   │   ├── router/        # 路由
│   │   ├── stores/        # 状态管理
│   │   ├── api/           # API调用
│   │   └── utils/         # 工具函数
│   └── index.html
└── docs/                   # 文档
```

## 6. 开发规范

### 6.1 代码规范
- 遵循Google Java Style Guide
- 前端遵循Vue 3 Style Guide
- 变量命名语义化

### 6.2 Git规范
- 提交信息格式：`<类型>: <描述>`
- 分支策略：main + 功能分支

### 6.3 文档要求
- 所有文档使用中文
- 遇到问题记录到 docs/problem.md
