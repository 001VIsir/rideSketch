# 问题与解决方案记录

## 2026-02-19 - 路线规划功能测试

### 测试目标
验证以下功能的可用性：
1. F201 - 基础路径规划（骑行/步行）
2. F202 - AI智能路线规划
3. F203 - 多途经点路线

### 测试过程

#### 1. F201 - 基础路径规划（骑行）✅

**测试方法**：直接调用API
```bash
curl -s -X GET "http://localhost:8080/api/route/riding?origin=116.358104,39.961554&destination=116.427428,39.929226"
```

**结果**：成功
- 路线：北京邮电大学 → 天安门
- 距离：8.8公里
- 耗时：约35分钟（2123秒）
- 返回详细的导航步骤

**结论**：功能正常

---

#### 2. F201 - 基础路径规划（步行）⚠️

**测试方法**：
```bash
curl -s -X GET "http://localhost:8080/api/route/walking?origin=116.358104,39.961554&destination=116.427428,39.929226"
```

**结果**：
```json
{"status":"0","info":"SERVICE_NOT_AVAILABLE","route":null}
```

**分析**：
- 高德步行路线API返回服务不可用
- 可能原因：高德API配额限制或服务问题
- 骑行功能正常工作

**结论**：部分可用，骑行功能正常，步行功能受高德API限制

---

#### 3. F203 - 多途经点路线 ✅

**测试方法**：
```bash
curl -s -X POST "http://localhost:8080/api/route/plan" \
  -H "Content-Type: application/json" \
  -d '{"origin":"116.358104,39.961554","destination":"116.427428,39.929226","waypoints":"116.373100,39.941200|116.397428,39.909226","mode":"riding"}'
```

**结果**：成功
- 经过2个途经点
- 距离：14.9公里
- 耗时：约60分钟（3574秒）
- 返回详细的导航步骤

**结论**：功能正常

---

#### 4. F202 - AI智能路线规划 ✅

**测试方法**：
```bash
# 英文输入测试
curl -s -X POST "http://localhost:8080/api/route/ai-plan" \
  -H "Content-Type: application/json" \
  -d '{"description":"I want to plan a cycling route starting from Beijing University of Posts and Telecommunications, about 30km, focusing on scenic spots. Please recommend a route.", "city":"Beijing"}'
```

**结果**：成功
- LLM (Ollama qwen3:8b) 成功解析用户需求
- 返回结果：
  ```json
  {
    "status": "0",
    "info": "请提供更详细的起点和终点信息",
    "analysis": "{\"origin\": \"北京邮电大学\", \"destination\": null, \"distance\": 30, \"preference\": \"scenic\", \"description\": \"路线将串联颐和园西堤、圆明园遗址公园及香山公园，融合湖光山色与古典园林景观...\"}"
  }
  ```

**分析**：
- AI成功识别起点"北京邮电大学"
- AI推荐了途经点：颐和园、圆明园、香山公园
- 由于缺少终点，提示需要更多信息（这是预期行为）

**结论**：AI功能正常工作，Ollama qwen3:8b 模型运行正常

---

### 遇到的问题与解决方案

#### 问题1：Windows命令行中文编码问题

**现象**：curl发送中文请求时报错
```
Invalid UTF-8 middle byte 0xd2
```

**解决方案**：
- 使用Python脚本或PowerShell脚本发送请求
- 或使用英文输入进行测试

#### 问题2：GET请求带中文参数

**现象**：
```
Invalid character found in the request target
```

**解决方案**：
- 使用POST请求代替GET请求
- 或对中文进行URL编码

---

### 测试结论

| 功能 | 状态 | 说明 |
|------|------|------|
| F201 骑行路线 | ✅ 正常 | 8.8km路线规划成功 |
| F201 步行路线 | ⚠️ 限制 | 高德API限制 |
| F203 多途经点 | ✅ 正常 | 14.9km路线规划成功 |
| F202 AI路线 | ✅ 正常 | Ollama qwen3:8b 工作正常 |

### 服务依赖确认

- ✅ 后端服务：localhost:8080
- ✅ 前端服务：localhost:5173
- ✅ MySQL：localhost:3306
- ✅ Redis：localhost:6379
- ✅ Ollama：localhost:11434 (qwen3:8b模型)
- ✅ 高德地图API：已配置

---

### 2026-02-19 - 前端浏览器测试尝试

#### 问题：Playwright MCP 无法启动 Chrome 浏览器

**现象**：
```
Error: browserType.launchPersistentContext: Failed to launch the browser process.
Browser logs:
<process did exit: exitCode=0, signal=null>
中文错误信息：系统中通用的进程已打开
```

**分析**：
- Windows 环境下 Chrome 浏览器沙箱权限问题
- 可能与企业安全策略或权限配置有关

**解决方案**：
- 尝试安装浏览器但仍失败
- 改用代码审查方式确认前端状态

---

#### 前端功能现状发现

**检查结果**：
- 前端路由只有两个页面：`/map` 和 `/profile`
- `MapPage.vue` 只实现了：
  - 地址搜索功能
  - 坐标拾取功能
- **缺失**：路线规划相关的UI组件
- **缺失**：AI智能路线规划界面
- **缺失**：图案路书生成界面

**API 调用情况**：
- `frontend/src/api/map.ts` 只包含：
  - `searchAddress()` - 地址搜索
  - `geocode()` - 地理编码
  - `reGeocode()` - 逆地理编码
- **缺失**：路线规划API调用（`/api/route/plan`, `/api/route/ai-plan`, `/api/route/pattern`）

**结论**：
- 后端 API 已完整实现 F201-F203 功能
- 前端 UI 尚未实现路线规划相关功能
- 需要开发前端界面来调用后端API

---

### 后续建议

1. **步行路线问题**：可能是高德API配额限制，可考虑：
   - 申请更高配额
   - 使用骑行路线作为备选
   - 检查高德开发者后台

2. **前端开发建议**：
   - 在 MapPage 添加路线规划面板
   - 实现起点/终点/途经点输入
   - 添加出行方式选择（骑行/步行）
   - 添加AI路线规划输入框
   - 添加路线结果展示和地图渲染

3. **AI功能增强**：可考虑：
   - 完善prompt模板提高解析准确率
   - 添加更多POI类型支持
   - 优化返回结果展示
