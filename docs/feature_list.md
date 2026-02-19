# 功能列表

## 用户模块

### F001 - 用户注册
- **描述**: 用户可以通过用户名和密码注册账号
- **后端**: ✅ 已实现 (API: POST /api/auth/register)
- **前端**:
  - 状态: ✅ 已通过
  - 文件: `views/auth/RegisterPage.vue`
  - 路由: `/register`
- **优先级**: 高

### F002 - 用户登录
- **描述**: 用户可以登录系统，获取JWT Token
- **后端**: ✅ 已实现 (API: POST /api/auth/login)
- **前端**:
  - 状态: ✅ 已通过
  - 文件: `views/auth/LoginPage.vue`
  - 路由: `/login`
- **优先级**: 高

### F003 - 用户信息管理
- **描述**: 用户可以查看和修改个人信息
- **后端**: ✅ 已实现 (API: GET/PUT /api/auth/me)
- **前端**:
  - 状态: ⚠️ 部分实现
  - 已有: `views/profile/ProfilePage.vue` 页面
  - 已有: `api/user.ts` 用户API
  - 需要: 完善头像上传、昵称修改等功能
- **优先级**: 中

---

## 地图模块

### F101 - 地图展示
- **描述**: 在前端页面展示高德地图
- **后端**: ✅ N/A (前端直接使用高德JS API)
- **前端**:
  - 状态: ✅ 已实现
  - 文件: `components/common/Amap.vue`, `views/map/MapPage.vue`
- **优先级**: 高

### F102 - 地图基本操作
- **描述**: 支持缩放、平移、拖动地图
- **后端**: ✅ N/A
- **前端**:
  - 状态: ✅ 已实现 (高德JS API内置支持)
- **优先级**: 高

### F103 - 地址搜索
- **描述**: 用户可以搜索地址/POI
- **后端**: ✅ 已实现 (API: GET /api/map/search)
- **前端**:
  - 状态: ✅ 已实现
  - 文件: `views/map/MapPage.vue` 搜索框
- **优先级**: 高

### F104 - 坐标拾取
- **描述**: 用户可以点击地图获取经纬度
- **后端**: ✅ 已实现 (API: GET /api/map/regeocode)
- **前端**:
  - 状态: ✅ 已实现
  - 文件: `views/map/MapPage.vue` 拾取坐标功能
- **优先级**: 中

---

## 路线规划模块

### F201 - 基础路径规划
- **描述**: 使用高德API进行基础骑行/步行路线规划
- **后端**: ✅ 已实现
  - API: GET /api/route/riding
  - API: GET /api/route/walking
  - API: POST /api/route/plan
- **前端**:
  - 状态: ✅ 已通过
  - 文件: `api/route.ts`, `stores/routeStore.ts`
  - 组件: `components/route/RoutePanel.vue`, `RouteResultPanel.vue`
  - 集成: `views/map/MapPage.vue`
- **优先级**: 高

### F202 - AI智能路线规划
- **描述**: 用户输入自然语言描述，LLM分析并规划路线
- **后端**: ✅ 已实现 (API: POST /api/route/ai-plan)
- **前端**:
  - 状态: ✅ 已通过
  - 文件: `components/route/AIRoutePanel.vue`
  - 集成: `views/map/MapPage.vue`
- **优先级**: 高

### F203 - 多途经点路线
- **描述**: 支持添加多个途经点
- **后端**: ✅ 已实现 (通过 /api/route/plan 的 waypoints 参数)
- **前端**:
  - 状态: ❌ 待开发
  - 需要: `components/route/WaypointList.vue` 途经点管理组件
- **优先级**: 中

---

## 图案路书模块

### F301 - 图案路书生成
- **描述**: AI Agent自动生成图案形状的骑行路线
- **后端**: ✅ 已实现 (API: POST /api/route/pattern)
- **前端**:
  - 状态: ✅ 已通过
  - 文件: `views/route/PatternPage.vue`
  - 路由: `/pattern`
- **优先级**: 高

### F302 - 路线量化数据展示
- **描述**: 显示路线总距离、预计时间、难度等数据
- **后端**: ✅ 已实现 (API返回数据包含distance/duration)
- **前端**:
  - 状态: ✅ 已通过 (集成在RouteResultPanel中)
- **优先级**: 中

---

## 社区论坛模块

### F401 - 发布路线
- **描述**: 用户可以发布自己的骑行路线到社区
- **后端**: ✅ 已实现 (API: POST /api/community/route)
- **前端**:
  - 状态: ❌ 待开发
  - 需要: `api/community.ts` 社区API
  - 需要: 发布表单组件
  - 需要: 路线保存功能入口
- **优先级**: 高

### F402 - 查看路线列表
- **描述**: 社区首页展示所有发布的路线
- **后端**: ✅ 已实现 (API: GET /api/community/routes)
- **前端**:
  - 状态: ❌ 待开发
  - 需要: `views/community/CommunityPage.vue` 社区首页
  - 需要: 路由 `/community`
  - 需要: 路线卡片组件
- **优先级**: 高

### F403 - 查看路线详情
- **描述**: 查看路线的详细信息和地图
- **后端**: ✅ 已实现 (API: GET /api/community/route/{id})
- **前端**:
  - 状态: ❌ 待开发
  - 需要: `views/community/RouteDetailPage.vue` 路线详情页
  - 需要: 路由 `/community/route/:id`
  - 需要: 地图展示路线
- **优先级**: 高

### F404 - 编辑/删除路线
- **描述**: 作者可以编辑或删除自己的路线
- **后端**: ✅ 已实现 (API: PUT/DELETE /api/community/route/{id})
- **前端**:
  - 状态: ❌ 待开发
  - 需要: 在RouteDetailPage添加编辑/删除按钮
- **优先级**: 中

### F405 - 路线点赞
- **描述**: 用户可以为喜欢的路线点赞
- **后端**: ✅ 已实现 (API: POST /api/community/route/{id}/like)
- **前端**:
  - 状态: ❌ 待开发
  - 需要: 点赞按钮组件
- **优先级**: 中

### F406 - 评论/回复
- **描述**: 用户可以对路线进行评论和回复
- **后端**: ✅ 已实现 (API: GET/POST /api/community/route/{id}/comments)
- **前端**:
  - 状态: ❌ 待开发
  - 需要: 评论列表组件
  - 需要: 评论输入组件
- **优先级**: 中

---

## 前端开发文件清单

### 页面 (views/)
```
views/
├── auth/
│   ├── LoginPage.vue      # 登录页面 (F002)
│   └── RegisterPage.vue   # 注册页面 (F001)
├── map/
│   └── MapPage.vue       # 地图页面 (F101-F104, F201-F203)
├── route/
│   └── PatternPage.vue   # 图案路书页面 (F301)
└── community/
    ├── CommunityPage.vue    # 社区首页 (F402)
    └── RouteDetailPage.vue  # 路线详情页 (F403-F406)
```

### 组件 (components/)
```
components/
├── route/
│   ├── RoutePanel.vue          # 路线规划面板 (F201)
│   ├── RouteModeSelector.vue   # 出行方式选择 (F201)
│   ├── RouteResultPanel.vue    # 路线结果展示 (F201-F202)
│   ├── AIRouteInput.vue       # AI规划输入 (F202)
│   └── WaypointList.vue       # 途经点列表 (F203)
└── community/
    ├── RouteCard.vue           # 路线卡片 (F402)
    ├── CommentList.vue         # 评论列表 (F406)
    └── CommentForm.vue         # 评论表单 (F406)
```

### 状态管理 (stores/)
```
stores/
├── routeStore.ts    # 路线状态管理 (F201-F203)
└── communityStore.ts # 社区状态管理 (F402-F406)
```

### API (api/)
```
api/
├── route.ts       # 路线规划API (F201-F203, F301)
└── community.ts   # 社区API (F401-F406) - 需要新建
```

### 需要扩展的文件
- `router/index.ts` - 添加新路由
- `utils/amap.ts` - 添加路线绘制函数
- `types/map.ts` - 添加路线/社区相关类型
- `App.vue` - 添加社区导航入口

---

## 开发优先级排序

### Phase 1: 用户模块 (F001-F003)
1. F002 登录页面 - 最高优先级
2. F001 注册页面
3. F003 用户信息管理（完善）

### Phase 2: 地图+路线规划 (F101-F203)
4. F201 基础路线规划
5. F202 AI智能路线规划
6. F203 多途经点路线

### Phase 3: 社区论坛 (F401-F406)
7. F402 查看路线列表
8. F403 查看路线详情
9. F401 发布路线
10. F404-F406 互动功能

### Phase 4: 图案路书 (F301-F302)
11. F301 图案路书生成

---

## 技术升级任务

### T001 - Spring AI 升级
- **描述**: 使用Spring AI框架重构AI服务，引入Function Calling和RAG能力
- **状态**: 规划中
- **优先级**: 中
