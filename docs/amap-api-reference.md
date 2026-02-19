# 高德地图 Web JS API 参考文档

> 本文档基于高德地图 JS API v2.0+ 版本整理
> 官网: https://lbs.amap.com/api/javascript-api/summary

## 目录
1. [快速开始](#快速开始)
2. [地图基础](#地图基础)
3. [地图控件](#地图控件)
4. [地图事件](#地图事件)
5. [地理编码服务](#地理编码服务)
6. [路径规划](#路径规划)
7. [坐标转换](#坐标转换)
8. [搜索服务](#搜索服务)
9. [插件](#插件)

---

## 快速开始

### 引入方式

```html
<!-- CDN引入 -->
<script type="text/javascript">
    // 高德地图JS API
    https://webapi.amap.com/maps?v=2.0&key=你的Key
</script>
```

### 初始化地图

```javascript
// 创建地图实例
const map = new AMap.Map('container', {
    zoom: 12,              // 缩放级别 [3, 20]
    center: [116.397428, 39.90923], // 中心点经纬度 [lng, lat]
    viewMode: '2D',        // 2D或'3D'
    pitch: 0,              // 俯仰角度 (3D模式)
    mapStyle: 'amap://styles/normal', // 地图样式
});

// 或者不设置中心点，自动定位
const map = new AMap.Map('container');
```

---

## 地图基础

### 常用配置项

```javascript
const map = new AMap.Map('container', {
    // 视图
    zoom: 12,                  // 缩放级别
    center: [lng, lat],        // 中心点
    viewMode: '2D',            // 视图模式: '2D' | '3D'
    pitch: 0,                  // 俯仰角度 (3D)
    rotation: 0,               // 旋转角度

    // 控件
    showIndoorMap: true,      // 显示室内地图
    showBuildingBlock: true, // 展示楼块

    // 样式
    mapStyle: 'amap://styles/normal',      // 普通
    mapStyle: 'amap://styles/dark',       // 夜间
    mapStyle: 'amap://styles/light',      // 浅色
    mapStyle: 'amap://styles/fresh',      // 青春
    mapStyle: 'amap://styles/grey',       // 灰色
    mapStyle: 'amap://styles/macaron',    // 马卡龙

    // 交互
    dragEnable: true,         // 允许拖拽
    zoomEnable: true,         // 允许缩放
    rotateEnable: false,      // 允许旋转
    pitchEnable: false,       // 允许俯仰
    doubleClickZoom: true,    // 允许双击缩放
    keyboardEnable: true,     // 允许键盘操作
});
```

### 常用方法

```javascript
// 设置中心点和缩放级别
map.setZoomAndCenter(12, [116.397428, 39.90923]);

// 设置缩放级别
map.setZoom(12);

// 设置中心点
map.setCenter([116.397428, 39.90923]);

// 获取当前缩放级别
const zoom = map.getZoom();

// 获取中心点经纬度
const center = map.getCenter();

// 获取视口范围
const bounds = map.getBounds();

// 添加覆盖物
map.add(marker);

// 移除覆盖物
map.remove(marker);

// 清除所有覆盖物
map.clearMap();

// 设置地图样式
map.setMapStyle('amap://styles/dark');

// 调整视野适合所有覆盖物
map.setFitView();

// 销毁地图
map.destroy();
```

---

## 地图控件

### 工具条控件

```javascript
// 工具条
const toolbar = new AMap.ToolBar({
    position: 'RT',  // 位置: LT/RT/LB/RB
    locate: true,    // 定位按钮
    liteStyle: true  // 简洁样式
});
map.addControl(toolbar);

// 定位
const location = new AMap.Geolocation({
    enableHighAccuracy: true,  // 高精度
    timeout: 10000,            // 超时时间
    buttonPosition: 'LT',     // 按钮位置
    buttonOffset: new AMap.Pixel(10, 20),
    showMarker: true,         // 显示定位点
    showCircle: true,         // 显示定位精度圆
});
map.addControl(location);

// 比例尺
const scale = new AMap.Scale({
    position: 'LB'  // 左下角
});
map.addControl(scale);

// 鹰眼
const overview = new AMap.OverView({
    position: 'RB',
    isOpen: true  // 默认展开
});
map.addControl(overview);
```

---

## 地图事件

### 常用事件

```javascript
// 点击事件
map.on('click', function(ev) {
    console.log('经纬度:', ev.lnglat.getLng(), ev.lnglat.getLat());
});

// 移动结束
map.on('moveend', function() {
    console.log('地图移动结束');
});

// 缩放结束
map.on('zoomend', function() {
    console.log('缩放级别:', map.getZoom());
});

// 视野变化
map.on('bounds_changed', function() {
    const bounds = map.getBounds();
    console.log('东北:', bounds.getNorthEast());
    console.log('西南:', bounds.getSouthWest());
});

// 移除事件
map.off('click', clickHandler);

// 点击获取经纬度示例
map.on('click', function(ev) {
    const { lng, lat } = ev.lnglat;
    console.log(`经度: ${lng}, 纬度: ${lat}`);
});
```

---

## 地理编码服务

### 正向地理编码 (地址 → 坐标)

```javascript
// 加载地理编码插件
AMap.plugin('AMap.Geocoder', function() {
    const geocoder = new AMap.Geocoder({
        city: '010',  // 城市编码，默认全国
        radius: 1000  // 搜索半径
    });

    // 地址转坐标
    geocoder.getLocation('北京市朝阳区阜通西大街18号', function(status, result) {
        if (status === 'complete' && result.info === 'OK') {
            const location = result.geocodes[0].location;
            console.log('经度:', location.getLng());
            console.log('纬度:', location.getLat());
        } else {
            console.error('地理编码失败');
        }
    });
});
```

### 逆地理编码 (坐标 → 地址)

```javascript
AMap.plugin('AMap.Geocoder', function() {
    const geocoder = new AMap.Geocoder({
        city: '010'
    });

    // 坐标转地址
    geocoder.getAddress([116.397428, 39.90923], function(status, result) {
        if (status === 'complete' && result.info === 'OK') {
            console.log('地址:', result.regeocode.formatted_address);
            console.log('省:', result.regeocode.addressComponent.province);
            console.log('市:', result.regeocode.addressComponent.city);
            console.log('区:', result.regeocode.addressComponent.district);
        }
    });
});
```

### 常用API

```javascript
const geocoder = new AMap.Geocoder({
    city: '010',
    radius: 1000
});

// 批量地理编码
geocoder.getLocation(['地址1', '地址2'], function(status, result) {
    // 处理结果
});
```

---

## 路径规划

### 驾车路径规划

```javascript
AMap.plugin('AMap.Driving', function() {
    const driving = new AMap.Driving({
        city: 'beijing',
        cityd: 'beijing',
        map: map,              // 绑定地图
        panel: 'panel',         // 结果面板ID
        policy: AMap.DrivingPolicy.LEAST_TIME, // 最短时间
        showTraffic: true,     // 显示实时路况
        isOutline: true,       // 显示路线外轮廓
        autoFitView: true      // 自动适应视野
    });

    // 规划路线
    driving.search([
        { keyword: '出发点', city: '北京' },
        { keyword: '目的点', city: '北京' }
    ], function(status, result) {
        if (status === 'complete') {
            console.log('规划成功');
            // 获取规划线路
            const routes = result.routes;
            console.log('距离:', routes[0].distance, '米');
            console.log('时间:', routes[0].time, '秒');
        }
    });

    // 清除路线
    // driving.clear();
});
```

### 驾车策略类型

```javascript
{
    // policy 可选值:
    AMap.DrivingPolicy.LEAST_TIME,       // 最快捷
    AMap.DrivingPolicy.LEAST_FEE,        // 最经济
    AMap.DrivingPolicy.LEAST_DISTANCE,   // 最短距离
    AMap.DrivingPolicy.REAL_TRAFFIC,    // 考虑实时路况
}
```

### 步行路径规划

```javascript
AMap.plugin('AMap.Walking', function() {
    const walking = new AMap.Walking({
        map: map,
        panel: 'panel',
        isOutline: true,
        autoFitView: true
    });

    walking.search([
        { keyword: '起点', city: '北京' },
        { keyword: '终点', city: '北京' }
    ], function(status, result) {
        // 处理结果
    });
});
```

### 骑行路径规划

```javascript
AMap.plugin('AMap.Riding', function() {
    const riding = new AMap.Riding({
        map: map,
        panel: 'panel',
        isOutline: true,
        autoFitView: true,
        policy: 0  // 0:推荐路线 1:骑行路线
    });

    riding.search([
        { keyword: '起点', city: '北京' },
        { keyword: '终点', city: '北京' }
    ], function(status, result) {
        // 处理结果
    });
});
```

### 公交路径规划

```javascript
AMap.plugin('AMap.Transfer', function() {
    const transfer = new AMap.Transfer({
        map: map,
        panel: 'panel',
        city: 'beijing',
        isOutline: true,
        autoFitView: true,
        policy: AMap.TransferPolicy.LEAST_TIME  // 最早出发
    });

    transfer.search([
        { keyword: '起点', city: '北京' },
        { keyword: '终点', city: '北京' }
    ], function(status, result) {
        // 处理结果
    });
});
```

---

## 坐标转换

### GPS坐标转高德坐标

```javascript
AMap.plugin('AMap.Geolocation', function() {
    // GPS转高德
    AMap.convertFrom([lng, lat], 'gps', function(status, result) {
        if (status === 'complete') {
            console.log('高德坐标:', result.locations[0]);
        }
    });
});
```

### 批量坐标转换

```javascript
AMap.convertFrom([
    [lng1, lat1],
    [lng2, lat2]
], 'gps', function(status, result) {
    if (status === 'complete') {
        console.log('转换结果:', result.locations);
    }
});

// type 可选: 'gps' | 'mapbar' | 'baidu'
```

---

## 搜索服务

### 地点关键字搜索

```javascript
AMap.plugin('AMap.PlaceSearch', function() {
    const placeSearch = new AMap.PlaceSearch({
        city: '010',           // 城市
        citylimit: true,       // 限制城市
        pageSize: 20,          // 每页数量
        pageIndex: 1,          // 页码
        map: map,              // 绑定地图
        panel: 'panel',        // 结果面板
        autoFitView: true,     // 自动适应
        type: '风景名胜|餐饮服务' // 类型
    });

    // 搜索
    placeSearch.search('餐厅', function(status, result) {
        if (status === 'complete') {
            console.log('搜索结果:', result.poiList.pois);
        }
    });

    // 点击结果
    placeSearch.on('markerClick', function(e) {
        console.log('点击:', e.data.name);
    });
});
```

### 周边搜索

```javascript
// 在指定经纬度附近搜索
placeSearch.searchNearBy('餐厅', [116.397428, 39.90923], 1000, function(status, result) {
    // 1000表示半径(米)
});
```

### 矩形区域搜索

```javascript
// 在矩形区域内搜索
placeSearch.searchInBounds('餐厅', new AMap.Bounds(
    [lng1, lat1],  // 西南
    [lng2, lat2]   // 东北
), function(status, result) {
});
```

---

## 插件

### 加载插件

```javascript
// 单个插件
AMap.plugin('AMap.ToolBar', function() {
    // 插件加载完成
});

// 多个插件
AMap.plugin(['AMap.ToolBar', 'AMap.Geolocation', 'AMap.Driving'], function() {
    // 所有插件加载完成
});
```

### 常用插件列表

| 插件名称 | 说明 |
|---------|------|
| AMap.ToolBar | 工具条 |
| AMap.Geolocation | 定位 |
| AMap.Scale | 比例尺 |
| AMap.OverView | 鹰眼 |
| AMap.Geocoder | 地理编码 |
| AMap.Driving | 驾车路线规划 |
| AMap.Walking | 步行路线规划 |
| AMap.Riding | 骑行路线规划 |
| AMap.Transfer | 公交路线规划 |
| AMap.PlaceSearch | 地点搜索 |
| AMap.StationSearch | 公交站点搜索 |
| AMap.LineSearch | 公交线路搜索 |
| AMap.DistrictSearch | 行政区划查询 |
| AMap.Autocomplete | 输入提示 |
| AMap.DistrictLayer | 行政区划图层 |

---

## 覆盖物

### 点标记 (Marker)

```javascript
const marker = new AMap.Marker({
    position: [lng, lat],      // 位置
    title: '标题',             // 标题
    icon: '图标URL',           // 图标
    offset: new AMap.Pixel(-13, -30), // 偏移
    draggable: true,           // 可拖拽
    cursor: 'pointer',        // 鼠标样式
    visible: true,            // 可见
    zIndex: 100,              // 层级
    angle: 0,                 // 旋转角度
});

// 自定义图标
marker.setIcon(new AMap.Icon({
    size: new AMap.Size(40, 50),
    image: '图标URL',
    imageSize: new AMap.Size(40, 50)
}));

map.add(marker);

// 绑定点击事件
marker.on('click', function() {
    console.log('点击标记');
});

// 鼠标移入
marker.on('mouseover', function() {
    // 显示信息窗体
    infoWindow.open(map, marker.getPosition());
});
```

### 信息窗体 (InfoWindow)

```javascript
const infoWindow = new AMap.InfoWindow({
    isCustom: false,           // 是否自定义
    content: '<div>内容</div>', // HTML内容
    offset: new AMap.Pixel(0, -30), // 偏移
    position: [lng, lat],      // 位置
    closeWhenClickMap: true,   // 点击地图关闭
    showShadow: true           // 显示阴影
});

// 打开信息窗体
infoWindow.open(map, [lng, lat]);

// 关闭信息窗体
infoWindow.close();
```

### 折线 (Polyline)

```javascript
const polyline = new AMap.Polyline({
    path: [
        [lng1, lat1],
        [lng2, lat2],
        [lng3, lat3]
    ],
    strokeColor: '#FF0000',    // 颜色
    strokeWeight: 5,          // 宽度
    strokeStyle: 'solid',     // 样式: solid | dashed
    strokeDashArray: [10, 5], // 虚线间隔
    geodesic: true,           // 地球线
    showDir: true,           // 显示方向
    zIndex: 50
});

map.add(polyline);
```

### 多边形 (Polygon)

```javascript
const polygon = new AMap.Polygon({
    path: [
        [lng1, lat1],
        [lng2, lat2],
        [lng3, lat3],
        [lng1, lat1] // 闭合
    ],
    fillColor: '#FF0000',
    fillOpacity: 0.3,
    strokeColor: '#0000FF',
    strokeWeight: 2,
    strokeStyle: 'solid'
});

map.add(polygon);
```

### 圆 (Circle)

```javascript
const circle = new AMap.Circle({
    center: [lng, lat],       // 圆心
    radius: 1000,             // 半径(米)
    fillColor: '#FF0000',
    fillOpacity: 0.3,
    strokeColor: '#00FF00',
    strokeWeight: 2
});

map.add(circle);
```

### 公交/地铁线路

```javascript
AMap.plugin('AMap.LineSearch', function() {
    const lineSearch = new AMap.LineSearch({
        pageIndex: 1,
        city: 'beijing',
        extensions: 'all'
    });

    // 搜索公交线路
    lineSearch.searchLine('1路', function(status, result) {
        if (status === 'complete') {
            const lineInfo = result.lineInfo[0];
            console.log('线路名:', lineInfo.name);

            // 绘制线路
            const path = lineInfo.path;
            const polyline = new AMap.Polyline({
                path: path,
                strokeColor: '#09F',
                strokeWeight: 5
            });
            map.add(polyline);
        }
    });
});
```

---

## 常用工具函数

### 计算两点距离

```javascript
// 需加载 AMap.GeometryUtil 插件
AMap.plugin('AMap.GeometryUtil', function() {
    const distance = AMap.GeometryUtil.distance(
        [lng1, lat1],
        [lng2, lat2]
    );
    console.log('距离:', distance, '米');
});
```

### 判断点是否在多边形内

```javascript
AMap.plugin('AMap.GeometryUtil', function() {
    const polygonPath = [[lng1, lat1], [lng2, lat2], [lng3, lat3]];
    const isInside = AMap.GeometryUtil.isPointInRing([lng, lat], polygonPath);
    console.log('是否在多边形内:', isInside);
});
```

### 遮蔽物

```javascript
// 圆形遮蔽
AMap.plugin('AMap.MouseTool', function() {
    const mouseTool = new AMap.MouseTool(map);

    // 绘制圆
    mouseTool.circle({
        fillColor: '#fff',
        fillOpacity: 0.5,
        strokeColor: '#000',
        strokeWeight: 2
    });

    // 绘制矩形
    mouseTool.rectangle({
        fillColor: '#fff',
        fillOpacity: 0.5,
        strokeColor: '#000'
    });
});
```

---

## 项目集成示例

### Vue 3 集成

```vue
<template>
  <div id="map-container" style="width: 100%; height: 500px;"></div>
</template>

<script setup>
import { onMounted, ref } from 'vue';

const map = ref(null);

onMounted(() => {
  // 加载高德地图
  const AMap = window.AMap;

  map.value = new AMap.Map('map-container', {
    zoom: 12,
    center: [116.397428, 39.90923]
  });

  // 点击获取坐标
  map.value.on('click', (ev) => {
    console.log('经度:', ev.lnglat.getLng());
    console.log('纬度:', ev.lnglat.getLat());
  });
});
</script>
```

### React 集成

```jsx
import { useEffect, useRef } from 'react';

function MapComponent() {
  const mapRef = useRef(null);

  useEffect(() => {
    const AMap = window.AMap;
    const map = new AMap.Map(mapRef.current, {
      zoom: 12,
      center: [116.397428, 39.90923]
    });

    return () => {
      map.destroy();
    };
  }, []);

  return <div ref={mapRef} style={{ width: '100%', height: '500px' }} />;
}
```

---

## 常见问题

### 1. 地图不显示
- 检查容器是否有高度
- 检查API Key是否正确
- 检查是否正确引入JS API

### 2. 定位失败
- 检查是否使用https
- 检查浏览器是否允许定位
- 检查是否正确配置安全密钥

### 3. 路径规划失败
- 检查城市名称是否正确
- 检查关键字是否准确
- 查看控制台错误信息

### 4. 坐标偏移
- 使用`AMap.convertFrom`转换坐标
- 确认数据源的坐标系类型

---

## 相关链接

- [高德开放平台](https://lbs.amap.com/)
- [JS API 控制台](https://console.amap.com/)
- [API Key 申请](https://console.amap.com/dev/key/app)
- [示例中心](https://lbs.amap.com/api/javascript-api/example)
- [错误码参考](https://lbs.amap.com/api/webservice/guide/tools/errorcode)

---

*本文档最后更新于 2026-02-19*
