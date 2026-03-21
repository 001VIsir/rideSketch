# rideSketch（骑迹）

> 智能骑行路线规划与社区分享平台（Spring Boot + Vue 3）

[![Java](https://img.shields.io/badge/Java-17-007396?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.x-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Vue](https://img.shields.io/badge/Vue-3-4FC08D?logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![MySQL](https://img.shields.io/badge/MySQL-8.x-4479A1?logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Redis](https://img.shields.io/badge/Redis-7.x-DC382D?logo=redis&logoColor=white)](https://redis.io/)

rideSketch（骑迹）是一个面向骑行场景的全栈应用，提供**地图搜索、智能路线规划、图案路书生成、社区发布互动**等能力。项目采用前后端分离架构，后端提供统一 API，前端负责地图交互与业务体验。

---

## ✨ 核心特性

- **智能路线规划**：支持骑行/步行、多途经点规划
- **AI 路线助手**：自然语言输入（如“给我一条 20km 河边骑行路线”）
- **图案路书**：自动生成特定图形轨迹骑行路线
- **社区分享**：发布路线、查看详情、点赞、评论
- **RAG 知识检索**：面向骑行场景的语义搜索能力

---

## 🧱 技术栈

### Backend
- Java 17
- Spring Boot 3.5.x
- Spring Security + JWT
- MyBatis-Plus
- MySQL 8.x
- Redis
- Spring AI + Ollama

### Frontend
- Vue 3 + TypeScript
- Vite
- Pinia
- Vue Router
- Element Plus
- Amap JS API 2.0

---

## 🗂️ 项目结构

```text
rideSketch/
├─ src/main/java/org/example/ridesketch/
│  ├─ controller/     # API 控制器
│  ├─ service/        # 业务接口与实现
│  ├─ mapper/         # MyBatis-Plus 持久层
│  ├─ entity/         # 实体对象
│  ├─ dto/            # 数据传输对象
│  ├─ security/       # JWT 与安全过滤器
│  └─ config/         # 系统配置
├─ src/main/resources/
│  └─ application.properties
├─ frontend/
│  ├─ src/views/      # 页面
│  ├─ src/components/ # 组件
│  ├─ src/api/        # 前端 API 封装
│  ├─ src/stores/     # Pinia 状态管理
│  └─ src/router/
└─ docs/              # 项目文档
```

---

## 🚀 快速开始

### 1) 环境准备

- JDK 17+
- Node.js 20+
- MySQL 8.x
- Redis 6+
- Ollama（本地）

建议先拉起本地依赖服务：MySQL / Redis / Ollama。

### 2) 克隆与初始化

```powershell
git clone <your-repo-url>
cd rideSketch
```

如仓库存在初始化脚本，可执行：

```powershell
./init.sh
```

### 3) 启动后端

```powershell
.\mvnw.cmd spring-boot:run
```

后端默认地址：`http://localhost:8080`

### 4) 启动前端

```powershell
cd frontend
npm install
npm run dev
```

前端默认地址：`http://localhost:5173`

---

## ⚙️ 配置说明

后端主要配置位于：`src/main/resources/application.properties`

需按本地环境设置：
- MySQL 连接信息
- Redis 地址
- JWT 密钥与过期时间
- 高德地图服务端 Key
- Ollama 服务地址与模型名

前端高德 Key 配置：`frontend/src/utils/amap.ts`

> 建议使用环境变量或本地私有配置覆盖，避免将敏感信息提交到仓库。

---

## 🧪 测试与构建

### 后端

```powershell
.\mvnw.cmd test
.\mvnw.cmd clean package -DskipTests
```

### 前端

```powershell
cd frontend
npm run build
```

---

## 📡 API 概览

### 认证与用户
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `PUT /api/auth/me`

### 地图
- `GET /api/map/geocode`
- `GET /api/map/regeo`
- `GET /api/map/search`

### 路线规划
- `GET /api/route/riding`
- `GET /api/route/walking`
- `POST /api/route/plan`
- `POST /api/route/ai-plan`
- `POST /api/route/pattern`

### 社区
- `GET /api/community/routes`
- `POST /api/community/route`
- `GET /api/community/route/{id}`
- `PUT /api/community/route/{id}`
- `DELETE /api/community/route/{id}`
- `POST /api/community/route/{id}/like`
- `GET /api/community/route/{id}/comment`
- `POST /api/community/route/{id}/comment`

---

## 🧭 Roadmap

- [ ] 完善用户资料管理（头像上传、昵称编辑等）
- [ ] 社区检索与筛选能力增强
- [ ] 前端自动化测试体系补齐
- [ ] 生产环境部署模板（Docker / CI）

---

## 🤝 贡献指南

欢迎提交 Issue 与 PR。

1. Fork 仓库并新建分支
2. 提交修改并确保本地构建/测试通过
3. 发起 Pull Request，说明变更内容与动机

---

## 📄 License

如无特殊声明，默认采用 MIT License。你可以根据仓库实际需要补充 `LICENSE` 文件。

---

## 🙏 致谢

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Vue 3](https://vuejs.org/)
- [Amap](https://lbs.amap.com/)
- [Ollama](https://ollama.com/)
