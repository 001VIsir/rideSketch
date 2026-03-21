# dev_2 分支全项目优化重构报告（2026-03-17）

## 1. 任务背景

本次在 `dev_2` 分支执行全项目优化重构，目标是提升可维护性与性能一致性，同时保证行为兼容、过程可审计、结果可回滚。

## 2. 阶段0基线

### 环境与状态
- 已执行初始化脚本：`bash -lc "./init.sh"`（编译成功）
- 文档基线已读取：`docs/progress.md`、`docs/feature_list.md`、`docs/problem.md`

### 质量基线（改动前）
- 后端测试：`./mvnw.cmd test` ✅
- 前端测试：`frontend npm run test` ❌（无 test 脚本，为历史现状）
- 前端构建：`frontend npm run build` ✅（含 `vue-tsc -b`）

## 3. 阶段1全量分析（P0/P1/P2）

### P0（稳定性/性能高风险）
1. `CommunityServiceImpl#getRouteList` 存在典型 N+1 查询：
   - 每条路线都 `hasLiked` 一次
   - 每条路线都 `selectById` 查询用户一次

### P1（高收益重构）
1. 前端 API 层重复创建 Axios 客户端（`map.ts` / `route.ts` / `community.ts`）
2. `community.ts` 中 `likeRoute` 与 `unlikeRoute` 重复逻辑

### P2（可选优化）
1. 前端缺少统一测试脚本，自动化验证入口不一致（保留为后续项）
2. 前端构建存在体积告警（非阻塞）

## 4. 阶段2实施重构

### 后端（P0）
- 文件：`src/main/java/org/example/ridesketch/service/impl/CommunityServiceImpl.java`
- 改动：
  1. 引入 `getUserMapByRoutes()`：一次性批量查询发布者信息
  2. 引入 `getLikedRouteIds()`：一次性查询当前用户已点赞路线集合
  3. `getRouteList()` / `getMyRoutes()` 统一走批量映射，消除列表 N+1
  4. `toggleLike()` 兜底保护，避免点赞数出现负数

### 前端（P1）
- 新增：`frontend/src/api/http.ts`
  - 统一 token 注入、401 处理、baseURL 与 timeout 基础能力
- 改造：
  - `frontend/src/api/map.ts` 改为复用 `createHttpClient`
  - `frontend/src/api/route.ts` 改为复用 `createHttpClient(180000)`
  - `frontend/src/api/community.ts` 改为复用 `createHttpClient`
  - `community.ts` 抽取 `toggleLike()` 消除 `like/unlike` 重复请求逻辑

## 5. 阶段3测试与质量保障

### 新增测试
- `src/test/java/org/example/ridesketch/service/impl/CommunityServiceImplTest.java`
  1. `getRouteListShouldBatchLoadUserAndLikeData`
  2. `toggleLikeShouldNotReduceLikesBelowZero`

### 验证命令与结果
1. `./mvnw.cmd test -Dtest=CommunityServiceImplTest` ✅
2. `./mvnw.cmd test` ✅（17 tests, 0 fail）
3. `./mvnw.cmd clean package -DskipTests` ✅
4. `frontend npm run build` ✅（`vue-tsc -b && vite build`）
5. `frontend npm run`（确认无 `test` 脚本）

### 说明
- Java LSP（jdtls）在当前环境不可用，已通过 Maven 编译+测试完成等价校验。
- 前端 build 存在环境提示：当前 Node `22.11.0`，Vite 提示建议 `>=22.12`。
- 前端 build 存在 chunk size 警告，当前不阻塞功能与构建产出。

## 6. 风险、回滚与兼容性

### 兼容性
- 接口出参与行为保持兼容；仅优化查询路径与前端请求基础设施复用。

### 风险控制
- 核心改动聚焦在 `CommunityServiceImpl` 列表路径，测试覆盖关键行为。
- 前端 API 改动不改变业务参数，仅替换底层 client 创建方式。

### 回滚方案
1. 回滚后端：还原 `CommunityServiceImpl.java`
2. 回滚前端：还原 `api/{map,route,community}.ts` 并移除 `api/http.ts`
3. 回滚测试：删除 `CommunityServiceImplTest.java`

## 7. 结论

本次重构完成了后端高风险 N+1 优化与前端 API 层去重收敛，质量门禁（后端测试、后端构建、前端构建）全部通过，过程证据可复现。
